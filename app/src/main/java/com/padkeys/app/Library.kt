package com.padkeys.app

import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.runtime.mutableStateListOf
import java.io.BufferedInputStream
import java.io.DataInputStream
import java.io.EOFException
import java.io.InputStream

/** Isang na-import na .sf2 file at ang mga instrument (preset) sa loob nito. */
class SoundFont(val uri: Uri, val file: String, presets: List<String>) {
    val presets = mutableStateListOf<String>().also { it.addAll(presets) }
}

/** Isang na-import na MP3/WAV. */
class Sample(val uri: Uri, val name: String, val fmt: String, val dur: String)

/** Isang piraso na puwedeng piliin sa Sound Library. */
class SoundItem(
    val id: String,
    val isSoundfont: Boolean,
    val name: String,
    val group: String,   // pangalan ng sf2 file
    val meta: String,    // sample: haba
    val badge: String,   // SF2 / WAV / MP3
)

class SoundLibrary {
    val soundfonts = mutableStateListOf<SoundFont>()
    val samples = mutableStateListOf<Sample>()
    val favorites = mutableStateListOf<String>()

    fun allItems(): List<SoundItem> {
        val out = ArrayList<SoundItem>()
        for (sf in soundfonts) for (p in sf.presets) {
            out.add(SoundItem("sf:${sf.file}:$p", true, p, sf.file, sf.file, "SF2"))
        }
        for (sm in samples) {
            out.add(SoundItem("sm:${sm.name}", false, sm.name, "Sampler", sm.dur, sm.fmt))
        }
        return out
    }

    fun toggleFavorite(id: String) {
        if (!favorites.remove(id)) favorites.add(id)
    }

    fun delete(item: SoundItem) {
        favorites.remove(item.id)
        if (item.isSoundfont) {
            val sf = soundfonts.firstOrNull { it.file == item.group } ?: return
            sf.presets.remove(item.name)
            if (sf.presets.isEmpty()) soundfonts.remove(sf)
        } else {
            samples.removeAll { it.name == item.name }
        }
    }

    // ---- Import (tinatawag sa background thread)

    fun importSoundfont(ctx: Context, uri: Uri) {
        keepAccess(ctx, uri)
        val name = displayName(ctx, uri) ?: "Soundfont.sf2"
        val presets = try {
            ctx.contentResolver.openInputStream(uri)?.use { readSf2Presets(it) } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
        if (presets.isEmpty()) return
        if (soundfonts.any { it.file == name }) return
        soundfonts.add(SoundFont(uri, name, presets))
    }

    fun importSample(ctx: Context, uri: Uri) {
        keepAccess(ctx, uri)
        val name = displayName(ctx, uri) ?: "Sample"
        if (samples.any { it.name == name }) return
        val fmt = name.substringAfterLast('.', "").uppercase().ifEmpty { "AUDIO" }
        samples.add(Sample(uri, name, fmt, duration(ctx, uri)))
    }

    private fun keepAccess(ctx: Context, uri: Uri) {
        try {
            ctx.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (_: Exception) {
        }
    }

    private fun displayName(ctx: Context, uri: Uri): String? = try {
        ctx.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        }
    } catch (e: Exception) {
        null
    }

    private fun duration(ctx: Context, uri: Uri): String {
        val r = MediaMetadataRetriever()
        return try {
            r.setDataSource(ctx, uri)
            val ms = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            val s = ms / 1000
            "${s / 60}:${(s % 60).toString().padStart(2, '0')}"
        } catch (e: Exception) {
            "--:--"
        } finally {
            try {
                r.release()
            } catch (_: Exception) {
            }
        }
    }
}

// ------------------------------------------------------------------ SF2 reader (preset names lang)

private fun DataInputStream.readId(): String? {
    val b = ByteArray(4)
    return try {
        readFully(b)
        String(b, Charsets.ISO_8859_1)
    } catch (e: EOFException) {
        null
    }
}

private fun DataInputStream.readLE32(): Long? {
    val b = ByteArray(4)
    return try {
        readFully(b)
        (b[0].toLong() and 0xFF) or ((b[1].toLong() and 0xFF) shl 8) or
            ((b[2].toLong() and 0xFF) shl 16) or ((b[3].toLong() and 0xFF) shl 24)
    } catch (e: EOFException) {
        null
    }
}

private fun DataInputStream.skipFully(n: Long) {
    var left = n
    while (left > 0) {
        val s = skip(left)
        if (s > 0) {
            left -= s
        } else {
            if (read() < 0) throw EOFException()
            left--
        }
    }
}

/** Binabasa ang listahan ng preset (instrument) mula sa 'phdr' chunk ng isang .sf2. */
fun readSf2Presets(input: InputStream): List<String> {
    val d = DataInputStream(BufferedInputStream(input, 1 shl 16))
    val found = ArrayList<Triple<Int, Int, String>>() // bank, preset, name
    try {
        val head = ByteArray(12)
        d.readFully(head)
        if (String(head, 0, 4, Charsets.ISO_8859_1) != "RIFF" ||
            String(head, 8, 4, Charsets.ISO_8859_1) != "sfbk"
        ) return emptyList()

        while (true) {
            val id = d.readId() ?: break
            val size = d.readLE32() ?: break
            if (id == "LIST") {
                val type = d.readId() ?: break
                if (type == "pdta") {
                    var remaining = size - 4
                    while (remaining >= 8) {
                        val sid = d.readId() ?: break
                        val ssz = d.readLE32() ?: break
                        remaining -= 8
                        if (sid == "phdr") {
                            val buf = ByteArray(ssz.toInt())
                            d.readFully(buf)
                            val count = buf.size / 38
                            for (i in 0 until count - 1) { // huling record ay "EOP"
                                val off = i * 38
                                var end = 0
                                while (end < 20 && buf[off + end].toInt() != 0) end++
                                val name = String(buf, off, end, Charsets.ISO_8859_1).trim()
                                val preset = (buf[off + 20].toInt() and 0xFF) or ((buf[off + 21].toInt() and 0xFF) shl 8)
                                val bank = (buf[off + 22].toInt() and 0xFF) or ((buf[off + 23].toInt() and 0xFF) shl 8)
                                if (name.isNotEmpty()) found.add(Triple(bank, preset, name))
                            }
                            return found.sortedWith(compareBy({ it.first }, { it.second })).map { it.third }.distinct()
                        } else {
                            d.skipFully(ssz)
                        }
                        remaining -= ssz
                        if (ssz and 1L == 1L) {
                            d.skipFully(1)
                            remaining -= 1
                        }
                    }
                } else {
                    d.skipFully(size - 4)
                }
            } else {
                d.skipFully(size)
            }
            if (size and 1L == 1L) d.skipFully(1)
        }
    } catch (e: Exception) {
        // sira o putol na file: ibalik kung ano ang nabasa
    }
    return found.sortedWith(compareBy({ it.first }, { it.second })).map { it.third }.distinct()
}
