package com.padkeys.app

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Process
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.tanh

/** Ang 6 na pad presets. */
enum class PadPreset(val label: String, val blurb: String) {
    WarmWorship("Warm Worship", "Mainit, malambot na pundasyon"),
    SoftShimmer("Soft Shimmer", "Banayad na kinang sa itaas"),
    DeepStillness("Deep Stillness", "Malalim, tahimik, madilim"),
    CathedralGlow("Cathedral Glow", "Malawak na simbahan, mahabang reverb"),
    GentleChoir("Gentle Choir", "Parang koro na 'ah'"),
    AiryPraise("Airy Praise", "Maaliwalas, may hangin"),
}

private const val SR = 44100
private const val TABLE_SIZE = 2048
private const val BLOCK = 64
private val TWO_PI = (2.0 * PI).toFloat()
private val HALF_PI = (PI / 2.0).toFloat()

// Pangkalahatang lakas ng isang voice at makeup gain sa dulo.
private const val VOICE_GAIN = 0.22f
private const val MAKEUP = 2.0f

private class Spec(
    harmonics: Int,
    rolloff: Float,
    val gSub: Float,
    val gRoot: Float,
    val gFifth: Float,
    val gOct: Float,
    val gShimmer: Float,
    val detune: Float,      // cents
    val cutoff: Float,      // Hz (bago ang Tone)
    val lfoRate: Float,     // Hz
    val lfoDepth: Float,    // 0..1 na galaw ng cutoff
    val wet: Float,
    val room: Float,        // reverb feedback
    val damp: Float,
    val formant: Float = 0f,
    val air: Float = 0f,
    val vibrato: Float = 0f,
) {
    val table: FloatArray = buildTable(harmonics, rolloff)
}

private fun buildTable(harmonics: Int, rolloff: Float): FloatArray {
    val t = FloatArray(TABLE_SIZE + 2)
    var peak = 0f
    for (i in 0 until TABLE_SIZE) {
        var s = 0f
        for (n in 1..harmonics) {
            s += sin(TWO_PI * n * i / TABLE_SIZE) / n.toFloat().pow(rolloff)
        }
        t[i] = s
        if (abs(s) > peak) peak = abs(s)
    }
    if (peak > 0f) for (i in 0 until TABLE_SIZE) t[i] = t[i] / peak
    t[TABLE_SIZE] = t[0]
    t[TABLE_SIZE + 1] = t[1]
    return t
}

private val SPECS: List<Spec> = listOf(
    // Warm Worship
    Spec(harmonics = 5, rolloff = 1.7f, gSub = 0.25f, gRoot = 1.0f, gFifth = 0.55f, gOct = 0.45f, gShimmer = 0.10f,
        detune = 5f, cutoff = 1300f, lfoRate = 0.11f, lfoDepth = 0.18f, wet = 0.35f, room = 0.84f, damp = 0.35f),
    // Soft Shimmer
    Spec(harmonics = 6, rolloff = 1.3f, gSub = 0.10f, gRoot = 0.9f, gFifth = 0.5f, gOct = 0.5f, gShimmer = 0.40f,
        detune = 6f, cutoff = 2400f, lfoRate = 0.16f, lfoDepth = 0.22f, wet = 0.42f, room = 0.87f, damp = 0.25f),
    // Deep Stillness
    Spec(harmonics = 3, rolloff = 2.0f, gSub = 0.80f, gRoot = 1.0f, gFifth = 0.45f, gOct = 0.20f, gShimmer = 0.03f,
        detune = 4f, cutoff = 650f, lfoRate = 0.07f, lfoDepth = 0.15f, wet = 0.30f, room = 0.82f, damp = 0.50f),
    // Cathedral Glow
    Spec(harmonics = 6, rolloff = 1.5f, gSub = 0.20f, gRoot = 1.0f, gFifth = 0.6f, gOct = 0.45f, gShimmer = 0.22f,
        detune = 6f, cutoff = 1800f, lfoRate = 0.09f, lfoDepth = 0.20f, wet = 0.58f, room = 0.93f, damp = 0.22f),
    // Gentle Choir
    Spec(harmonics = 10, rolloff = 1.0f, gSub = 0.10f, gRoot = 1.0f, gFifth = 0.50f, gOct = 0.40f, gShimmer = 0.06f,
        detune = 7f, cutoff = 2300f, lfoRate = 0.13f, lfoDepth = 0.12f, wet = 0.40f, room = 0.86f, damp = 0.30f,
        formant = 0.55f, vibrato = 0.0009f),
    // Airy Praise
    Spec(harmonics = 5, rolloff = 1.4f, gSub = 0.05f, gRoot = 0.8f, gFifth = 0.5f, gOct = 0.65f, gShimmer = 0.45f,
        detune = 5f, cutoff = 3200f, lfoRate = 0.19f, lfoDepth = 0.25f, wet = 0.45f, room = 0.88f, damp = 0.20f,
        air = 0.012f),
)

// ------------------------------------------------------------------ Voice

private class Voice(private val spec: Spec, rootHz: Float, private val seed: Int) {
    // p: 0..1 na "progress" ng fade. dir=+1 fade in, -1 fade out. Iisang p lang kaya
    // tuloy-tuloy kahit mag-iba ng direksyon sa gitna (walang pop).
    var p = 0f
    var dir = 1
    var rate = 0f
    var dead = false

    private val ratios = floatArrayOf(0.5f, 1f, 1.5f, 2f, 4f, 6f)
    private val gains = floatArrayOf(spec.gSub, spec.gRoot, spec.gFifth, spec.gOct, spec.gShimmer, spec.gShimmer * 0.6f)
    private val active = gains.indices.filter { gains[it] > 0f }
    private val n = active.size * 2

    private val ph = FloatArray(n)
    private val inc = FloatArray(n)
    private val amp = FloatArray(n)
    private val gl = FloatArray(n)
    private val gr = FloatArray(n)
    private val shim = BooleanArray(n)

    private var lfoPh = ((seed * 37) % 100) / 100f
    private val lfoInc = spec.lfoRate * (0.9f + 0.05f * (seed % 5)) / SR
    private var lfo2Ph = ((seed * 61) % 100) / 100f
    private val lfo2Inc = 0.23f / SR
    private var vibPh = 0f
    private val vibInc = 5.2f / SR

    private var lp1L = 0f
    private var lp1R = 0f
    private var lp2L = 0f
    private var lp2R = 0f
    private var noise = seed * 7919 + 12345

    private var f1Lo = 0f
    private var f1Bd = 0f
    private var f2Lo = 0f
    private var f2Bd = 0f

    init {
        var k = 0
        for (i in active) {
            val f = rootHz * ratios[i]
            for (s in 0..1) {
                val spread = 0.7f + 0.3f * ((i * 7 + seed) % 3)
                val cents = (if (s == 0) -1f else 1f) * spec.detune * spread
                inc[k] = f * 2f.pow(cents / 1200f) / SR
                ph[k] = ((seed * 0.618f * (k + 1)) % 1f + 1f) % 1f
                amp[k] = gains[i]
                val leftBias = if ((s + i) % 2 == 0) 0.8f else 0.2f
                gl[k] = leftBias
                gr[k] = 1f - leftBias
                shim[k] = i >= 4
                k++
            }
        }
    }

    fun render(outL: FloatArray, outR: FloatArray, count: Int, cutScale: Float, shimScale: Float) {
        val lfoNow = sin(lfoPh * TWO_PI)
        val fc = (spec.cutoff * cutScale * (1f + spec.lfoDepth * lfoNow)).coerceIn(120f, 9000f)
        val a = 1f - exp(-TWO_PI * fc / SR)

        for (j in 0 until count) {
            p += dir * rate
            if (p >= 1f) {
                p = 1f
            } else if (p <= 0f) {
                p = 0f
                if (dir < 0) {
                    dead = true
                    return
                }
            }
            val s = p * p * (3f - 2f * p)
            val g = sin(s * HALF_PI) // equal-power

            lfoPh += lfoInc
            if (lfoPh >= 1f) lfoPh -= 1f
            lfo2Ph += lfo2Inc
            if (lfo2Ph >= 1f) lfo2Ph -= 1f
            val l1 = sin(lfoPh * TWO_PI)
            val l2 = sin(lfo2Ph * TWO_PI)

            var mult = 1f + l1 * 0.0005f
            if (spec.vibrato > 0f) {
                vibPh += vibInc
                if (vibPh >= 1f) vibPh -= 1f
                mult += sin(vibPh * TWO_PI) * spec.vibrato
            }

            var l = 0f
            var r = 0f
            for (k in 0 until n) {
                var x = ph[k] + inc[k] * mult
                if (x >= 1f) x -= 1f
                ph[k] = x
                val t = x * TABLE_SIZE
                val i0 = t.toInt()
                val fr = t - i0
                val tbl = spec.table
                var v = (tbl[i0] + (tbl[i0 + 1] - tbl[i0]) * fr) * amp[k]
                if (shim[k]) v *= shimScale * (0.65f + 0.35f * l2)
                l += v * gl[k]
                r += v * gr[k]
            }

            if (spec.air > 0f) {
                noise = noise * 1664525 + 1013904223
                l += (noise shr 8) / 8388608f * spec.air
                noise = noise * 1664525 + 1013904223
                r += (noise shr 8) / 8388608f * spec.air
            }

            // Warm low-pass (2 poles)
            lp1L += a * (l - lp1L)
            lp2L += a * (lp1L - lp2L)
            lp1R += a * (r - lp1R)
            lp2R += a * (lp1R - lp2R)
            l = lp2L
            r = lp2R

            if (spec.formant > 0f) {
                val m = (l + r) * 0.5f
                f1Lo += F1 * f1Bd
                val h1 = m - f1Lo - FQ * f1Bd
                f1Bd += F1 * h1
                f2Lo += F2 * f2Bd
                val h2 = m - f2Lo - FQ * f2Bd
                f2Bd += F2 * h2
                val fo = (f1Bd + 0.7f * f2Bd) * spec.formant * 2f
                val keep = 1f - 0.5f * spec.formant
                l = l * keep + fo
                r = r * keep + fo
            }

            val o = g * (1f + 0.08f * l1) * VOICE_GAIN
            outL[j] += l * o
            outR[j] += r * o
        }
    }

    private companion object {
        // "Ah" na vowel: ~730 Hz at ~1090 Hz (Chamberlin SVF)
        val F1 = 2f * sin((PI * 730.0 / SR).toFloat())
        val F2 = 2f * sin((PI * 1090.0 / SR).toFloat())
        const val FQ = 0.25f
    }
}

// ------------------------------------------------------------------ Reverb (Freeverb-style, stereo)

private class Reverb {
    private val combTun = intArrayOf(1116, 1188, 1277, 1356, 1422, 1491, 1557, 1617)
    private val apTun = intArrayOf(556, 441, 341, 225)
    private val spread = 23

    private val combL = Array(8) { FloatArray(combTun[it]) }
    private val combR = Array(8) { FloatArray(combTun[it] + spread) }
    private val ciL = IntArray(8)
    private val ciR = IntArray(8)
    private val fL = FloatArray(8)
    private val fR = FloatArray(8)

    private val apL = Array(4) { FloatArray(apTun[it]) }
    private val apR = Array(4) { FloatArray(apTun[it] + spread) }
    private val aiL = IntArray(4)
    private val aiR = IntArray(4)

    var feedback = 0.84f
    var damp = 0.3f

    fun process(input: Float, out: FloatArray) {
        val x = input * 0.03f
        var l = 0f
        var r = 0f
        for (k in 0 until 8) {
            val bl = combL[k][ciL[k]]
            var fl = bl * (1f - damp) + fL[k] * damp
            if (abs(fl) < 1e-15f) fl = 0f
            fL[k] = fl
            combL[k][ciL[k]] = x + fl * feedback
            if (++ciL[k] >= combL[k].size) ciL[k] = 0
            l += bl

            val br = combR[k][ciR[k]]
            var fr = br * (1f - damp) + fR[k] * damp
            if (abs(fr) < 1e-15f) fr = 0f
            fR[k] = fr
            combR[k][ciR[k]] = x + fr * feedback
            if (++ciR[k] >= combR[k].size) ciR[k] = 0
            r += br
        }
        for (k in 0 until 4) {
            val bl = apL[k][aiL[k]]
            val yl = -l + bl
            apL[k][aiL[k]] = l + bl * 0.5f
            if (++aiL[k] >= apL[k].size) aiL[k] = 0
            l = yl

            val br = apR[k][aiR[k]]
            val yr = -r + br
            apR[k][aiR[k]] = r + br * 0.5f
            if (++aiR[k] >= apR[k].size) aiR[k] = 0
            r = yr
        }
        out[0] = l
        out[1] = r
    }
}

// ------------------------------------------------------------------ Engine

/**
 * Real-time worship pad synth. AudioTrack, 44.1 kHz, stereo, float PCM.
 * Lahat ng setter ay ligtas tawagin mula sa UI thread.
 */
class PadSynth {
    @Volatile private var running = false
    @Volatile private var targetKey = -1
    @Volatile private var presetIdx = 0
    @Volatile private var tone = 0.5f
    @Volatile private var brightness = 0.5f
    @Volatile private var volPad = 0.7f
    @Volatile private var volMaster = 0.8f

    private var thread: Thread? = null

    @Synchronized
    fun start() {
        if (thread?.isAlive == true) return
        running = true
        thread = Thread({ renderLoop() }, "PadSynth").also { it.start() }
    }

    @Synchronized
    fun stop() {
        running = false
        thread?.join(2000)
        thread = null
    }

    /** key: 0 = C ... 11 = B, -1 = patayin (fade out). */
    fun setKey(key: Int) { targetKey = key.coerceIn(-1, 11) }
    fun setPreset(preset: PadPreset) { presetIdx = preset.ordinal }
    fun setTone(v: Float) { tone = v.coerceIn(0f, 1f) }
    fun setBrightness(v: Float) { brightness = v.coerceIn(0f, 1f) }
    fun setPadVolume(v: Float) { volPad = v.coerceIn(0f, 1f) }
    fun setMasterVolume(v: Float) { volMaster = v.coerceIn(0f, 1f) }

    private fun renderLoop() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)

        val chunk = 512
        val minBuf = AudioTrack.getMinBufferSize(SR, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_FLOAT)
        val bufBytes = max(minBuf, 4096 * 2 * 4)
        val track: AudioTrack = try {
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build(),
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                        .setSampleRate(SR)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                        .build(),
                )
                .setBufferSizeInBytes(bufBytes)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
        } catch (e: Exception) {
            running = false
            return
        }
        if (track.state != AudioTrack.STATE_INITIALIZED) {
            track.release()
            running = false
            return
        }
        track.play()

        val out = FloatArray(chunk * 2)
        val vl = FloatArray(BLOCK)
        val vr = FloatArray(BLOCK)
        val rev = FloatArray(2)
        val voices = ArrayList<Voice>()
        val reverb = Reverb()

        var toneS = tone
        var brS = brightness
        var padG = 0f      // nagsisimula sa 0 -> walang pop sa simula
        var masG = 0f
        var wetS = 0f
        var fbS = SPECS[0].room
        var dampS = SPECS[0].damp
        var curKey = -1
        var curPreset = -1
        var seed = 1
        var closingChunks = 0

        val smooth = 1f - exp(-1f / (SR * 0.03f))            // ~30 ms, bawat sample
        val blockSmooth = 1f - exp(-BLOCK.toFloat() / (SR * 0.08f)) // ~80 ms, bawat block

        while (true) {
            val closing = !running
            if (closing && ++closingChunks > 14) break

            // ---- Control (galing UI)
            val key = targetKey
            val pr = presetIdx
            val keyChanged = key != curKey
            val presetChanged = pr != curPreset
            if (!closing && (keyChanged || presetChanged)) {
                if (keyChanged || key >= 0) {
                    val dur = if (key < 0) 2.0f else 2.5f // fade out 2s, crossfade 2.5s
                    for (v in voices) {
                        if (v.dir > 0) {
                            v.dir = -1
                            v.rate = 1f / (SR * dur)
                        }
                    }
                    if (key >= 0) {
                        val hz = 130.8128f * 2f.pow(key / 12f) // C3..B3
                        val v = Voice(SPECS[pr], hz, seed++)
                        v.dir = 1
                        v.rate = 1f / (SR * 2.5f)
                        voices.add(v)
                    }
                }
                curKey = key
                curPreset = pr
            }
            // Limitahan ang dami ng voices kapag mabilis ang pag-tap.
            if (voices.size > 4) {
                for (i in 0 until voices.size - 4) {
                    val v = voices[i]
                    if (v.dir < 0) v.rate = max(v.rate, 1f / (SR * 0.5f))
                }
            }

            val padT = if (closing) 0f else volPad * volPad
            val masT = if (closing) 0f else volMaster * volMaster

            var frame = 0
            while (frame < chunk) {
                toneS += (tone - toneS) * blockSmooth
                brS += (brightness - brS) * blockSmooth
                val cutScale = 2f.pow((toneS - 0.5f) * 3f)
                val shimScale = 0.2f + 1.6f * brS

                java.util.Arrays.fill(vl, 0f)
                java.util.Arrays.fill(vr, 0f)
                for (v in voices) v.render(vl, vr, BLOCK, cutScale, shimScale)
                for (i in voices.size - 1 downTo 0) if (voices[i].dead) voices.removeAt(i)

                val spec = SPECS[if (curPreset < 0) 0 else curPreset]
                fbS += (spec.room - fbS) * blockSmooth
                dampS += (spec.damp - dampS) * blockSmooth
                reverb.feedback = fbS
                reverb.damp = dampS

                for (j in 0 until BLOCK) {
                    padG += (padT - padG) * smooth
                    masG += (masT - masG) * smooth
                    wetS += (spec.wet - wetS) * smooth
                    val dl = vl[j]
                    val dr = vr[j]
                    reverb.process((dl + dr) * 0.5f, rev)
                    val gain = padG * masG * MAKEUP
                    val idx = (frame + j) * 2
                    out[idx] = tanh((dl + rev[0] * wetS) * gain)
                    out[idx + 1] = tanh((dr + rev[1] * wetS) * gain)
                }
                frame += BLOCK
            }

            track.write(out, 0, out.size, AudioTrack.WRITE_BLOCKING)
        }

        try {
            track.stop()
        } catch (_: Exception) {
        }
        track.release()
    }
}
