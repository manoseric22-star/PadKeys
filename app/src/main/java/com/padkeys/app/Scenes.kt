package com.padkeys.app

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

val DangerRed = Color(0xFFD93C42)
val DangerSoft = Color(0xFFFF7B80)

/** Madilim na background na humaharang ng tap; ang content ay nasa gitna. */
@Composable
fun Scrim(modifier: Modifier = Modifier, alpha: Float = 0.68f, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier
            .fillMaxSize()
            .background(Color(0xFF05080C).copy(alpha = alpha))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
        contentAlignment = Alignment.Center,
        content = content,
    )
}

/** Textbox na kapareho ng itsura sa preview. */
@Composable
fun TextBox(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    leading: (@Composable () -> Unit)? = null,
) {
    val shape = RoundedCornerShape(10.dp)
    Row(
        modifier
            .background(C.Track, shape)
            .border(1.dp, C.BtnBorder, shape)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        leading?.invoke()
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(color = C.Text, fontSize = 16.sp, fontWeight = FontWeight.Medium),
            cursorBrush = SolidColor(C.Accent),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            modifier = Modifier.weight(1f),
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) Text(placeholder, color = C.Muted, fontSize = 16.sp)
                    inner()
                }
            },
        )
    }
}

// ---------------------------------------------------------------- Scenes bar

@Composable
fun ScenesBar(state: PadKeysState, modifier: Modifier) {
    val listState = rememberLazyListState()
    val count = state.scenes.size

    LaunchedEffect(count) {
        if (count > 0) listState.animateScrollToItem(state.scene.coerceIn(0, count - 1))
    }

    Panel(modifier) {
        Row(
            Modifier.fillMaxSize().padding(start = 16.dp, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            LazyRow(
                Modifier.weight(1f).fillMaxHeight(),
                state = listState,
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Tap = lumipat sa scene. Long press = options (Rename / Transition / Edit / Delete)
                itemsIndexed(state.scenes, key = { _, s -> System.identityHashCode(s) }) { i, sc ->
                    val on = state.scene == i
                    val shape = RoundedCornerShape(8.dp)
                    Box(
                        Modifier
                            .width(96.dp)
                            .height(44.dp)
                            .clip(shape)
                            .background(if (on) C.Accent else C.Btn)
                            .border(1.dp, if (on) C.Accent else C.BtnBorder, shape)
                            .pointerInput(i) {
                                detectTapGestures(
                                    onTap = { state.selectScene(i) },
                                    onLongPress = { state.scenePopup = i },
                                )
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            sc.name.ifBlank { "Scene ${i + 1}" },
                            color = if (on) C.Ink else C.Text,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            modifier = Modifier.padding(horizontal = 8.dp),
                        )
                    }
                }
            }

            // Add | Save
            val pill = RoundedCornerShape(8.dp)
            Row(
                Modifier
                    .height(44.dp)
                    .clip(pill)
                    .background(C.Btn)
                    .border(1.dp, C.BtnBorder, pill),
            ) {
                PillAction("Add") { state.addScene() }
                Box(Modifier.width(1.dp).fillMaxHeight().background(C.BtnBorder))
                PillAction("Save") { state.saveScene() }
            }

            Spacer(Modifier.width(8.dp))

            // Layers
            val big = RoundedCornerShape(12.dp)
            val ly = state.layerMode
            Box(
                Modifier
                    .size(48.dp)
                    .clip(big)
                    .background(
                        if (ly) Brush.verticalGradient(listOf(Color(0x806EB4FF), Color(0x806EB4FF)))
                        else Brush.verticalGradient(listOf(Color(0xFF2A3A5E), Color(0xFF17213A))),
                    )
                    .border(1.dp, if (ly) Color(0x9987BEFF) else Color(0xFF3A4C78), big)
                    .clickable(role = Role.Button) { state.layerMode = !state.layerMode },
                contentAlignment = Alignment.Center,
            ) {
                LayersIcon(Modifier.size(28.dp), Color.White)
            }

            KeyboardToggle(state)
        }
    }
}

@Composable
fun KeyboardToggle(state: PadKeysState) {
    val big = RoundedCornerShape(12.dp)
    val kbVisible = state.keyboardVisible
    Box(
        Modifier
            .size(48.dp)
            .clip(big)
            .background(if (kbVisible) Color(0x806EB4FF) else Color(0x1F6EB4FF))
            .border(1.dp, if (kbVisible) Color(0x9987BEFF) else Color(0x4D87BEFF), big)
            .clickable(role = Role.Button) { state.keyboardVisible = !state.keyboardVisible },
        contentAlignment = Alignment.Center,
    ) {
        PianoIcon(Modifier.size(30.dp).alpha(if (kbVisible) 1f else 0.45f))
    }
}

@Composable
private fun PillAction(label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .width(72.dp)
            .fillMaxHeight()
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = C.Text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

// ---------------------------------------------------------------- Scene options popup

@Composable
fun SceneOptions(state: PadKeysState) {
    val index = state.scenePopup
    val sc = state.scenes.getOrNull(index) ?: return
    var confirmDelete by remember(index) { mutableStateOf(false) }
    val many = state.scenes.size > 1
    val close = { state.scenePopup = -1 }

    BackHandler(onBack = close)

    Scrim {
        val shape = RoundedCornerShape(16.dp)
        Column(
            Modifier
                .width(460.dp)
                .clip(shape)
                .background(C.Panel)
                .border(1.dp, C.BtnBorder, shape),
        ) {
            // Header
            Row(
                Modifier.fillMaxWidth().padding(start = 24.dp, end = 12.dp, top = 14.dp, bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        "SCENE OPTIONS",
                        color = C.Muted,
                        fontFamily = Condensed,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        letterSpacing = 1.5.sp,
                    )
                    Text(
                        sc.name.ifBlank { "Scene ${index + 1}" },
                        color = C.Text,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                    )
                }
                Box(
                    Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)).clickable(role = Role.Button, onClick = close),
                    contentAlignment = Alignment.Center,
                ) { CloseIcon(Modifier.size(22.dp), C.Muted) }
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(C.PanelBorder))

            // Body
            Column(
                Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                // Rename
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PopupLabel("Name")
                    TextBox(
                        sc.name,
                        { sc.name = it },
                        Modifier.fillMaxWidth().height(48.dp),
                        placeholder = "Scene ${index + 1}",
                        leading = { PencilIcon(Modifier.size(18.dp), C.Muted) },
                    )
                }

                // Transition
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                        PopupLabel("Transition time")
                        Spacer(Modifier.weight(1f))
                        Text("Fade papunta sa scene na ito", color = C.Muted, fontSize = 12.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Btn(Modifier.size(48.dp), shape = RoundedCornerShape(10.dp), onClick = {
                            sc.transition = (sc.transition - 0.5f).coerceAtLeast(0f)
                        }) { MinusIcon(Modifier.size(20.dp), C.Text) }
                        Box(
                            Modifier
                                .weight(1f)
                                .height(48.dp)
                                .background(C.Track, RoundedCornerShape(10.dp))
                                .border(1.dp, C.BtnBorder, RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    "%.1f".format(sc.transition),
                                    color = C.Text,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text("seconds", color = C.Muted, fontSize = 14.sp, modifier = Modifier.padding(bottom = 3.dp))
                            }
                        }
                        Btn(Modifier.size(48.dp), shape = RoundedCornerShape(10.dp), onClick = {
                            sc.transition = (sc.transition + 0.5f).coerceAtMost(10f)
                        }) { PlusIcon(Modifier.size(20.dp), C.Text) }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (v in listOf(0, 1, 2, 3, 5)) {
                            val on = sc.transition == v.toFloat()
                            Box(
                                Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (on) C.AccentDeep else C.Btn)
                                    .border(BorderStroke(1.dp, if (on) C.Accent else C.BtnBorder), RoundedCornerShape(8.dp))
                                    .clickable(role = Role.Button) { sc.transition = v.toFloat() },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("${v}s", color = C.Text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                // Edit scene
                val rowShape = RoundedCornerShape(10.dp)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(rowShape)
                        .background(C.Strip)
                        .border(1.dp, C.BtnBorder, rowShape)
                        .clickable(role = Role.Button) {
                            state.selectScene(index)
                            state.scenePopup = -1
                        }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Box(
                        Modifier.size(40.dp).background(C.AccentDeep, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center,
                    ) { SlidersIcon(Modifier.size(22.dp), Color(0xFF8FBBFF), C.AccentDeep) }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("Edit scene", color = C.Text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Text("I-load ang scene, ayusin ang mga channel, tapos Save", color = C.Muted, fontSize = 12.sp)
                    }
                    ChevronIcon(Modifier.size(20.dp), C.Muted)
                }
            }

            Box(Modifier.fillMaxWidth().height(1.dp).background(C.PanelBorder))

            // Footer
            Row(
                Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 14.dp, bottom = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val dShape = RoundedCornerShape(10.dp)
                Row(
                    Modifier
                        .height(44.dp)
                        .alpha(if (many) 1f else 0.4f)
                        .clip(dShape)
                        .background(if (confirmDelete) DangerRed else Color.Transparent)
                        .border(1.dp, if (confirmDelete) DangerRed else DangerSoft.copy(alpha = 0.45f), dShape)
                        .clickable(enabled = many, role = Role.Button) {
                            if (!confirmDelete) confirmDelete = true else state.deleteScene(index)
                        }
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TrashIcon(Modifier.size(18.dp), if (confirmDelete) Color.White else DangerSoft)
                    Text(
                        if (confirmDelete) "Tap again to confirm" else "Delete scene",
                        color = if (confirmDelete) Color.White else DangerSoft,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Spacer(Modifier.weight(1f))
                Btn(Modifier.height(44.dp).width(96.dp), shape = dShape, base = C.Btn, onClick = close) {
                    Text("Cancel", color = C.Text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.width(8.dp))
                Btn(Modifier.height(44.dp).width(100.dp), selected = true, shape = dShape, onClick = close) {
                    Text("Save", color = C.Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
fun PopupLabel(text: String) {
    Text(
        text.uppercase(),
        color = C.Muted,
        fontFamily = Condensed,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        letterSpacing = 1.5.sp,
    )
}
