package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.vfx.VfxEffectsHost
import com.example.ui.StudioViewModel
import com.example.ui.components.effects.EffectLiveSwatch
import com.example.ui.components.effects.EffectPosterCache
import com.example.ui.theme.SkyBlue
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceVariant
import com.vfx.engine.core.effect.EffectCategory
import com.vfx.engine.core.params.ParamDescriptor

// ParamDescriptor does not declare a range for vector params, so vector sliders use a generic range.
private const val VEC_MIN = -1f
private const val VEC_MAX = 2f

/**
 * Per-clip effect stack panel built from the com.vfx registry: add effects by category, reorder,
 * enable/disable, set intensity, and edit every parameter generated from its descriptor.
 * The stack is stored on the clip (VideoClip.vfxStackJson) so it saves and undoes with the timeline.
 */
@Composable
fun VfxEffectsPanel(
  viewModel: StudioViewModel,
  modifier: Modifier = Modifier,
  onClose: () -> Unit = {}
) {
  val context = LocalContext.current
  val timeline by viewModel.timelineEngine.timeline.collectAsState()
  val selectedElement by viewModel.timelineEngine.selectedElement.collectAsState()
  // Selection changes must retarget the panel even when the timeline itself did not change;
  // remembering on the timeline alone kept writing to the previously selected clip.
  val clip = remember(timeline, selectedElement) { viewModel.getSelectedVideoClip() }
  val clipId = clip?.id
  val json = clip?.vfxStackJson
  val stack = remember(json) { VfxEffectsHost.decode(json) }

  var showCatalog by remember(clipId) { mutableStateOf(false) }
  var category by remember { mutableStateOf<EffectCategory?>(null) }
  var expanded by remember(clipId) { mutableIntStateOf(-1) }

  fun commit(next: String?, reveal: Boolean = false) {
    val id = clipId ?: return
    viewModel.timelineEngine.setClipVfxStack(id, next)
    // A newly added effect must be visible immediately, CapCut-style: bring the clip under the
    // playhead when it is off-screen, then repaint the held frame with the new stack. Slider
    // ticks pass reveal = false so dragging never seeks.
    if (reveal) viewModel.revealClipForLookPreview(clip)
    // Force the look apply: a stale playback-engine timeline + non-forced apply could silently
    // skip setVideoEffects (the graph signature looked unchanged) and leave the preview stuck
    // on the old picture. forceLook=true also re-runs the compositor activation gate so a newly
    // added VFX switches the preview onto the GPU compositor pipeline.
    viewModel.refreshCurrentFrame(preferClipId = id, forceLook = true)
    // Real-device fix: the FIRST VFX add on a freshly-imported clip activates the GPU
    // compositor (was off, no VFX), and its first render races the new TextureView's
    // surface-attach + the compositor's own first MediaCodec decode. Schedule the same
    // follow-up repaint ladder as applyCatalogEffect so the preview converges on the new
    // look even when the immediate requestRender landed on a surface-less window.
    viewModel.schedulePostApplyRepaint(id, label = "VfxStack")
  }

  Column(
    modifier = modifier
      .fillMaxWidth()
      .heightIn(max = 440.dp)
      .background(StudioSurface)
      .testTag("vfx_stack_panel")
  ) {
    Row(
      Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      IconButton(onClick = onClose) { Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White) }
      Text("Effect Stack", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.weight(1f))
      FilledTonalButton(
        enabled = clipId != null,
        onClick = { showCatalog = !showCatalog },
        modifier = Modifier.testTag("vfx_add_btn")
      ) {
        Icon(Icons.Default.Add, contentDescription = null)
        Spacer(Modifier.width(4.dp))
        Text(if (showCatalog) "Done" else "Add", fontSize = 12.sp)
      }
    }

    if (clipId == null) {
      Text("Select a video clip to add effects.", color = Color.White.copy(alpha = 0.7f), modifier = Modifier.padding(16.dp))
      return@Column
    }

    Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 14.dp, vertical = 4.dp)) {
      if (showCatalog) {
        val cats = remember { VfxEffectsHost.categories() }
        val active = category ?: cats.firstOrNull()
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          cats.forEach { c ->
            FilterChip(
              selected = active == c, onClick = { category = c },
              label = { Text(c.name.lowercase().replaceFirstChar { it.uppercase() }, fontSize = 11.sp) }
            )
          }
        }
        active?.let { c ->
          VfxEffectsHost.byCategory(c).forEach { def ->
            Row(
              Modifier
                .fillMaxWidth()
                .padding(bottom = 3.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(StudioSurfaceVariant.copy(alpha = 0.35f))
                .clickable {
                  // Toast BEFORE the commit so the user sees confirmation even if the
                  // compositor activation races the render (first add on a fresh clip).
                  runCatching {
                    android.widget.Toast.makeText(context, "Adding: ${def.name}", android.widget.Toast.LENGTH_LONG).show()
                  }
                  commit(VfxEffectsHost.addEffect(json, def.id), reveal = true)
                  expanded = stack.size
                  showCatalog = false
                }
                .padding(horizontal = 10.dp, vertical = 8.dp)
                .testTag("vfx_catalog_${def.id}"),
              verticalAlignment = Alignment.CenterVertically
            ) {
              // Real poster artwork as the swatch base for production assets (shader key
              // lookup); other engine entries keep the designed fallback scene. Never a
              // clip video frame.
              EffectLiveSwatch(
                shaderKey = "vfx:${def.id}",
                baseBitmap = remember(def.id) { EffectPosterCache.forShaderKey(context, "vfx:${def.id}") },
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(6.dp))
              )
              Spacer(Modifier.width(10.dp))
              Column(Modifier.weight(1f)) {
                Text(def.name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                if (def.description.isNotBlank()) {
                  Text(def.description, color = Color.White.copy(alpha = 0.55f), fontSize = 10.sp, maxLines = 2)
                }
              }
            }
          }
        }
      } else if (stack.isEmpty()) {
        Text("No effects yet. Tap Add to pick one.", color = Color.White.copy(alpha = 0.7f), modifier = Modifier.padding(vertical = 12.dp))
      } else {
        stack.effects().forEachIndexed { i, inst ->
          EffectCard(
            title = inst.name,
            enabled = inst.enabled,
            expanded = expanded == i,
            canMoveUp = i > 0,
            canMoveDown = i < stack.size - 1,
            onToggleExpand = { expanded = if (expanded == i) -1 else i },
            onEnabled = { commit(VfxEffectsHost.setEnabled(json, i, it)) },
            onUp = { commit(VfxEffectsHost.move(json, i, i - 1)); if (expanded == i) expanded = i - 1 },
            onDown = { commit(VfxEffectsHost.move(json, i, i + 1)); if (expanded == i) expanded = i + 1 },
            onDuplicate = { commit(VfxEffectsHost.duplicate(json, i)) },
            onDelete = { commit(VfxEffectsHost.removeAt(json, i)); expanded = -1 },
            onReset = { commit(VfxEffectsHost.resetEffect(json, i)) },
            tag = "vfx_fx_$i"
          ) {
            VfxSlider("Intensity", inst.intensity, 0f, 1f, 1f, inst.enabled, "vfx_${i}_intensity") {
              commit(VfxEffectsHost.setIntensity(json, i, it))
            }
            inst.definition.params.forEach { d ->
              val value: Any? = runCatching { inst.getParam<Any>(d.id) }.getOrNull()
              ParamEditor(d, value, inst.enabled, "vfx_${i}_${d.id}") { v -> commit(VfxEffectsHost.setParam(json, i, d.id, v)) }
            }
          }
        }
      }
      Spacer(Modifier.height(12.dp))
    }
  }
}

@Composable
private fun EffectCard(
  title: String, enabled: Boolean, expanded: Boolean, canMoveUp: Boolean, canMoveDown: Boolean,
  onToggleExpand: () -> Unit, onEnabled: (Boolean) -> Unit, onUp: () -> Unit, onDown: () -> Unit,
  onDuplicate: () -> Unit, onDelete: () -> Unit, onReset: () -> Unit, tag: String,
  content: @Composable ColumnScope.() -> Unit
) {
  Column(
    Modifier
      .fillMaxWidth()
      .padding(bottom = 6.dp)
      .clip(RoundedCornerShape(10.dp))
      .background(StudioSurfaceVariant.copy(alpha = 0.35f))
      .testTag(tag)
  ) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onToggleExpand).padding(horizontal = 10.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
      Text(title, color = if (enabled) Color.White else Color.White.copy(alpha = 0.5f), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
      Switch(checked = enabled, onCheckedChange = onEnabled, modifier = Modifier.testTag("${tag}_enabled"))
    }
    if (expanded) {
      Row(Modifier.padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(enabled = canMoveUp, onClick = onUp) { Icon(Icons.Default.ArrowUpward, "Move up", tint = Color.White) }
        IconButton(enabled = canMoveDown, onClick = onDown) { Icon(Icons.Default.ArrowDownward, "Move down", tint = Color.White) }
        IconButton(onClick = onDuplicate) { Icon(Icons.Default.ContentCopy, "Duplicate", tint = Color.White) }
        IconButton(onClick = onReset) { Icon(Icons.Default.Restore, "Reset", tint = SkyBlue) }
        Spacer(Modifier.weight(1f))
        IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "Remove", tint = Color(0xFFFF6B6B)) }
      }
      Column(Modifier.padding(horizontal = 10.dp, vertical = 4.dp), content = content)
    }
  }
}

/** Generates the right control for a descriptor. Types without an editor yet show a note instead of failing. */
@Composable
private fun ParamEditor(d: ParamDescriptor<*>, value: Any?, enabled: Boolean, tag: String, onChange: (Any) -> Unit) {
  when (d) {
    is ParamDescriptor.FloatP -> {
      val v = value as? Float ?: d.default
      VfxSlider(d.label, v, d.min, d.max, d.default, enabled, tag) { onChange(it) }
    }
    is ParamDescriptor.IntP -> {
      val v = value as? Int ?: d.default
      VfxSlider(d.label, v.toFloat(), d.min.toFloat(), d.max.toFloat(), d.default.toFloat(), enabled, tag, decimals = 0,
        steps = if (d.max - d.min in 2..50) d.max - d.min - 1 else 0) { onChange(it.toInt()) }
    }
    is ParamDescriptor.BoolP -> {
      val v = value as? Boolean ?: d.default
      Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(d.label, color = Color.White, fontSize = 12.sp, modifier = Modifier.weight(1f))
        Switch(checked = v, enabled = enabled, onCheckedChange = { onChange(it) }, modifier = Modifier.testTag("vfx_switch_$tag"))
      }
    }
    is ParamDescriptor.EnumP -> {
      val v = value as? String ?: d.default
      Text(d.label, color = Color.White, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
      Row(Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        d.options.forEach { o -> FilterChip(selected = v == o, enabled = enabled, onClick = { onChange(o) }, label = { Text(o, fontSize = 11.sp) }) }
      }
    }
    is ParamDescriptor.ColorP -> {
      val v = (value as? FloatArray)?.takeIf { it.size == 4 } ?: d.default
      ComponentSliders(d.label, v, listOf("R", "G", "B", "A"), 0f, 1f, d.default, enabled, tag, onChange)
    }
    is ParamDescriptor.Vec2P -> {
      val v = (value as? FloatArray)?.takeIf { it.size == 2 } ?: d.default
      ComponentSliders(d.label, v, listOf("X", "Y"), VEC_MIN, VEC_MAX, d.default, enabled, tag, onChange)
    }
    is ParamDescriptor.Vec3P -> {
      val v = (value as? FloatArray)?.takeIf { it.size == 3 } ?: d.default
      ComponentSliders(d.label, v, listOf("X", "Y", "Z"), VEC_MIN, VEC_MAX, d.default, enabled, tag, onChange)
    }
    is ParamDescriptor.Vec4P -> {
      val v = (value as? FloatArray)?.takeIf { it.size == 4 } ?: d.default
      ComponentSliders(d.label, v, listOf("X", "Y", "Z", "W"), VEC_MIN, VEC_MAX, d.default, enabled, tag, onChange)
    }
    else -> Text("${d.label}: editor not available yet", color = Color.White.copy(alpha = 0.45f), fontSize = 11.sp, modifier = Modifier.padding(vertical = 3.dp))
  }
}

@Composable
private fun ComponentSliders(
  label: String, value: FloatArray, names: List<String>, min: Float, max: Float, neutral: FloatArray,
  enabled: Boolean, tag: String, onChange: (Any) -> Unit
) {
  Text(label, color = SkyBlue, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 6.dp))
  names.forEachIndexed { i, n ->
    VfxSlider(n, value[i], min, max, neutral.getOrElse(i) { 0f }, enabled, "${tag}_$i") { nv ->
      onChange(value.copyOf().also { it[i] = nv })
    }
  }
}

@Composable
private fun VfxSlider(
  label: String, value: Float, min: Float, max: Float, neutral: Float,
  enabled: Boolean, tag: String, decimals: Int = 2, steps: Int = 0, onChange: (Float) -> Unit
) {
  Row(Modifier.fillMaxWidth().padding(bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
    Text(label, color = Color.White, fontSize = 12.sp, modifier = Modifier.width(96.dp))
    Slider(
      value = value.coerceIn(min, max),
      onValueChange = onChange,
      valueRange = min..max,
      steps = steps,
      enabled = enabled,
      modifier = Modifier.weight(1f).testTag("vfx_slider_$tag")
    )
    TextButton(
      onClick = { onChange(neutral) },
      enabled = enabled && value != neutral,
      contentPadding = PaddingValues(horizontal = 6.dp)
    ) { Text("%.${decimals}f".format(value), fontSize = 11.sp, color = if (value != neutral) SkyBlue else Color.White.copy(alpha = 0.6f)) }
  }
}
