package com.padkeys.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import android.os.SystemClock
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

const val MAX_CHANNELS = 16
const val MAX_SCENES = 16
const val KEY_STEPS = 36 // saklaw ng layers: 36 puting key (C2 hanggang dulo ng 61 keys)

private var nextChannelUid = 0

class ChannelState(
    initialNumber: Int,
    initialVolume: Float = 0.7f,
    startKey: Int = 0,
    endKey: Int = KEY_STEPS,
) {
    val uid = nextChannelUid++
    var number by mutableIntStateOf(initialNumber)
    val color: Color get() = ChannelColors[(number - 1) % ChannelColors.size]

    // Walang instrument muna = blangko ang pangalan
    var name by mutableStateOf("")
    var instrumentId by mutableStateOf<String?>(null)

    var volume by mutableFloatStateOf(initialVolume)
    var mute by mutableStateOf(false)
    var solo by mutableStateOf(false)
    var fx by mutableStateOf(false)

    // Saklaw ng layer sa keyboard, sa puting key: lo ..< hi (0..36)
    var lo by mutableIntStateOf(startKey)
    var hi by mutableIntStateOf(endKey)

    fun setRange(newLo: Int, newHi: Int) {
        lo = newLo.coerceIn(0, KEY_STEPS - 1)
        hi = newHi.coerceIn(lo + 1, KEY_STEPS)
    }
}

class Song(val title: String, val patch: String)

class SceneSnapshot(
    val volumes: List<Float>,
    val mutes: List<Boolean>,
    val solos: List<Boolean>,
    val fx: List<Boolean>,
)

class SceneState(name: String, transition: Float = 1f) {
    var name by mutableStateOf(name)
    var transition by mutableFloatStateOf(transition) // segundo ng fade papunta sa scene na ito
    var snapshot: SceneSnapshot? = null
}

class PadKeysState : ViewModel() {
    // ---- Audio engine (Ambient Pads). Nakatali sa ViewModel kaya hindi nauulit kapag nag-rotate.
    private val synth = PadSynth()

    private var _master by mutableFloatStateOf(0.8f)
    var master: Float
        get() = _master
        set(v) {
            _master = v.coerceIn(0f, 1f)
            synth.setMasterVolume(_master)
        }

    private var _padVolume by mutableFloatStateOf(0.7f)
    var padVolume: Float
        get() = _padVolume
        set(v) {
            _padVolume = v.coerceIn(0f, 1f)
            synth.setPadVolume(_padVolume)
        }

    private var _padKey by mutableIntStateOf(-1)
    /** Aktibong pad key: 0 = C ... 11 = B, -1 = walang naka-on. */
    var dronePad: Int
        get() = _padKey
        set(v) {
            _padKey = v
            synth.setKey(v)
        }

    private var _preset by mutableStateOf(PadPreset.WarmWorship)
    var preset: PadPreset
        get() = _preset
        set(v) {
            _preset = v
            synth.setPreset(v)
        }

    private var _tone by mutableFloatStateOf(0.5f)
    var tone: Float
        get() = _tone
        set(v) {
            _tone = v.coerceIn(0f, 1f)
            synth.setTone(_tone)
        }

    private var _brightness by mutableFloatStateOf(0.5f)
    var brightness: Float
        get() = _brightness
        set(v) {
            _brightness = v.coerceIn(0f, 1f)
            synth.setBrightness(_brightness)
        }

    init {
        synth.setMasterVolume(_master)
        synth.setPadVolume(_padVolume)
        synth.setKey(_padKey)
        synth.setPreset(_preset)
        synth.setTone(_tone)
        synth.setBrightness(_brightness)
    }

    fun startAudio() = synth.start()
    fun stopAudio() = synth.stop()

    override fun onCleared() {
        synth.stop()
    }

    // Lahat ng channel ay walang instrument muna.
    val channels = mutableStateListOf(
        ChannelState(1, 0.78f, 0, 36),
        ChannelState(2, 0.62f, 0, 18),
        ChannelState(3, 0.85f, 18, 36),
        ChannelState(4, 0.54f, 0, 13),
        ChannelState(5, 0.70f, 22, 36),
    )

    // Setlist (Song + Patch)
    val songs = mutableStateListOf(
        Song("Song 1", "Patch 1"),
        Song("Song 2", "Patch 2"),
        Song("Song 3", "Patch 3"),
    )
    var selectedSong by mutableIntStateOf(0)

    // Scenes
    val scenes = mutableStateListOf(
        SceneState("Scene 1"), SceneState("Scene 2"), SceneState("Scene 3"), SceneState("Scene 4"),
    )
    var scene by mutableIntStateOf(0)
    var scenePopup by mutableIntStateOf(-1) // index ng scene na may bukas na options

    // Layers editor
    var layerMode by mutableStateOf(false)
    var layerSel by mutableIntStateOf(0)

    // Sound Library (popup kapag tinap ang channel)
    val library = SoundLibrary()
    var channelPopup by mutableIntStateOf(-1) // index ng channel na may bukas na library

    // Keyboard
    var keyboardVisible by mutableStateOf(true)
    var keyCount by mutableIntStateOf(61)
    var pitch by mutableFloatStateOf(0.5f)
    var mod by mutableFloatStateOf(0f)
    var pressed by mutableStateOf(setOf<Int>())

    fun noteOn(note: Int) {
        pressed = pressed + note
        // TODO: dito ikokonekta ang tunog (sound engine)
    }

    fun noteOff(note: Int) {
        pressed = pressed - note
    }

    fun addChannel() {
        if (channels.size >= MAX_CHANNELS) return
        channels.add(ChannelState(channels.size + 1))
    }

    fun deleteChannel(index: Int) {
        if (channels.size <= 1 || index !in channels.indices) return
        channels.removeAt(index)
        channels.forEachIndexed { i, c -> c.number = i + 1 }
        layerSel = 0
        channelPopup = -1
    }

    fun prevSong() {
        if (selectedSong > 0) selectedSong--
    }

    fun nextSong() {
        if (selectedSong < songs.lastIndex) selectedSong++
    }

    fun addSong() {
        val n = songs.size + 1
        songs.add(Song("Song $n", "Patch $n"))
        selectedSong = songs.lastIndex
    }

    private var fadeJob: Job? = null

    /** Lumipat sa scene. May fade ng volume ayon sa Transition time ng scene. */
    fun selectScene(index: Int) {
        if (index !in scenes.indices) return
        scene = index
        val target = scenes[index]
        val snap = target.snapshot ?: return
        fadeJob?.cancel()

        val chs = channels.toList()
        val from = chs.map { it.volume }
        chs.forEachIndexed { k, c ->
            if (k < snap.volumes.size) {
                c.mute = snap.mutes[k]
                c.solo = snap.solos[k]
                c.fx = snap.fx[k]
            }
        }
        val millis = target.transition * 1000f
        if (millis <= 0f) {
            chs.forEachIndexed { k, c -> if (k < snap.volumes.size) c.volume = snap.volumes[k] }
            return
        }
        fadeJob = viewModelScope.launch {
            val t0 = SystemClock.uptimeMillis()
            while (true) {
                val t = ((SystemClock.uptimeMillis() - t0) / millis).coerceIn(0f, 1f)
                chs.forEachIndexed { k, c ->
                    if (k < snap.volumes.size) c.volume = from[k] + (snap.volumes[k] - from[k]) * t
                }
                if (t >= 1f) break
                delay(16)
            }
        }
    }

    fun saveScene() {
        scenes[scene].snapshot = SceneSnapshot(
            volumes = channels.map { it.volume },
            mutes = channels.map { it.mute },
            solos = channels.map { it.solo },
            fx = channels.map { it.fx },
        )
    }

    fun addScene() {
        if (scenes.size >= MAX_SCENES) return
        scenes.add(SceneState("Scene ${scenes.size + 1}"))
        scene = scenes.lastIndex
    }

    fun deleteScene(index: Int) {
        if (scenes.size <= 1 || index !in scenes.indices) return
        scenes.removeAt(index)
        scene = when {
            scene > index -> scene - 1
            else -> scene
        }.coerceIn(0, scenes.lastIndex)
        scenePopup = -1
    }
}
