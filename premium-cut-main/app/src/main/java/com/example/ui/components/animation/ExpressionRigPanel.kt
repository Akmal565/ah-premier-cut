package com.example.ui.components.animation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.ClipKeyframe
import com.example.engine.effects.FaceExpressionPreset
import com.example.engine.effects.FaceExpressionResolver
import com.example.engine.effects.FaceExpressionState
import com.example.engine.motion.BodyMotionGenerator
import com.example.engine.motion.BodyMotionPreset
import com.example.engine.motion.BodyMotionState
import com.example.engine.motion.RigFocus
import com.example.ui.StudioViewModel
import com.example.ui.theme.SkyBlue
import kotlin.math.roundToInt

// Styling (mirrors the sibling animation panels; one accent marks every active item).
private val ErPanelBg = Color(0xFF081020)
private val ErCardBg = Color(0xFF0D1B2E)
private val ErCardSelected = Color(0xFF12325A)
private val ErSectionBg = Color(0xFF0A1626)
private val ErBorder = Color(0xFF1B2C47)
private val ErTextPrimary = Color(0xFFE8F0FA)
private val ErTextSecondary = Color(0xFF94A3B8)
private val ErTextTertiary = Color(0xFF5B6B84)
private val ErAccent = SkyBlue

private enum class ExpressionRigTab(val label: String, val testTag: String) {
    FACE("Face", "exprig_tab_face"),
    BODY("Body", "exprig_tab_body"),
    SCRIPTS("Scripts", "exprig_tab_scripts"),
}

/**
 * Expression & Rig - redesigned animation panel (Face expressions + Body motion).
 *
 * Face tab: drives the REAL face mesh-warp channels (DeformationParams) through
 * StudioViewModel.setFaceDeform - the same pipeline the Face effects render with, in preview
 * and export. Presets are real channel sets, sliders are real signed warp channels, Auto blink
 * is a real time-based blink channel. (Lip sync / Eye track are intentionally ABSENT: there is
 * no real audio-envelope or gaze-target source in the engine yet, and this panel ships no fake
 * controls.)
 *
 * Body tab: generates REAL ClipKeyframes (position / scale / rotation) on the selected clip
 * via BodyMotionGenerator - the existing keyframe interpolator renders the motion everywhere.
 *
 * Scripts tab: keeps the existing AE-style expression editor + bone rig (MotionScriptPanel)
 * reachable - no feature regression from the redesign.
 */
@Composable
fun ExpressionRigPanel(
    viewModel: StudioViewModel,
    layerId: String?,
    isVideoLayer: Boolean,
    layerDurationMs: Long,
    layerStartMs: Long,
    currentPosMs: Long,
    currentKeyframes: List<ClipKeyframe>,
    onReplaceKeyframes: (List<ClipKeyframe>) -> Unit,
    onAddKeyframe: (ClipKeyframe) -> Unit,
    modifier: Modifier = Modifier
) {
    var activeTab by remember { mutableStateOf(ExpressionRigTab.FACE) }

    // Hoisted tab state so the header Reset acts on the ACTIVE tab (PDF: reset returns all
    // sliders to their default values). Face state hydrates once from the clip's rendered
    // params so undo / project load is reflected; while open, edits are the source of truth.
    var faceState by remember(layerId) {
        mutableStateOf(
            layerId?.let { viewModel.faceDeform.value[it] }
                ?.let { FaceExpressionResolver.toState(it) }
                ?: FaceExpressionState.DEFAULT
        )
    }
    var bodyState by remember(layerId) { mutableStateOf(BodyMotionState.DEFAULT) }

    fun applyFace(next: FaceExpressionState) {
        faceState = next
        if (layerId != null && isVideoLayer) {
            // The panel owns the EXPRESSION channels; beauty-only channels the user set in
            // other panels (skin smooth, teeth whiten, nose/chin, head warps, curls) survive.
            viewModel.setFaceDeform(layerId) { cur ->
                val r = FaceExpressionResolver.resolve(next)
                cur.copy(
                    eyeEnlarge = r.eyeEnlarge, faceSlim = r.faceSlim, jawSharp = r.jawSharp,
                    chubby = r.chubby, wink = r.wink, smileAdjust = r.smileAdjust,
                    browAdjust = r.browAdjust, autoBlink = r.autoBlink,
                )
            }
        }
    }

    fun generateBody(next: BodyMotionState) {
        val id = layerId ?: return
        if (next.preset == null) return
        val rel = (currentPosMs - layerStartMs).coerceIn(0L, layerDurationMs)
        val anchor = BodyMotionGenerator.poseAt(currentKeyframes, rel)
        val motion = BodyMotionGenerator.generate(
            state = next,
            clipDurationMs = layerDurationMs,
            playheadRelMs = rel,
            baseX = anchor?.posX ?: 0f,
            baseY = anchor?.posY ?: 0f,
        )
        if (motion.isNotEmpty()) onReplaceKeyframes(motion)
    }

    fun resetActiveTab() {
        when (activeTab) {
            ExpressionRigTab.FACE -> {
                faceState = FaceExpressionState.DEFAULT
                if (layerId != null && isVideoLayer) viewModel.resetFaceDeform(layerId)
            }
            ExpressionRigTab.BODY -> bodyState = BodyMotionState.DEFAULT
            ExpressionRigTab.SCRIPTS -> Unit
        }
    }

    Column(modifier = modifier.fillMaxSize().background(ErPanelBg)) {
        // Header: title + Reset (returns all sliders to their default values)
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Expression & Rig",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = ErTextPrimary
            )
            Text(
                "Reset",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = ErAccent,
                modifier = Modifier
                    .clickable { resetActiveTab() }
                    .padding(horizontal = 8.dp, vertical = 8.dp)
                    .testTag("exprig_reset")
            )
        }

        // Segmented tabs: Face | Body | Scripts
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(ErSectionBg)
                .padding(2.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            ExpressionRigTab.values().forEach { tab ->
                val active = activeTab == tab
                Text(
                    tab.label,
                    fontSize = 11.sp,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                    color = if (active) Color.Black else ErTextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (active) ErAccent else Color.Transparent)
                        .clickable { activeTab = tab }
                        .padding(vertical = 8.dp)
                        .testTag(tab.testTag)
                )
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            when (activeTab) {
                ExpressionRigTab.FACE -> FaceTabContent(
                    state = faceState,
                    editable = layerId != null && isVideoLayer,
                    onEdit = ::applyFace,
                    layerDurationMs = layerDurationMs,
                    layerStartMs = layerStartMs,
                    currentPosMs = currentPosMs,
                    currentKeyframes = currentKeyframes,
                    onAddKeyframe = onAddKeyframe
                )
                ExpressionRigTab.BODY -> BodyTabContent(
                    state = bodyState,
                    editable = layerId != null,
                    onEdit = { next ->
                        bodyState = next
                        generateBody(next)
                    },
                    layerDurationMs = layerDurationMs,
                    layerStartMs = layerStartMs,
                    currentPosMs = currentPosMs,
                    currentKeyframes = currentKeyframes,
                    onAddKeyframe = onAddKeyframe
                )
                ExpressionRigTab.SCRIPTS -> MotionScriptPanel(
                    viewModel = viewModel,
                    clipId = layerId,
                    scrollable = false,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

// -----------------------------------------------------------------------------------------
// FACE TAB - real expression channels on the face mesh-warp pipeline
// -----------------------------------------------------------------------------------------
@Composable
private fun FaceTabContent(
    state: FaceExpressionState,
    editable: Boolean,
    onEdit: (FaceExpressionState) -> Unit,
    layerDurationMs: Long,
    layerStartMs: Long,
    currentPosMs: Long,
    currentKeyframes: List<ClipKeyframe>,
    onAddKeyframe: (ClipKeyframe) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SectionLabel("Presets", "One-tap expressions")
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(FaceExpressionPreset.values().toList()) { preset ->
                ErChip(
                    label = preset.label,
                    selected = state.preset == preset,
                    enabled = editable,
                    modifier = Modifier.testTag("exprig_face_preset_${preset.name.lowercase()}"),
                    onClick = {
                        onEdit(
                            if (state.preset == preset) {
                                // Tapping the active preset again clears it (back to neutral).
                                FaceExpressionState(
                                    intensity = state.intensity, eyes = state.eyes,
                                    brows = state.brows, mouth = state.mouth,
                                    autoBlink = state.autoBlink
                                )
                            } else {
                                state.copy(preset = preset)
                            }
                        )
                    }
                )
            }
        }

        SectionLabel("Sliders", "Range 0 - 100")
        // Sliders fine-tune ON TOP of the selected preset: resolve() adds the signed slider
        // offsets to the preset's intensity-scaled channels.
        ErSlider("Intensity", state.intensity, editable) { onEdit(state.copy(intensity = it)) }
        ErSlider("Eyes", state.eyes, editable) { onEdit(state.copy(eyes = it)) }
        ErSlider("Brows", state.brows, editable) { onEdit(state.copy(brows = it)) }
        ErSlider("Mouth", state.mouth, editable) { onEdit(state.copy(mouth = it)) }

        SectionLabel("Toggles", "Natural motion")
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ErToggleChip(
                label = "Auto blink",
                on = state.autoBlink,
                enabled = editable,
                modifier = Modifier.testTag("exprig_toggle_autoblink"),
                onClick = { onEdit(state.copy(autoBlink = !state.autoBlink)) }
            )
        }

        if (!editable) {
            ErNote("Face expressions work on a selected video / photo clip - the face warp tracks a real face.")
        }

        ErKeyframeButton(enabled = editable) {
            val rel = (currentPosMs - layerStartMs).coerceIn(0L, layerDurationMs)
            val pose = BodyMotionGenerator.poseAt(currentKeyframes, rel)
                ?: ClipKeyframe(timeMs = rel)
            onAddKeyframe(pose.copy(timeMs = rel))
        }
    }
}

// -----------------------------------------------------------------------------------------
// BODY TAB - real procedural keyframe motion on the selected clip
// -----------------------------------------------------------------------------------------
@Composable
private fun BodyTabContent(
    state: BodyMotionState,
    editable: Boolean,
    onEdit: (BodyMotionState) -> Unit,
    layerDurationMs: Long,
    layerStartMs: Long,
    currentPosMs: Long,
    currentKeyframes: List<ClipKeyframe>,
    onAddKeyframe: (ClipKeyframe) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SectionLabel("Presets", "Body motions")
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(BodyMotionPreset.values().toList()) { preset ->
                ErChip(
                    label = preset.label,
                    selected = state.preset == preset,
                    enabled = editable,
                    modifier = Modifier.testTag("exprig_body_preset_${preset.name.lowercase()}"),
                    onClick = { onEdit(if (state.preset == preset) state else state.copy(preset = preset)) }
                )
            }
        }

        SectionLabel("Sliders", "Range 0 - 100")
        ErSlider("Speed", state.speed, editable && state.preset != null) { onEdit(state.copy(speed = it)) }
        ErSlider("Smooth", state.smooth, editable && state.preset != null) { onEdit(state.copy(smooth = it)) }

        SectionLabel("Rig focus", "Which part moves")
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            RigFocus.values().forEach { focus ->
                ErChip(
                    label = focus.label,
                    selected = state.focus == focus,
                    enabled = editable && state.preset != null,
                    compact = true,
                    modifier = Modifier.testTag("exprig_focus_${focus.name.lowercase()}"),
                    onClick = { onEdit(state.copy(focus = focus)) }
                )
            }
        }

        SectionLabel("Toggles", "Motion behaviour")
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ErToggleChip("Loop", state.loop, editable && state.preset != null) { onEdit(state.copy(loop = !state.loop)) }
            ErToggleChip("Mirror", state.mirror, editable && state.preset != null) { onEdit(state.copy(mirror = !state.mirror)) }
            ErToggleChip("IK balance", state.ikBalance, editable && state.preset != null) { onEdit(state.copy(ikBalance = !state.ikBalance)) }
        }

        if (!editable) {
            ErNote("Select a clip first - body motion writes real keyframes on the selected layer.")
        }

        ErKeyframeButton(enabled = editable && currentKeyframes.isNotEmpty()) {
            val rel = (currentPosMs - layerStartMs).coerceIn(0L, layerDurationMs)
            val pose = BodyMotionGenerator.poseAt(currentKeyframes, rel) ?: ClipKeyframe(timeMs = rel)
            onAddKeyframe(pose.copy(timeMs = rel))
        }
    }
}

// -----------------------------------------------------------------------------------------
// Shared compact pieces (touch targets >= 36dp, one accent for every active item)
// -----------------------------------------------------------------------------------------
@Composable
private fun SectionLabel(title: String, hint: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(title, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = ErAccent)
        Text(hint, fontSize = 9.sp, color = ErTextTertiary)
    }
}

@Composable
private fun ErChip(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    compact: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(7.dp),
        color = when {
            selected -> ErCardSelected
            !enabled -> ErSectionBg
            else -> ErCardBg
        },
        border = BorderStroke(1.dp, if (selected) ErAccent else ErBorder),
        modifier = modifier.then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        Text(
            label,
            fontSize = 10.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = when {
                selected -> ErAccent
                !enabled -> ErTextTertiary
                else -> ErTextPrimary
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = if (compact) 9.dp else 11.dp)
        )
    }
}

@Composable
private fun ErToggleChip(
    label: String,
    on: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(7.dp),
        color = if (on) ErCardSelected else ErCardBg,
        border = BorderStroke(1.dp, if (on) ErAccent else ErBorder),
        modifier = modifier.then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            // Blue dot means on.
            Canvas(modifier = Modifier.size(7.dp)) {
                drawCircle(color = if (on) ErAccent else Color(0xFF3A4A63))
            }
            Text(
                label,
                fontSize = 10.sp,
                fontWeight = if (on) FontWeight.Bold else FontWeight.Normal,
                color = if (on) ErAccent else ErTextSecondary,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun ErSlider(label: String, value: Int, enabled: Boolean, onChange: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 36.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            fontSize = 10.sp,
            color = ErTextPrimary,
            modifier = Modifier.width(62.dp)
        )
        Slider(
            value = value.toFloat(),
            onValueChange = { onChange(it.roundToInt()) },
            valueRange = 0f..100f,
            enabled = enabled,
            colors = SliderDefaults.colors(
                thumbColor = ErAccent,
                activeTrackColor = ErAccent,
                inactiveTrackColor = ErBorder
            ),
            modifier = Modifier.weight(1f).height(26.dp).testTag("exprig_slider_${label.lowercase()}")
        )
        Text(
            "$value",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = ErTextSecondary,
            textAlign = TextAlign.End,
            modifier = Modifier.width(30.dp)
        )
    }
}

@Composable
private fun ErNote(text: String) {
    Surface(
        shape = RoundedCornerShape(7.dp),
        color = ErSectionBg,
        border = BorderStroke(1.dp, ErBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text,
            fontSize = 9.sp,
            color = ErTextTertiary,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun ErKeyframeButton(enabled: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (enabled) ErAccent else ErSectionBg,
        modifier = Modifier
            .fillMaxWidth()
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .testTag("exprig_add_keyframe")
    ) {
        Text(
            "Add Keyframe",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (enabled) Color.Black else ErTextTertiary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 11.dp)
        )
    }
}
