package com.example.ui.screens

import android.net.Uri
import kotlinx.coroutines.launch
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.*
import com.example.engine.SelectedTrackElement
import com.example.engine.composition.ChromaKeyResolver
import com.example.ui.StudioViewModel
import com.example.ui.theme.*

@Composable
fun EditToolPanel(
  viewModel: StudioViewModel,
  modifier: Modifier = Modifier
) {
  val selectedElement by viewModel.timelineEngine.selectedElement.collectAsState()

  Column(
    modifier = modifier
      .fillMaxWidth()
      .background(StudioSurface)
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "Edit Clip Operations",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
      )
      IconButton(onClick = { viewModel.setActiveToolbarTab(null) }) {
        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
      }
    }

    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      EditorActionTile(icon = Icons.Default.VolumeUp, label = "Volume", color = GreenAccent) {
        viewModel.setActiveToolbarTab(com.example.ui.EditorToolbarTab.VOLUME)
      }
      EditorActionTile(icon = Icons.Default.CallSplit, label = "Split", color = CyanAccent) {
        viewModel.timelineEngine.splitSelectedClipAtPlayhead()
      }
      EditorActionTile(icon = Icons.Default.ContentCut, label = "Trim Tool", color = AmberAccent) {
        viewModel.setActiveToolbarTab(com.example.ui.EditorToolbarTab.TRIM)
      }
      EditorActionTile(icon = Icons.Default.Delete, label = "Delete", color = RedAccent) {
        viewModel.timelineEngine.deleteSelected()
      }
      EditorActionTile(icon = Icons.Default.ContentCopy, label = "Duplicate", color = PurpleAccent) {
        viewModel.timelineEngine.duplicateSelected()
      }
      EditorActionTile(icon = Icons.Default.AcUnit, label = "Freeze Frame", color = CyanAccent) {
        viewModel.timelineEngine.freezeFrameAtPlayhead()
      }
      EditorActionTile(icon = Icons.Default.RotateRight, label = "Rotate 90°", color = TextPrimary) {
        viewModel.timelineEngine.rotateSelectedClip()
      }
      EditorActionTile(icon = Icons.Default.Flip, label = "Flip H", color = TextPrimary) {
        viewModel.timelineEngine.flipSelectedClip(horizontal = true)
      }
      EditorActionTile(icon = Icons.Default.SwapVert, label = "Flip V", color = TextPrimary) {
        viewModel.timelineEngine.flipSelectedClip(horizontal = false)
      }
    }

    // Embedded Volume Slider Control
    VolumeSliderSection(viewModel = viewModel)
  }
}

@Composable
fun VolumeSliderSection(
  viewModel: StudioViewModel,
  modifier: Modifier = Modifier
) {
  val timeline by viewModel.timelineEngine.timeline.collectAsState()
  val selectedElement by viewModel.timelineEngine.selectedElement.collectAsState()

  val targetClipId = when (selectedElement) {
    is SelectedTrackElement.Video -> (selectedElement as SelectedTrackElement.Video).clipId
    is SelectedTrackElement.Overlay -> (selectedElement as SelectedTrackElement.Overlay).clipId
    is SelectedTrackElement.Audio -> (selectedElement as SelectedTrackElement.Audio).clipId
    else -> timeline.videoClips.firstOrNull()?.id ?: timeline.audioClips.firstOrNull()?.id
  }

  val videoClip = timeline.videoClips.find { it.id == targetClipId }
  val overlayClip = timeline.overlayClips.find { it.id == targetClipId }
  val audioClip = timeline.audioClips.find { it.id == targetClipId }

  val clipName = videoClip?.name ?: overlayClip?.name ?: audioClip?.title ?: "Selected Track / Clip"
  val currentVol = videoClip?.volume ?: overlayClip?.volume ?: audioClip?.volume ?: 1.0f
  val isMuted = videoClip?.isMuted ?: overlayClip?.isMuted ?: audioClip?.isMuted ?: false

  var sliderVal by remember(targetClipId, currentVol, isMuted) {
    mutableFloatStateOf(if (isMuted) 0f else currentVol)
  }

  Surface(
    shape = RoundedCornerShape(12.dp),
    color = StudioSurfaceVariant,
    border = BorderStroke(1.dp, StudioBorder),
    modifier = modifier.fillMaxWidth()
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          modifier = Modifier.weight(1f)
        ) {
          Icon(
            imageVector = if (isMuted || sliderVal == 0f) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
            contentDescription = null,
            tint = if (isMuted || sliderVal == 0f) RedAccent else GreenAccent,
            modifier = Modifier.size(20.dp)
          )
          Column {
            Text(
              text = "Volume Gain Adjustment",
              style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
            )
            Text(
              text = clipName,
              style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontSize = 11.sp),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
        }

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          // Decrease Volume Button (-)
          IconButton(
            onClick = {
              val newVol = (sliderVal - 0.10f).coerceIn(0f, 3.0f)
              sliderVal = newVol
              viewModel.timelineEngine.setClipVolume(targetClipId, newVol)
            },
            modifier = Modifier
              .size(30.dp)
              .clip(CircleShape)
              .background(StudioSurface)
              .border(1.dp, StudioBorder, CircleShape)
              .testTag("volume_decrease_btn")
          ) {
            Icon(
              imageVector = Icons.Default.Remove,
              contentDescription = "Decrease Volume",
              tint = TextPrimary,
              modifier = Modifier.size(16.dp)
            )
          }

          // Clickable Percentage Display Chip
          Surface(
            onClick = {
              val nextVol = when {
                isMuted || sliderVal == 0f -> 1.0f
                sliderVal < 0.5f -> 0.5f
                sliderVal < 1.0f -> 1.0f
                sliderVal < 1.5f -> 1.5f
                sliderVal < 2.0f -> 2.0f
                sliderVal < 3.0f -> 3.0f
                else -> 1.0f
              }
              sliderVal = nextVol
              viewModel.timelineEngine.setClipVolume(targetClipId, nextVol)
            },
            shape = RoundedCornerShape(6.dp),
            color = if (isMuted || sliderVal == 0f) RedAccent.copy(alpha = 0.2f) else GreenAccent.copy(alpha = 0.2f),
            border = BorderStroke(1.dp, if (isMuted || sliderVal == 0f) RedAccent else GreenAccent),
            modifier = Modifier.testTag("volume_percentage_chip")
          ) {
            Text(
              text = if (isMuted || sliderVal == 0f) "Muted" else "${(sliderVal * 100).toInt()}%",
              style = MaterialTheme.typography.labelMedium.copy(
                color = if (isMuted || sliderVal == 0f) RedAccent else GreenAccent,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
              ),
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
            )
          }

          // Increase Volume Button (+)
          IconButton(
            onClick = {
              val newVol = if (isMuted || sliderVal == 0f) 1.0f else (sliderVal + 0.10f).coerceIn(0f, 3.0f)
              sliderVal = newVol
              viewModel.timelineEngine.setClipVolume(targetClipId, newVol)
            },
            modifier = Modifier
              .size(30.dp)
              .clip(CircleShape)
              .background(GreenAccent.copy(alpha = 0.25f))
              .border(1.dp, GreenAccent, CircleShape)
              .testTag("volume_increase_btn")
          ) {
            Icon(
              imageVector = Icons.Default.Add,
              contentDescription = "Increase Volume",
              tint = GreenAccent,
              modifier = Modifier.size(18.dp)
            )
          }

          // Mute / Speaker Toggle Button
          IconButton(
            onClick = {
              if (isMuted || sliderVal == 0f) {
                val newVol = if (currentVol > 0f) currentVol else 1.0f
                sliderVal = newVol
                viewModel.timelineEngine.setClipVolume(targetClipId, newVol)
              } else {
                viewModel.timelineEngine.toggleClipMute(targetClipId)
              }
            },
            modifier = Modifier
              .size(30.dp)
              .clip(CircleShape)
              .background(StudioSurface)
              .border(1.dp, StudioBorder, CircleShape)
              .testTag("volume_mute_btn")
          ) {
            Icon(
              imageVector = if (isMuted || sliderVal == 0f) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
              contentDescription = "Mute Toggle",
              tint = if (isMuted || sliderVal == 0f) RedAccent else GreenAccent,
              modifier = Modifier.size(16.dp)
            )
          }
        }
      }

      Slider(
        value = sliderVal,
        onValueChange = { newValue ->
          sliderVal = newValue
          viewModel.timelineEngine.setClipVolume(targetClipId, newValue)
        },
        valueRange = 0f..3.0f,
        colors = SliderDefaults.colors(
          thumbColor = GreenAccent,
          activeTrackColor = GreenAccent,
          inactiveTrackColor = StudioBorder
        ),
        modifier = Modifier.testTag("volume_gain_slider")
      )

      val presets = listOf(
        0.0f to "Mute",
        0.5f to "50%",
        1.0f to "100%",
        1.5f to "150%",
        2.0f to "200% Boost",
        3.0f to "300% Max"
      )

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        presets.forEach { (volVal, label) ->
          val isSelected = (sliderVal - volVal).let { kotlin.math.abs(it) < 0.05f }
          Surface(
            onClick = {
              sliderVal = volVal
              viewModel.timelineEngine.setClipVolume(targetClipId, volVal)
            },
            shape = RoundedCornerShape(8.dp),
            color = if (isSelected) GreenAccent.copy(alpha = 0.25f) else StudioSurface,
            border = BorderStroke(1.dp, if (isSelected) GreenAccent else StudioBorder),
            modifier = Modifier.testTag("vol_preset_${label.replace("%", "").replace(" ", "_").lowercase()}")
          ) {
            Text(
              text = label,
              style = MaterialTheme.typography.labelSmall.copy(
                color = if (isSelected) GreenAccent else TextPrimary,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                fontSize = 10.sp
              ),
              modifier = Modifier.padding(horizontal = 5.dp, vertical = 4.dp)
            )
          }
        }
      }
    }
  }
}

@Composable
fun VolumeToolPanel(
  viewModel: StudioViewModel,
  modifier: Modifier = Modifier
) {
  val timeline by viewModel.timelineEngine.timeline.collectAsState()
  val selectedElement by viewModel.timelineEngine.selectedElement.collectAsState()

  val targetClipId = when (selectedElement) {
    is SelectedTrackElement.Video -> (selectedElement as SelectedTrackElement.Video).clipId
    is SelectedTrackElement.Overlay -> (selectedElement as SelectedTrackElement.Overlay).clipId
    is SelectedTrackElement.Audio -> (selectedElement as SelectedTrackElement.Audio).clipId
    else -> timeline.videoClips.firstOrNull()?.id ?: timeline.audioClips.firstOrNull()?.id
  }

  val videoClip = timeline.videoClips.find { it.id == targetClipId }
  val audioClip = timeline.audioClips.find { it.id == targetClipId }
  val currentAudioFx = videoClip?.audioEffects ?: audioClip?.audioEffects ?: AudioEffectsSettings()

  var noiseReduceEnabled by remember(targetClipId, currentAudioFx.noiseReductionDb) {
    mutableStateOf(currentAudioFx.noiseReductionDb > 0.0f)
  }
  var noiseDbLevel by remember(targetClipId, currentAudioFx.noiseReductionDb) {
    mutableFloatStateOf(if (currentAudioFx.noiseReductionDb > 0.0f) currentAudioFx.noiseReductionDb else 12.0f)
  }

  Column(
    modifier = modifier
      .fillMaxWidth()
      .background(StudioSurface)
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(Icons.Default.VolumeUp, contentDescription = null, tint = GreenAccent)
        Text(
          text = "Track / Clip Volume & Noise Controls",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
        )
      }
      IconButton(onClick = { viewModel.setActiveToolbarTab(null) }) {
        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
      }
    }

    VolumeSliderSection(viewModel = viewModel)

    // Noise Reduction Controls Section
    Surface(
      shape = RoundedCornerShape(12.dp),
      color = StudioSurfaceVariant,
      border = BorderStroke(1.dp, StudioBorder),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Icon(
              imageVector = Icons.Default.GraphicEq,
              contentDescription = null,
              tint = CyanAccent,
              modifier = Modifier.size(20.dp)
            )
            Column {
              Text(
                text = "Reduce Noise",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
              )
              Text(
                text = "Spectral noise gate & hum reduction",
                style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontSize = 11.sp)
              )
            }
          }

          Switch(
            checked = noiseReduceEnabled,
            onCheckedChange = { enabled ->
              noiseReduceEnabled = enabled
              val updatedDb = if (enabled) noiseDbLevel else 0.0f
              val newFx = currentAudioFx.copy(noiseReductionDb = updatedDb)
              viewModel.timelineEngine.setClipAudioEffects(targetClipId, newFx)
            },
            colors = SwitchDefaults.colors(
              checkedThumbColor = Color.White,
              checkedTrackColor = CyanAccent,
              uncheckedThumbColor = TextSecondary,
              uncheckedTrackColor = StudioBorder
            ),
            modifier = Modifier.testTag("reduce_noise_switch")
          )
        }

        if (noiseReduceEnabled) {
          Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "Background Noise Reduction",
                style = MaterialTheme.typography.labelMedium.copy(color = TextSecondary)
              )
              Text(
                text = "${noiseDbLevel.toInt()} dB",
                style = MaterialTheme.typography.labelMedium.copy(color = CyanAccent, fontWeight = FontWeight.Bold)
              )
            }

            Slider(
              value = noiseDbLevel,
              onValueChange = { db ->
                noiseDbLevel = db
                val newFx = currentAudioFx.copy(noiseReductionDb = db)
                viewModel.timelineEngine.setClipAudioEffects(targetClipId, newFx)
              },
              valueRange = 3.0f..24.0f,
              steps = 20,
              colors = SliderDefaults.colors(
                thumbColor = CyanAccent,
                activeTrackColor = CyanAccent,
                inactiveTrackColor = StudioBorder
              ),
              modifier = Modifier.testTag("noise_reduction_db_slider")
            )
          }
        }
      }
    }
  }
}

@Composable
fun EffectsToolPanel(
  viewModel: StudioViewModel,
  modifier: Modifier = Modifier,
  category: com.example.engine.effects.registry.EffectCategory =
    com.example.engine.effects.registry.EffectsAssetRegistry.populatedCategories()
      .firstOrNull()
      ?: com.example.engine.effects.registry.EffectCategory.VIDEO_EFFECTS
) {
  com.example.ui.components.effects.EffectsCategoryPanel(
    category = category,
    viewModel = viewModel,
    onClose = { viewModel.setActiveToolbarTab(null) },
    onApply = { effect -> viewModel.applyCatalogEffect(effect, category) },
    onIntensity = { effect, amount ->
      // Strength ticks must not move the effect's 3s play window - only a fresh tap re-anchors it.
      viewModel.applyCatalogEffect(effect, category, amount, reanchorWindow = false)
    },
    modifier = modifier
  )
}

@Composable
fun ChromaKeyPanel(
  viewModel: StudioViewModel,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val timeline by viewModel.timelineEngine.timeline.collectAsState()
  val selectedIds by viewModel.timelineEngine.selectedClipIds.collectAsState()
  val selectedElement by viewModel.timelineEngine.selectedElement.collectAsState()

  // The clip this panel edits. Null = nothing selected, and then the edits are the project default that
  // every clip without its own key inherits (the old, project-wide behaviour in a clearly labelled place).
  val targetClip = remember(timeline, selectedIds, selectedElement) { viewModel.getSelectedChromaClip() }
  val effective = remember(timeline, targetClip?.id) {
    targetClip?.chromaKey ?: timeline.chromaKey
  }
  var chroma by remember(targetClip?.id, effective) { mutableStateOf(effective) }
  var customKeyOpen by remember(targetClip?.id) { mutableStateOf(false) }

  val push: (ChromaKeySettings) -> Unit = { next ->
    chroma = next
    viewModel.setChromaKeyFor(targetClip?.id, next)
  }

  val bgPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia()
  ) { uri ->
    if (uri != null) {
      coroutineScope.launch {
        val persistentPath = com.example.engine.media.MediaPersistenceManager.persistMedia(
          context = context,
          sourceUriString = uri.toString(),
          suggestedName = "Chroma Background"
        )
        // A picked video must be stored as a video: the compositor decodes frames for a video
        // background and uploads a still texture for an image one.
        val mime = runCatching { context.contentResolver.getType(uri) }.getOrNull()
        val isVideo = mime?.startsWith("video") == true
        push(
          chroma.copy(
            backgroundUri = persistentPath,
            backgroundType = if (isVideo) ChromaKeyResolver.BACKGROUND_VIDEO else ChromaKeyResolver.BACKGROUND_IMAGE
          )
        )
      }
    }
  }

  Column(
    modifier = modifier
      .fillMaxWidth()
      .background(StudioSurface)
      .padding(horizontal = 14.dp, vertical = 8.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    // 1. Single Compact Header: [ 🎨 Chroma Key    X ]
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .height(40.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Box(
          modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(Color(0xFF1E283E)),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.Palette,
            contentDescription = null,
            tint = Color(0xFF00C2FF),
            modifier = Modifier.size(16.dp)
          )
        }
        Text(
          text = "Chroma Key",
          style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.Bold,
            color = Color.White,
            fontSize = 14.sp
          )
        )
      }

      IconButton(
        onClick = { viewModel.setActiveToolbarTab(null) },
        modifier = Modifier
          .size(32.dp)
          .clip(CircleShape)
          .background(Color(0xFF1E283E))
      ) {
        Icon(
          imageVector = Icons.Default.Close,
          contentDescription = "Close",
          tint = Color.White,
          modifier = Modifier.size(18.dp)
        )
      }
    }

    // 2. Which clip is being keyed (or the project default when nothing is selected).
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      val isPerClip = targetClip?.chromaKey != null
      Text(
        text = when {
          targetClip == null -> "No clip on the playhead"
          isPerClip -> "Editing clip: ${targetClip.name}"
          else -> "Key for: ${targetClip.name} (first change stores it on this clip)"
        },
        style = MaterialTheme.typography.bodySmall.copy(color = if (targetClip != null) CyanAccent else AmberAccent),
        fontSize = 11.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.weight(1f)
      )
      TextButton(
        onClick = { viewModel.resetChromaKey(targetClip?.id) },
        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
        modifier = Modifier.height(28.dp)
      ) {
        Text("Reset", color = TextSecondary, fontSize = 11.sp)
      }
    }
    if (targetClip == null) {
      Text(
        "Move the playhead over a clip (or select one on the timeline) to key it. There is no clip here, so "
          + "these settings are the project default that every un-keyed clip inherits.",
        fontSize = 10.sp,
        color = TextSecondary
      )
    }

    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(1.dp)
        .background(Color(0xFF1E283E))
    )

    // Enable Chroma Key Toggle Row
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text("Enable Chroma Key", style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary, fontSize = 13.sp))
      Switch(
        checked = chroma.enabled,
        onCheckedChange = { push(chroma.copy(enabled = it)) },
        colors = SwitchDefaults.colors(checkedThumbColor = GreenAccent)
      )
    }

    if (chroma.enabled) {
      LazyColumn(
        modifier = Modifier.fillMaxWidth().height(260.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        // 1. Color Selection: the shipped presets plus a full picker for a studio colour that is not listed.
        item {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text("Key Color", style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontWeight = FontWeight.Bold))
            Text(
              text = "#${"%06X".format(chroma.targetColor.toInt() and 0xFFFFFF)}",
              fontSize = 11.sp,
              color = TextSecondary
            )
          }
          Spacer(modifier = Modifier.height(4.dp))
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
            ChromaKeyResolver.KEY_PRESETS.forEach { (label, colorVal) ->
              val isSelected = chroma.targetColor == colorVal
              Box(
                modifier = Modifier
                  .size(34.dp)
                  .clip(CircleShape)
                  .background(Color(colorVal))
                  .border(
                    width = if (isSelected) 3.dp else 1.dp,
                    color = if (isSelected) CyanAccent else StudioBorder,
                    shape = CircleShape
                  )
                  .clickable { push(chroma.copy(targetColor = colorVal)) },
                contentAlignment = Alignment.Center
              ) {
                if (isSelected) {
                  Icon(
                    Icons.Default.Check,
                    contentDescription = label,
                    tint = if (colorVal == 0xFFFFFFFFL) Color.Black else Color.White,
                    modifier = Modifier.size(16.dp)
                  )
                }
              }
            }
          }
          Spacer(modifier = Modifier.height(6.dp))
          TextButton(
            onClick = { customKeyOpen = !customKeyOpen },
            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
            modifier = Modifier.height(28.dp)
          ) {
            Text(if (customKeyOpen) "Hide colour picker" else "Custom key colour…", color = CyanAccent, fontSize = 11.sp)
          }
        }

        // 1b. Custom key colour: R/G/B picker (works for any screen colour, including the ones not in the presets).
        if (customKeyOpen) {
          item {
            val keyInt = chroma.targetColor.toInt()
            val r = ((keyInt ushr 16) and 0xFF).toFloat() / 255f
            val g = ((keyInt ushr 8) and 0xFF).toFloat() / 255f
            val b = (keyInt and 0xFF).toFloat() / 255f
            fun withChannel(nr: Float? = null, ng: Float? = null, nb: Float? = null): Long {
              val rr = ((nr ?: r) * 255f + 0.5f).toInt().coerceIn(0, 255)
              val gg = ((ng ?: g) * 255f + 0.5f).toInt().coerceIn(0, 255)
              val bb = ((nb ?: b) * 255f + 0.5f).toInt().coerceIn(0, 255)
              return 0xFF000000L or (rr.toLong() shl 16) or (gg.toLong() shl 8) or bb.toLong()
            }
            Column(modifier = Modifier.fillMaxWidth()) {
              Text("Red", fontSize = 10.sp, color = TextSecondary)
              Slider(
                value = r,
                onValueChange = { push(chroma.copy(targetColor = withChannel(nr = it))) },
                colors = SliderDefaults.colors(thumbColor = Color(0xFFEF4444), activeTrackColor = Color(0xFFEF4444))
              )
              Text("Green", fontSize = 10.sp, color = TextSecondary)
              Slider(
                value = g,
                onValueChange = { push(chroma.copy(targetColor = withChannel(ng = it))) },
                colors = SliderDefaults.colors(thumbColor = GreenAccent, activeTrackColor = GreenAccent)
              )
              Text("Blue", fontSize = 10.sp, color = TextSecondary)
              Slider(
                value = b,
                onValueChange = { push(chroma.copy(targetColor = withChannel(nb = it))) },
                colors = SliderDefaults.colors(thumbColor = Color(0xFF60A5FA), activeTrackColor = Color(0xFF60A5FA))
              )
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .height(22.dp)
                  .clip(RoundedCornerShape(6.dp))
                  .background(Color(chroma.targetColor))
              )
            }
          }
        }

        // 2. Similarity Slider
        item {
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Similarity", style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary))
            Text("${(chroma.similarity * 100).toInt()}%", style = MaterialTheme.typography.bodySmall.copy(color = TextPrimary))
          }
          Slider(
            value = chroma.similarity,
            valueRange = 0.05f..0.95f,
            onValueChange = { push(chroma.copy(similarity = it, intensity = it)) },
            colors = SliderDefaults.colors(thumbColor = GreenAccent, activeTrackColor = GreenAccent)
          )
        }

        // 3. Edge Softness Slider (feather of the matte)
        item {
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Edge Softness", style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary))
            Text("${(chroma.smoothness * 100).toInt()}%", style = MaterialTheme.typography.bodySmall.copy(color = TextPrimary))
          }
          Slider(
            value = chroma.smoothness,
            valueRange = 0.01f..0.50f,
            onValueChange = { push(chroma.copy(smoothness = it, edgeAdjustment = it)) },
            colors = SliderDefaults.colors(thumbColor = GreenAccent, activeTrackColor = GreenAccent)
          )
        }

        // 4. Spill Suppression Slider
        item {
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Spill Suppression", style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary))
            Text("${(chroma.spillSuppression * 100).toInt()}%", style = MaterialTheme.typography.bodySmall.copy(color = TextPrimary))
          }
          Slider(
            value = chroma.spillSuppression,
            valueRange = 0.0f..1.0f,
            onValueChange = { push(chroma.copy(spillSuppression = it, spillReduction = it)) },
            colors = SliderDefaults.colors(thumbColor = GreenAccent, activeTrackColor = GreenAccent)
          )
        }

        // 4b. Edge Cleanup: extra de-spill on the soft edge, where a green screen leaves its fringe.
        item {
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Edge Cleanup (halo)", style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary))
            Text("${(chroma.edgeControl * 100).toInt()}%", style = MaterialTheme.typography.bodySmall.copy(color = TextPrimary))
          }
          Slider(
            value = chroma.edgeControl,
            valueRange = 0.0f..1.0f,
            onValueChange = { push(chroma.copy(edgeControl = it)) },
            colors = SliderDefaults.colors(thumbColor = GreenAccent, activeTrackColor = GreenAccent)
          )
        }

        // 4c. Foreground opacity.
        item {
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Foreground Opacity", style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary))
            Text("${(chroma.foregroundOpacity * 100).toInt()}%", style = MaterialTheme.typography.bodySmall.copy(color = TextPrimary))
          }
          Slider(
            value = chroma.foregroundOpacity,
            valueRange = 0.0f..1f,
            onValueChange = { push(chroma.copy(foregroundOpacity = it)) },
            colors = SliderDefaults.colors(thumbColor = GreenAccent, activeTrackColor = GreenAccent)
          )
        }

        // 5. Background Type Selection
        item {
          Text("Background Replacement", style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontWeight = FontWeight.Bold))
          Spacer(modifier = Modifier.height(4.dp))
          val bgTypes = listOf(
            ChromaKeyResolver.BACKGROUND_TRANSPARENT,
            ChromaKeyResolver.BACKGROUND_SOLID,
            ChromaKeyResolver.BACKGROUND_IMAGE,
            ChromaKeyResolver.BACKGROUND_VIDEO
          )
          Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState())
          ) {
            bgTypes.forEach { type ->
              FilterChip(
                selected = chroma.backgroundType == type,
                onClick = { push(chroma.copy(backgroundType = type)) },
                label = {
                  Text(when (type) {
                    ChromaKeyResolver.BACKGROUND_TRANSPARENT -> "Transparent"
                    ChromaKeyResolver.BACKGROUND_SOLID -> "Solid Color"
                    ChromaKeyResolver.BACKGROUND_IMAGE -> "Image"
                    else -> "Video"
                  })
                },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = CyanAccent,
                  selectedLabelColor = Color.Black,
                  containerColor = StudioSurfaceVariant,
                  labelColor = TextPrimary
                )
              )
            }
          }
        }

        // Background Image/Video Pick
        if (chroma.backgroundType == ChromaKeyResolver.BACKGROUND_IMAGE ||
          chroma.backgroundType == ChromaKeyResolver.BACKGROUND_VIDEO
        ) {
          item {
            val bgUri = chroma.backgroundUri
            val hasBg = !bgUri.isNullOrBlank()
            Button(
              onClick = {
                bgPickerLauncher.launch(
                  androidx.activity.result.PickVisualMediaRequest(
                    if (chroma.backgroundType == ChromaKeyResolver.BACKGROUND_VIDEO) {
                      ActivityResultContracts.PickVisualMedia.VideoOnly
                    } else {
                      ActivityResultContracts.PickVisualMedia.ImageOnly
                    }
                  )
                )
              },
              colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Color.Black),
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.fillMaxWidth().height(38.dp)
            ) {
              Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = if (hasBg) {
                  "Change Background ${if (chroma.backgroundType == ChromaKeyResolver.BACKGROUND_VIDEO) "Video" else "Image"}"
                } else {
                  "Pick Background ${if (chroma.backgroundType == ChromaKeyResolver.BACKGROUND_VIDEO) "Video" else "Image"}"
                },
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
              )
            }
            if (hasBg) {
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "✓ Background set: ${bgUri?.substringAfterLast("/")}",
                color = GreenAccent,
                fontSize = 11.sp
              )
            }
          }
        }

        if (chroma.backgroundType == ChromaKeyResolver.BACKGROUND_SOLID) {
          item {
            Text("Solid Background Color", style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary))
            Spacer(modifier = Modifier.height(4.dp))
            val solidPalette = listOf(
              0xFF000000L to "Black",
              0xFFFFFFFFL to "White",
              0xFF1E3A8AL to "Navy",
              0xFF7C3AEDL to "Purple",
              0xFFEF4444L to "Coral"
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              solidPalette.forEach { (colorVal, name) ->
                val isSelected = chroma.backgroundColor == colorVal
                Box(
                  modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(colorVal))
                    .border(
                      width = if (isSelected) 3.dp else 1.dp,
                      color = if (isSelected) AmberAccent else StudioBorder,
                      shape = CircleShape
                    )
                    .clickable { push(chroma.copy(backgroundColor = colorVal)) }
                )
              }
            }
          }
        }

        // 6. Apply the current key to every clip (and make it the project default for clips added later).
        if (targetClip != null) {
          item {
            OutlinedButton(
              onClick = { viewModel.applyChromaKeyToAllClips(chroma) },
              modifier = Modifier.fillMaxWidth().height(38.dp),
              border = BorderStroke(1.dp, CyanAccent),
              shape = RoundedCornerShape(8.dp)
            ) {
              Text("Apply to all clips", color = CyanAccent, fontSize = 12.sp)
            }
          }
        }
      }
    }
  }
}

@Composable
fun CanvasPanel(
  viewModel: StudioViewModel,
  modifier: Modifier = Modifier
) {
  val currentAspect by viewModel.activeAspectRatio.collectAsState()
  val currentRes by viewModel.activeResolution.collectAsState()
  val currentFps by viewModel.activeFps.collectAsState()

  Column(
    modifier = modifier
      .fillMaxWidth()
      .background(StudioSurface)
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "Canvas & Aspect Ratio",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
      )
      IconButton(onClick = { viewModel.setActiveToolbarTab(null) }) {
        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
      }
    }

    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      items(AspectRatio.values()) { ratio ->
        FilterChip(
          selected = currentAspect == ratio,
          onClick = {
            viewModel.updateProjectSettings(ratio, currentRes, currentFps)
          },
          label = { Text(ratio.label) },
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = CyanAccent,
            selectedLabelColor = Color.Black
          )
        )
      }
    }
  }
}

@Composable
private fun EditorActionTile(
  icon: ImageVector,
  label: String,
  color: Color,
  onClick: () -> Unit
) {
  Card(
    modifier = Modifier
      .size(width = 80.dp, height = 72.dp)
      .clip(RoundedCornerShape(12.dp))
      .clickable(onClick = onClick),
    colors = CardDefaults.cardColors(containerColor = StudioSurfaceVariant)
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(8.dp),
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(24.dp))
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, color = TextPrimary, fontWeight = FontWeight.Medium),
        maxLines = 1
      )
    }
  }
}

@Composable
fun BackgroundToolPanel(
  viewModel: StudioViewModel,
  modifier: Modifier = Modifier
) {
  val currentAspect by viewModel.activeAspectRatio.collectAsState()
  val currentRes by viewModel.activeResolution.collectAsState()
  val currentFps by viewModel.activeFps.collectAsState()
  val currentSampleRate by viewModel.activeSampleRate.collectAsState()
  val activeCanvasColor by viewModel.activeCanvasColor.collectAsState()

  val colors = listOf(
    0xFF000000 to "Black",
    0xFF0F172A to "Slate",
    0xFF1E293B to "Charcoal",
    0xFFFFFFFF to "White",
    0xFF0A192F to "Navy",
    0xFF2E1065 to "Purple",
    0xFF064E3B to "Emerald",
    0xFF450A0A to "Crimson",
    0xFF083344 to "Cyan",
    0xFF18181B to "Zinc"
  )

  Column(
    modifier = modifier
      .fillMaxWidth()
      .background(StudioSurface)
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(Icons.Default.Texture, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(20.dp))
        Text(
          text = "Canvas Background",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
        )
      }
      IconButton(onClick = { viewModel.setActiveToolbarTab(null) }) {
        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
      }
    }

    Text(
      text = "Select solid or styled background color for canvas",
      style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
    )

    LazyRow(
      horizontalArrangement = Arrangement.spacedBy(10.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      items(colors) { (colorLong, name) ->
        val isSelected = activeCanvasColor == colorLong
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          modifier = Modifier.clickable {
            viewModel.updateProjectSettings(
              aspectRatio = currentAspect,
              resolution = currentRes,
              fps = currentFps,
              sampleRate = currentSampleRate,
              canvasColor = colorLong
            )
          }
        ) {
          Box(
            modifier = Modifier
              .size(44.dp)
              .clip(CircleShape)
              .background(Color(colorLong))
              .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) CyanAccent else Color.White.copy(alpha = 0.3f),
                shape = CircleShape
              ),
            contentAlignment = Alignment.Center
          ) {
            if (isSelected) {
              Icon(
                Icons.Default.Check,
                contentDescription = "Selected",
                tint = if (colorLong == 0xFFFFFFFF) Color.Black else Color.White,
                modifier = Modifier.size(20.dp)
              )
            }
          }
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = name,
            style = MaterialTheme.typography.labelSmall.copy(
              color = if (isSelected) CyanAccent else TextSecondary,
              fontSize = 10.sp,
              fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
          )
        }
      }
    }
  }
}
