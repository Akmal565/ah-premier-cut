package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.CrashRecoveryEntity
import com.example.data.local.ProjectEntity
import com.example.data.repository.NotificationRepository
import com.example.domain.model.AspectRatio
import com.example.domain.model.FrameRate
import com.example.domain.model.Resolution
import com.example.ui.AppScreen
import com.example.ui.StudioViewModel
import com.example.ui.components.home.HomeNavTab
import com.example.ui.components.home.PremierBottomNavigation
import com.example.ui.components.home.PremierHeader
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
  viewModel: StudioViewModel,
  initialTab: HomeNavTab = HomeNavTab.HOME,
  modifier: Modifier = Modifier
) {
  var activeNavTab by remember { mutableStateOf(initialTab) }
  val settings by viewModel.settings.collectAsState()
  val projects by viewModel.allProjects.collectAsState()
  val activeRecovery by viewModel.activeRecoverySession.collectAsState()
  val notifications by NotificationRepository.notifications.collectAsState()
  val hasUnread = remember(notifications) { notifications.any { !it.isRead } }

  // Media Picker for New Project
  val instantVideoPicker = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 10)
  ) { uris: List<Uri> ->
    if (uris.isNotEmpty()) {
      viewModel.createProjectWithMedia("Video Project", uris.map { it.toString() }, isVideo = true)
    }
  }

  val onStartNewProject: () -> Unit = {
    if (settings.openEditorDirectlyOnNewProject) {
      viewModel.createNewProject(
        name = "New Project",
        aspectRatio = AspectRatio.RATIO_9_16,
        resolution = Resolution.RES_1080P,
        fps = FrameRate.FPS_30,
        initialMediaClips = emptyList()
      )
    } else {
      instantVideoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
    }
  }

  Scaffold(
    modifier = modifier.fillMaxSize(),
    containerColor = LightPageBg,
    topBar = {
      PremierHeader(
        onNotificationClick = { viewModel.navigateTo(AppScreen.NOTIFICATIONS) },
        onSettingsClick = { viewModel.navigateTo(AppScreen.SETTINGS) },
        hasUnreadNotifications = hasUnread
      )
    },
    bottomBar = {
      PremierBottomNavigation(
        activeTab = activeNavTab,
        onNavigateHome = { activeNavTab = HomeNavTab.HOME },
        onNavigateProjects = { activeNavTab = HomeNavTab.PROJECTS },
        onNewProject = onStartNewProject,
        onNavigateAccount = { viewModel.navigateTo(AppScreen.ACCOUNT) }
      )
    }
  ) { padding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
    ) {
      when (activeNavTab) {
        HomeNavTab.HOME -> {
          HomeMainContent(
            onNewProjectClick = onStartNewProject
          )
        }
        HomeNavTab.PROJECTS -> {
          ProjectsTabContent(
            projects = projects,
            activeRecovery = activeRecovery,
            onOpenProject = { project -> viewModel.loadProject(project) },
            onDeleteProject = { project -> viewModel.deleteProject(project.id) },
            onDuplicateProject = { project -> viewModel.duplicateProject(project.id) },
            onRenameProject = { project, newName -> viewModel.renameProject(project.id, newName) },
            onRecoverSession = { viewModel.restoreCrashRecoverySession() },
            onDismissRecovery = { viewModel.discardCrashRecoverySession() }
          )
        }
        HomeNavTab.ACCOUNT -> {
          // Navigated via AppScreen.ACCOUNT
        }
      }
    }
  }
}

@Composable
private fun HomeMainContent(
  onNewProjectClick: () -> Unit
) {
  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
    contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp)
  ) {
    // 1. NEW PROJECT CARD
    item {
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = ripple(bounded = true),
            onClick = onNewProjectClick
          )
          .testTag("new_project_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = LightCardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(LightCardBorder, LightCardBorder)))
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 18.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = stringResource(R.string.new_project),
              style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = LightTextPrimary
              )
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
              text = stringResource(R.string.import_video_prompt),
              style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 12.sp,
                color = LightTextSecondary
              )
            )
          }

          Box(
            modifier = Modifier
              .size(40.dp)
              .clip(CircleShape)
              .background(PremierAccent),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.Add,
              contentDescription = stringResource(R.string.new_project),
              tint = Color.White,
              modifier = Modifier.size(22.dp)
            )
          }
        }
      }
    }

  }
}
@Composable
private fun ProjectsTabContent(
  projects: List<ProjectEntity>,
  activeRecovery: CrashRecoveryEntity?,
  onOpenProject: (ProjectEntity) -> Unit,
  onDeleteProject: (ProjectEntity) -> Unit,
  onDuplicateProject: (ProjectEntity) -> Unit,
  onRenameProject: (ProjectEntity, String) -> Unit,
  onRecoverSession: () -> Unit,
  onDismissRecovery: () -> Unit
) {
  var searchQuery by remember { mutableStateOf("") }
  var renameTarget by remember { mutableStateOf<ProjectEntity?>(null) }
  var renameText by remember { mutableStateOf("") }

  val filteredProjects = remember(projects, searchQuery) {
    if (searchQuery.isBlank()) projects else projects.filter {
      it.name.contains(searchQuery, ignoreCase = true)
    }
  }

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
    contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp)
  ) {
    // Crash Recovery Banner
    activeRecovery?.let { recovery ->
      item {
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = PremierAccentLight),
          border = BorderStroke(1.dp, PremierAccent)
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Unsaved Session Available",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = LightTextPrimary, fontSize = 13.sp)
              )
              Text(
                text = "Restore edits from \"${recovery.projectName}\"",
                style = MaterialTheme.typography.bodySmall.copy(color = LightTextSecondary, fontSize = 11.sp)
              )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              TextButton(onClick = onDismissRecovery) {
                Text("Dismiss", fontSize = 11.sp, color = LightTextSecondary)
              }
              Button(
                onClick = onRecoverSession,
                colors = ButtonDefaults.buttonColors(containerColor = PremierAccent),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                shape = RoundedCornerShape(8.dp)
              ) {
                Text("Restore", fontSize = 11.sp, color = Color.White)
              }
            }
          }
        }
      }
    }

    // Search Bar
    item {
      OutlinedTextField(
        value = searchQuery,
        onValueChange = { searchQuery = it },
        placeholder = { Text(stringResource(R.string.search_projects), color = LightTextTertiary, fontSize = 13.sp) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = LightTextSecondary, modifier = Modifier.size(18.dp)) },
        trailingIcon = {
          if (searchQuery.isNotEmpty()) {
            IconButton(onClick = { searchQuery = "" }) {
              Icon(Icons.Default.Clear, contentDescription = "Clear", tint = LightTextSecondary, modifier = Modifier.size(16.dp))
            }
          }
        },
        singleLine = true,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("projects_search_field"),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
          focusedContainerColor = LightCardBg,
          unfocusedContainerColor = LightCardBg,
          focusedBorderColor = PremierAccent,
          unfocusedBorderColor = LightCardBorder,
          focusedTextColor = LightTextPrimary,
          unfocusedTextColor = LightTextPrimary
        )
      )
    }

    // Projects Header
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Your Projects (${filteredProjects.size})",
          style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = LightTextPrimary, fontSize = 14.sp)
        )
      }
    }

    // Projects List or Empty State
    if (filteredProjects.isEmpty()) {
      item {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
          contentAlignment = Alignment.Center
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Outlined.FolderOpen, contentDescription = null, tint = LightTextTertiary, modifier = Modifier.size(44.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = if (searchQuery.isBlank()) "No projects yet" else "No matching projects",
              style = MaterialTheme.typography.bodyMedium.copy(color = LightTextSecondary, fontWeight = FontWeight.Medium)
            )
            Text(
              text = "Tap + New Project above to begin editing",
              style = MaterialTheme.typography.bodySmall.copy(color = LightTextTertiary, fontSize = 11.sp)
            )
          }
        }
      }
    } else {
      items(filteredProjects, key = { it.id }) { project ->
        ProjectCardItem(
          project = project,
          onClick = { onOpenProject(project) },
          onRename = {
            renameTarget = project
            renameText = project.name
          },
          onDuplicate = { onDuplicateProject(project) },
          onDelete = { onDeleteProject(project) }
        )
      }
    }
  }

  // Rename Dialog
  renameTarget?.let { target ->
    AlertDialog(
      onDismissRequest = { renameTarget = null },
      title = { Text("Rename Project", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
      text = {
        OutlinedTextField(
          value = renameText,
          onValueChange = { renameText = it },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(8.dp)
        )
      },
      confirmButton = {
        TextButton(
          onClick = {
            if (renameText.isNotBlank()) {
              onRenameProject(target, renameText.trim())
            }
            renameTarget = null
          }
        ) {
          Text("Save", color = PremierAccent, fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { renameTarget = null }) {
          Text("Cancel", color = LightTextSecondary)
        }
      },
      containerColor = LightCardBg
    )
  }
}

@Composable
private fun ProjectCardItem(
  project: ProjectEntity,
  onClick: () -> Unit,
  onRename: () -> Unit,
  onDuplicate: () -> Unit,
  onDelete: () -> Unit
) {
  var showMenu by remember { mutableStateOf(false) }
  val dateFormat = remember { SimpleDateFormat("MMM d, yyyy • HH:mm", Locale.getDefault()) }
  val dateStr = remember(project.lastEditedTime) { dateFormat.format(Date(project.lastEditedTime)) }

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = ripple(bounded = true),
        onClick = onClick
      )
      .testTag("project_item_${project.id}"),
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(containerColor = LightCardBg),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(LightCardBorder, LightCardBorder)))
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      // Thumbnail / Icon
      Box(
        modifier = Modifier
          .size(48.dp)
          .clip(RoundedCornerShape(8.dp))
          .background(Color(0xFFE2E8F0)),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Default.Movie,
          contentDescription = null,
          tint = PremierAccent,
          modifier = Modifier.size(24.dp)
        )
      }

      Spacer(modifier = Modifier.width(12.dp))

      // Project Info
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = project.name,
          style = MaterialTheme.typography.titleSmall.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = LightTextPrimary
          ),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = "${project.aspectRatio} • ${project.resolution} • $dateStr",
          style = MaterialTheme.typography.bodySmall.copy(
            fontSize = 11.sp,
            color = LightTextSecondary
          ),
          maxLines = 1
        )
      }

      // More Menu
      Box {
        IconButton(
          onClick = { showMenu = true },
          modifier = Modifier.size(36.dp)
        ) {
          Icon(
            imageVector = Icons.Default.MoreVert,
            contentDescription = "Options",
            tint = LightTextSecondary,
            modifier = Modifier.size(18.dp)
          )
        }

        DropdownMenu(
          expanded = showMenu,
          onDismissRequest = { showMenu = false },
          modifier = Modifier.background(LightCardBg)
        ) {
          DropdownMenuItem(
            text = { Text("Rename", fontSize = 13.sp, color = LightTextPrimary) },
            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = LightTextSecondary, modifier = Modifier.size(16.dp)) },
            onClick = {
              showMenu = false
              onRename()
            }
          )
          DropdownMenuItem(
            text = { Text("Duplicate", fontSize = 13.sp, color = LightTextPrimary) },
            leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null, tint = LightTextSecondary, modifier = Modifier.size(16.dp)) },
            onClick = {
              showMenu = false
              onDuplicate()
            }
          )
          DropdownMenuItem(
            text = { Text("Delete", fontSize = 13.sp, color = Color(0xFFEF4444)) },
            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp)) },
            onClick = {
              showMenu = false
              onDelete()
            }
          )
        }
      }
    }
  }
}
