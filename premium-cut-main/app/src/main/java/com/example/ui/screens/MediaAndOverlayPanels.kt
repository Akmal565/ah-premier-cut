package com.example.ui.screens

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.VideoClip
import com.example.engine.TimelineEngine
import com.example.ui.StudioViewModel
import com.example.ui.theme.*

enum class MediaImportTarget {
  MAIN_TRACK,
  OVERLAY_PIP
}

/**
 * Builds the [VideoClip] for media imported as an Overlay (PIP) layer. This is the ONE
 * placement contract shared by the Overlay panel and the MediaImportPanel overlay target:
 * a quarter-frame floating layer parked in the top-right quadrant; the engine resolves the
 * authoritative playhead time from [VideoClip.timelineStartMs] = -1. Pinned by unit tests.
 */
internal fun buildOverlayPipClip(
  uri: String,
  name: String,
  isVideo: Boolean,
  hasAudio: Boolean,
  durationMs: Long,
  width: Int,
  height: Int,
  naturalRotation: Int,
  frameRate: Float,
  mimeType: String
): VideoClip = VideoClip(
  uri = uri,
  name = name,
  isVideo = isVideo,
  timelineStartMs = -1L, // engine resolves the authoritative playhead time
  durationMs = durationMs,
  sourceStartMs = 0L,
  sourceEndMs = durationMs,
  sourceTotalDurationMs = if (isVideo) durationMs else 0L,
  cropScale = 0.45f,
  cropOffsetX = 0.25f,
  cropOffsetY = -0.25f,
  width = width,
  height = height,
  naturalRotation = naturalRotation,
  frameRate = frameRate,
  mimeType = mimeType,
  hasAudio = hasAudio
)

/**
 * The one real overlay importer. Every picked Uri is persisted locally, probed for real
 * metadata, built as a PIP clip and appended to the engine as ONE batch (one undo step,
 * one timeline emission for the whole selection). Heavy work runs on Dispatchers.Default;
 * per-file failures are logged and skipped without aborting the rest of the selection.
 */
internal suspend fun importOverlayMediaFromUris(
  context: Context,
  engine: TimelineEngine,
  uris: List<Uri>,
  fallbackName: String = "Imported Media",
  defaultImageDurationMs: Long = 3000L,
  forceIsVideo: Boolean? = null,
  forceHasAudio: Boolean? = null
) {
  val overlayBatch = ArrayList<VideoClip>()
  withContext(Dispatchers.Default) {
    for (uri in uris) {
      try {
        val fileName = getFileNameFromUri(context, uri) ?: fallbackName
        val persistentPath = com.example.engine.media.MediaPersistenceManager.persistMedia(
          context = context,
          sourceUriString = uri.toString(),
          suggestedName = fileName
        )
        val metadata = com.example.engine.media.MediaMetadataHelper.extractMetadataAsync(
          context,
          persistentPath,
          defaultImageDurationMs
        )
        overlayBatch.add(
          buildOverlayPipClip(
            uri = persistentPath,
            name = fileName,
            isVideo = forceIsVideo ?: metadata.isVideo,
            hasAudio = forceHasAudio ?: metadata.hasAudio,
            durationMs = metadata.durationMs,
            width = metadata.width,
            height = metadata.height,
            naturalRotation = metadata.rotationDegrees,
            frameRate = metadata.frameRate,
            mimeType = metadata.mimeType
          )
        )
      } catch (e: Throwable) {
        android.util.Log.e("OverlayImport", "Error importing overlay media", e)
      }
    }
    if (overlayBatch.isNotEmpty()) {
      engine.addOverlayClips(overlayBatch)
    }
  }
}

@Composable
fun MediaImportPanel(
  viewModel: StudioViewModel,
  onDismiss: () -> Unit,
  defaultTarget: MediaImportTarget = MediaImportTarget.MAIN_TRACK,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  var selectedTarget by remember { mutableStateOf(defaultTarget) }
  var imageDurationSec by remember { mutableFloatStateOf(3.0f) }
  var insertAtPlayhead by remember { mutableStateOf(true) }

  // ---------------------------------------------------------------------------
  // Consolidated importer for every launcher below.
  //
  // Runs on Dispatchers.Default: getFileNameFromUri (content-resolver query),
  // persistMedia (file copy) and extractMetadata (MediaMetadataRetriever) each block
  // for hundreds of ms PER FILE. Main-track picks are added clip-by-clip; overlay
  // picks share the ONE overlay importer (importOverlayMediaFromUris) with the
  // Overlay panel - appended as a single engine batch (one undo step, one emission).
  // ---------------------------------------------------------------------------
  suspend fun importPickedMedia(
    uris: List<Uri>,
    fallbackName: String,
    defaultImageDurationMs: Long = 3000L,
    forceIsVideo: Boolean? = null,
    forceHasAudio: Boolean? = null
  ) {
    if (selectedTarget == MediaImportTarget.OVERLAY_PIP) {
      importOverlayMediaFromUris(
        context = context,
        engine = viewModel.timelineEngine,
        uris = uris,
        fallbackName = fallbackName,
        defaultImageDurationMs = defaultImageDurationMs,
        forceIsVideo = forceIsVideo,
        forceHasAudio = forceHasAudio
      )
      return
    }
    withContext(Dispatchers.Default) {
      for (uri in uris) {
        try {
          val fileName = getFileNameFromUri(context, uri) ?: fallbackName
          val persistentPath = com.example.engine.media.MediaPersistenceManager.persistMedia(
            context = context,
            sourceUriString = uri.toString(),
            suggestedName = fileName
          )
          val metadata = com.example.engine.media.MediaMetadataHelper.extractMetadataAsync(
            context,
            persistentPath,
            defaultImageDurationMs
          )
          viewModel.checkAndAutoConfigureCanvasFromMedia(
            width = metadata.width,
            height = metadata.height,
            rotationDegrees = metadata.rotationDegrees,
            frameRate = metadata.frameRate
          )
          viewModel.timelineEngine.addVideoClip(
            uri = persistentPath,
            name = fileName,
            isVideo = forceIsVideo ?: metadata.isVideo,
            durationMs = metadata.durationMs,
            atPlayhead = insertAtPlayhead,
            width = metadata.width,
            height = metadata.height,
            rotationDegrees = metadata.rotationDegrees,
            frameRate = metadata.frameRate,
            mimeType = metadata.mimeType,
            hasAudio = forceHasAudio ?: metadata.hasAudio
          )
        } catch (e: Throwable) {
          android.util.Log.e("MediaImportPanel", "Error importing media", e)
        }
      }
    }
  }

  // Fallback GetMultipleContents launcher
  val fallbackMediaLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.GetMultipleContents()
  ) { uris: List<Uri> ->
    if (uris.isNotEmpty()) {
      coroutineScope.launch {
        importPickedMedia(
          uris = uris,
          fallbackName = "Imported Media",
          defaultImageDurationMs = (imageDurationSec * 1000).toLong()
        )
        onDismiss()
      }
    }
  }

  // System Media Launchers (Zero-permission Android Photo & Video Picker)
  val pickMultipleMediaLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickMultipleVisualMedia()
  ) { uris: List<Uri> ->
    if (uris.isNotEmpty()) {
      coroutineScope.launch {
        importPickedMedia(
          uris = uris,
          fallbackName = "Imported Media",
          defaultImageDurationMs = (imageDurationSec * 1000).toLong()
        )
        onDismiss()
      }
    }
  }

  var pendingCameraPath by rememberSaveable { mutableStateOf<String?>(null) }
  val captureVideoLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.CaptureVideo()
  ) { recorded: Boolean ->
    val file = pendingCameraPath?.let { java.io.File(it) }
    pendingCameraPath = null
    if (recorded && file != null && file.length() > 0L) {
      coroutineScope.launch {
        try {
          importPickedMedia(
            uris = listOf(Uri.fromFile(file)),
            fallbackName = "Recording",
            defaultImageDurationMs = (imageDurationSec * 1000).toLong(),
            forceIsVideo = true
          )
          file.delete()
          onDismiss()
        } catch (e: Throwable) {
          android.util.Log.e("MediaImportPanel", "Error importing camera recording", e)
        }
      }
    } else {
      file?.delete()
    }
  }

  fun startCameraCapture() {
    try {
      val dir = java.io.File(context.cacheDir, "camera").apply { mkdirs() }
      val file = java.io.File(dir, "rec_${System.currentTimeMillis()}.mp4")
      pendingCameraPath = file.absolutePath
      val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
      captureVideoLauncher.launch(uri)
    } catch (e: Throwable) {
      pendingCameraPath = null
      android.util.Log.e("MediaImportPanel", "Cannot start camera", e)
    }
  }

  val cameraPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { granted: Boolean -> if (granted) startCameraCapture() }

  val pickVideoLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickMultipleVisualMedia()
  ) { uris: List<Uri> ->
    if (uris.isNotEmpty()) {
      coroutineScope.launch {
        importPickedMedia(
          uris = uris,
          fallbackName = "Imported Video",
          forceIsVideo = true
        )
        onDismiss()
      }
    }
  }

  val pickImageLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickMultipleVisualMedia()
  ) { uris: List<Uri> ->
    if (uris.isNotEmpty()) {
      coroutineScope.launch {
        importPickedMedia(
          uris = uris,
          fallbackName = "Imported Photo",
          defaultImageDurationMs = (imageDurationSec * 1000).toLong(),
          forceIsVideo = false,
          forceHasAudio = false
        )
        onDismiss()
      }
    }
  }

  Column(
    modifier = modifier
      .fillMaxWidth()
      .background(StudioSurface)
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    // Top Bar: Header & Target Switcher
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          Icons.Default.VideoLibrary,
          contentDescription = null,
          tint = CyanAccent,
          modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "Add Media",
          style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            fontSize = 16.sp
          )
        )
      }

      IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
      }
    }

    // Destination Track Segmented Control (Main Track vs Overlay Track)
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(10.dp))
        .background(Color(0xFF0F172A))
        .padding(3.dp)
    ) {
      Box(
        modifier = Modifier
          .weight(1f)
          .clip(RoundedCornerShape(8.dp))
          .background(if (selectedTarget == MediaImportTarget.MAIN_TRACK) CyanAccent else Color.Transparent)
          .clickable { selectedTarget = MediaImportTarget.MAIN_TRACK }
          .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            Icons.Default.Movie,
            contentDescription = null,
            tint = if (selectedTarget == MediaImportTarget.MAIN_TRACK) Color.Black else TextSecondary,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "Main Video Track",
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            color = if (selectedTarget == MediaImportTarget.MAIN_TRACK) Color.Black else TextSecondary
          )
        }
      }

      Box(
        modifier = Modifier
          .weight(1f)
          .clip(RoundedCornerShape(8.dp))
          .background(if (selectedTarget == MediaImportTarget.OVERLAY_PIP) AmberAccent else Color.Transparent)
          .clickable { selectedTarget = MediaImportTarget.OVERLAY_PIP }
          .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            Icons.Default.Layers,
            contentDescription = null,
            tint = if (selectedTarget == MediaImportTarget.OVERLAY_PIP) Color.Black else TextSecondary,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "Overlay (PIP) Track",
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            color = if (selectedTarget == MediaImportTarget.OVERLAY_PIP) Color.Black else TextSecondary
          )
        }
      }
    }

    // Device Storage Import Actions
    Text(
      text = "Import from Device Gallery",
      style = MaterialTheme.typography.labelMedium.copy(
        fontWeight = FontWeight.Bold,
        color = TextSecondary,
        fontSize = 11.sp
      )
    )

    // Primary Full-Width Import Media from Gallery Button
    Button(
      onClick = {
        try {
          pickMultipleMediaLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
          )
        } catch (e: Throwable) {
          try {
            fallbackMediaLauncher.launch("video/*,image/*")
          } catch (e2: Throwable) {
            android.util.Log.e("MediaImportPanel", "Error launching picker", e2)
          }
        }
      },
      colors = ButtonDefaults.buttonColors(
        containerColor = CyanAccent,
        contentColor = Color.Black
      ),
      shape = RoundedCornerShape(12.dp),
      modifier = Modifier
        .fillMaxWidth()
        .height(50.dp)
        .testTag("import_media_from_gallery_primary_btn")
    ) {
      Icon(Icons.Default.Collections, contentDescription = null, modifier = Modifier.size(20.dp))
      Spacer(modifier = Modifier.width(8.dp))
      Text("Import Media from Gallery", fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      // Pick Videos Button
      Button(
        onClick = {
          try {
            pickVideoLauncher.launch(
              PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
            )
          } catch (e: Throwable) {
            try {
              fallbackMediaLauncher.launch("video/*")
            } catch (e2: Throwable) {
              android.util.Log.e("MediaImportPanel", "Error launching video picker", e2)
            }
          }
        },
        colors = ButtonDefaults.buttonColors(
          containerColor = Color(0xFF1E293B),
          contentColor = TextPrimary
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
          .weight(1f)
          .height(44.dp)
          .border(1.dp, CyanAccent.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
          .testTag("import_device_video_button")
      ) {
        Icon(Icons.Default.VideoCameraBack, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text("Videos Only", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
      }

      // Pick Images Button
      Button(
        onClick = {
          try {
            pickImageLauncher.launch(
              PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
          } catch (e: Throwable) {
            try {
              fallbackMediaLauncher.launch("image/*")
            } catch (e2: Throwable) {
              android.util.Log.e("MediaImportPanel", "Error launching image picker", e2)
            }
          }
        },
        colors = ButtonDefaults.buttonColors(
          containerColor = Color(0xFF1E293B),
          contentColor = TextPrimary
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
          .weight(1f)
          .height(44.dp)
          .border(1.dp, PurpleAccent.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
          .testTag("import_device_image_button")
      ) {
        Icon(Icons.Default.Image, contentDescription = null, tint = PurpleAccent, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text("Photos Only", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
      }
    }

    Button(
      onClick = {
        val granted = androidx.core.content.ContextCompat.checkSelfPermission(
          context, android.Manifest.permission.CAMERA
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (granted) startCameraCapture() else cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
      },
      colors = ButtonDefaults.buttonColors(
        containerColor = Color(0xFF1E293B),
        contentColor = TextPrimary
      ),
      shape = RoundedCornerShape(12.dp),
      modifier = Modifier
        .fillMaxWidth()
        .height(44.dp)
        .border(1.dp, Color(0xFFFF5252).copy(alpha = 0.6f), RoundedCornerShape(12.dp))
        .testTag("import_record_camera_button")
    ) {
      Icon(Icons.Default.Videocam, contentDescription = null, tint = Color(0xFFFF5252), modifier = Modifier.size(18.dp))
      Spacer(modifier = Modifier.width(6.dp))
      Text("Record with Camera", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
    }

    // Photo Duration & Placement Options
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
          text = "Photo Duration: ${String.format("%.1f", imageDurationSec)}s",
          style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Slider(
          value = imageDurationSec,
          onValueChange = { imageDurationSec = it },
          valueRange = 1f..10f,
          steps = 8,
          modifier = Modifier.width(130.dp),
          colors = SliderDefaults.colors(
            thumbColor = PurpleAccent,
            activeTrackColor = PurpleAccent,
            inactiveTrackColor = StudioBorder
          )
        )
      }

      if (selectedTarget == MediaImportTarget.MAIN_TRACK) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.clickable { insertAtPlayhead = !insertAtPlayhead }
        ) {
          Checkbox(
            checked = insertAtPlayhead,
            onCheckedChange = { insertAtPlayhead = it },
            colors = CheckboxDefaults.colors(checkedColor = CyanAccent)
          )
          Text(
            text = "At Playhead",
            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp)
          )
        }
      }
    }
  }
}

// -----------------------------------------------------------------------------------------
// Overlay Panel (lightweight import)
// -----------------------------------------------------------------------------------------

/**
 * The Overlay tool panel: one lightweight sheet with exactly two actions.
 * - Import Gallery: opens the phone's own photo/video picker and imports the picks as
 *   real Overlay (PIP) layers through [importOverlayMediaFromUris].
 * - Import My Library: intentionally NOT wired yet - no real online-library content
 *   exists, so the button stays disabled instead of faking a library.
 *
 * Editing of placed overlays lives where the layers live: timeline lane selection and the
 * Layers Drawer (select / duplicate / delete / reorder) plus the gesture transform frame
 * on the preview (move / scale / rotate). No duplicate inspector here.
 */
@Composable
fun OverlayPanel(
  viewModel: StudioViewModel,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  var isImporting by remember { mutableStateOf(false) }

  fun importPicked(uris: List<Uri>) {
    if (uris.isEmpty() || isImporting) return
    isImporting = true
    coroutineScope.launch {
      try {
        importOverlayMediaFromUris(context, viewModel.timelineEngine, uris)
      } catch (e: Throwable) {
        android.util.Log.e("OverlayPanel", "Overlay import failed", e)
      }
      isImporting = false
      onDismiss()
    }
  }

  // Direct phone-gallery import: Android Photo Picker (zero-permission)...
  val galleryLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickMultipleVisualMedia()
  ) { uris: List<Uri> -> importPicked(uris) }

  // ...with a content-URI fallback for devices without the system photo picker.
  val fallbackGalleryLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.GetMultipleContents()
  ) { uris: List<Uri> -> importPicked(uris) }

  Column(
    modifier = modifier
      .fillMaxWidth()
      .background(StudioSurface)
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    // Header
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          Icons.Default.Layers,
          contentDescription = null,
          tint = AmberAccent,
          modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "Overlay",
          style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            fontSize = 16.sp
          )
        )
      }

      IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
      }
    }

    // Import from the phone's gallery - opens the system picker straight from here.
    Button(
      onClick = {
        try {
          galleryLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
          )
        } catch (e: Throwable) {
          try {
            fallbackGalleryLauncher.launch("video/*,image/*")
          } catch (e2: Throwable) {
            android.util.Log.e("OverlayPanel", "Error launching gallery picker", e2)
          }
        }
      },
      enabled = !isImporting,
      colors = ButtonDefaults.buttonColors(
        containerColor = AmberAccent,
        contentColor = Color.Black
      ),
      shape = RoundedCornerShape(12.dp),
      modifier = Modifier
        .fillMaxWidth()
        .height(50.dp)
        .testTag("overlay_import_gallery_button")
    ) {
      if (isImporting) {
        CircularProgressIndicator(
          modifier = Modifier.size(18.dp),
          color = Color.Black,
          strokeWidth = 2.dp
        )
      } else {
        Icon(Icons.Default.Collections, contentDescription = null, modifier = Modifier.size(20.dp))
      }
      Spacer(modifier = Modifier.width(8.dp))
      Text("Import Gallery", fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }

    // Online library integration is intentionally not wired yet: nothing real has been
    // published to the library, so the button stays disabled - no fake library data.
    Button(
      onClick = { },
      enabled = false,
      colors = ButtonDefaults.buttonColors(
        containerColor = Color(0xFF1E293B),
        contentColor = TextPrimary,
        disabledContainerColor = Color(0xFF1E293B).copy(alpha = 0.5f),
        disabledContentColor = TextSecondary
      ),
      shape = RoundedCornerShape(12.dp),
      modifier = Modifier
        .fillMaxWidth()
        .height(50.dp)
        .testTag("overlay_import_library_button")
    ) {
      Icon(Icons.Default.VideoLibrary, contentDescription = null, modifier = Modifier.size(20.dp))
      Spacer(modifier = Modifier.width(8.dp))
      Text("Import My Library", fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
  }
}

// -----------------------------------------------------------------------------------------
// Helper Utilities for resolving Media Uris
// -----------------------------------------------------------------------------------------

fun getFileNameFromUri(context: Context, uri: Uri): String? {
  return try {
    var result: String? = null
    if (uri.scheme == "content") {
      val cursor = context.contentResolver.query(uri, null, null, null, null)
      cursor?.use {
        if (it.moveToFirst()) {
          val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
          if (index != -1) {
            result = it.getString(index)
          }
        }
      }
    }
    result ?: uri.lastPathSegment
  } catch (e: Exception) {
    uri.lastPathSegment
  }
}
