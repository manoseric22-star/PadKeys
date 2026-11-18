package com.padkeys.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

const val MAX_CHANNELS = 16

class ChannelState(
    val number: Int,
    val name: String,
    val color: Color,
    initialVolume: Float,
    initialRangeStart: Float,
    initialRangeEnd: Float,
) {
    var volume by mutableFloatStateOf(initialVolume)
    var mute by mutableStateOf(false)
    var solo by mutableStateOf(false)
    var fx by mutableStateOf(false)

    // Saklaw ng layer sa keyboard (0f = pinakakaliwa, 1f = pinakakanan)
    var rangeStart by mutableFloatStateOf(initialRangeStart)
    var rangeEnd by mutableFloatStateOf(initialRangeEnd)
}

class Song(val title: String, val patch: String)

class SceneSnapshot(
    val volumes: List<Float>,
    val mutes: List<Boolean>,
    val solos: List<Boolean>,
    val fx: List<Boolean>,
)

class PadKeysState {
    var master by mutableFloatStateOf(0.8f)

    val channels = mutableStateListOf(
        ChannelState(1, "Strings", ChannelColors[0], 0.78f, 0f, 1f),
        ChannelState(2, "Pad", ChannelColors[1], 0.62f, 0f, 0.5f),
        ChannelState(3, "Piano", ChannelColors[2], 0.85f, 0.5f, 1f),
        ChannelState(4, "Bass", ChannelColors[3], 0.54f, 0f, 0.35f),
        ChannelState(5, "Synth Lead", ChannelColors[4], 0.70f, 0.6f, 1f),
    )

    // Setlist (Song + Patch)
    val songs = mutableStateListOf(
        Song("Song 1", "Patch 1"),
        Song("Song 2", "Patch 2"),
        Song("Song 3", "Patch 3"),
    )
    var selectedSong by mutableIntStateOf(0)

    // Drone pads (-1 = walang naka-on)
    var dronePad by mutableIntStateOf(-1)

    // Scenes
    val scenes = mutableStateListOf<SceneSnapshot?>(null, null, null, null)
    var scene by mutableIntStateOf(0)

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
        val n = channels.size + 1
        // Walang instrument muna (blangko ang pangalan)
        channels.add(ChannelState(n, "", ChannelColors[(n - 1) % ChannelColors.size], 0.7f, 0f, 1f))
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

    fun selectScene(index: Int) {
        scene = index
        scenes[index]?.let { s ->
            channels.forEachIndexed { k, c ->
                // Ang mga channel na idinagdag pagkatapos i-save ang scene ay hindi ginagalaw.
                if (k < s.volumes.size) {
                    c.volume = s.volumes[k]
                    c.mute = s.mutes[k]
                    c.solo = s.solos[k]
                    c.fx = s.fx[k]
                }
            }
        }
    }

    fun saveScene() {
        scenes[scene] = SceneSnapshot(
            volumes = channels.map { it.volume },
            mutes = channels.map { it.mute },
            solos = channels.map { it.solo },
            fx = channels.map { it.fx },
        )
    }

    fun addScene() {
        if (scenes.size < 8) {
            scenes.add(null)
            scene = scenes.lastIndex
        }
    }
}
