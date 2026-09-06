package com.example

import android.Manifest
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.service.CaptureOverlayService
import com.example.ui.screens.ExportScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ReviewScreen
import com.example.ui.theme.PolishBackground
import com.example.ui.theme.PolishCardBorder
import com.example.ui.theme.PolishPrimary
import com.example.ui.theme.PolishPrimaryDark
import com.example.ui.theme.PolishTextPrimary
import com.example.ui.theme.PolishTextSecondary
import com.example.ui.theme.UpiNoteLoggerTheme
import com.example.ui.viewmodel.MainViewModel

enum class AppTab {
    HOME,
    REVIEW,
    EXPORT
}

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            UpiNoteLoggerTheme {
                val snackbarHostState = remember { SnackbarHostState() }
                val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()
                val isServiceRunning by viewModel.isCaptureServiceRunning.collectAsStateWithLifecycle()

                var currentTab by remember { mutableStateOf(AppTab.HOME) }
                var showCaptureDeniedDialog by remember { mutableStateOf(false) }

                // Screen Capture (MediaProjection) Launcher
                val screenCaptureLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult()
                ) { result ->
                    if (result.resultCode == RESULT_OK && result.data != null) {
                        CaptureOverlayService.startService(this, result.resultCode, result.data!!)
                        Toast.makeText(this, "Capture overlay active! Switch to your UPI app", Toast.LENGTH_SHORT).show()
                    } else {
                        showCaptureDeniedDialog = true
                    }
                }

                // Notification Permission Launcher (Android 13+)
                val notificationPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { _ ->
                    launchMediaProjectionIntent(screenCaptureLauncher)
                }

                val startCaptureFlow: () -> Unit = {
                    if (!viewModel.hasOverlayPermission(this@MainActivity)) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:$packageName")
                            )
                            startActivity(intent)
                        }
                    } else {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            launchMediaProjectionIntent(screenCaptureLauncher)
                        }
                    }
                }

                LaunchedEffect(snackbarMessage) {
                    snackbarMessage?.let { msg ->
                        snackbarHostState.showSnackbar(msg)
                        viewModel.clearSnackbar()
                    }
                }

                BackHandler(enabled = currentTab != AppTab.HOME) {
                    currentTab = AppTab.HOME
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = PolishBackground,
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    bottomBar = {
                        ProfessionalPolishNavBar(
                            currentTab = currentTab,
                            onTabSelected = { currentTab = it }
                        )
                    },
                    floatingActionButton = {
                        // Floating action trigger from design (w-14 h-14 bg-[#0061A4] rounded-2xl shadow-xl)
                        FloatingActionButton(
                            onClick = {
                                if (isServiceRunning) {
                                    Toast.makeText(this@MainActivity, "Floating overlay is currently visible", Toast.LENGTH_SHORT).show()
                                } else {
                                    startCaptureFlow()
                                }
                            },
                            containerColor = PolishPrimary,
                            contentColor = Color.White,
                            shape = RoundedCornerShape(18.dp),
                            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp),
                            modifier = Modifier
                                .size(56.dp)
                                .testTag("fab_capture_button")
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .border(2.dp, Color.White, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        AnimatedContent(
                            targetState = currentTab,
                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                            modifier = Modifier.fillMaxSize(),
                            label = "tab_transition"
                        ) { tab ->
                            when (tab) {
                                AppTab.HOME -> {
                                    HomeScreen(
                                        viewModel = viewModel,
                                        onStartCaptureSession = startCaptureFlow,
                                        onStopCaptureSession = {
                                            CaptureOverlayService.stopService(this@MainActivity)
                                            Toast.makeText(this@MainActivity, "Capture overlay stopped", Toast.LENGTH_SHORT).show()
                                        },
                                        onNavigateToReview = { currentTab = AppTab.REVIEW },
                                        onNavigateToExport = { currentTab = AppTab.EXPORT }
                                    )
                                }
                                AppTab.REVIEW -> {
                                    ReviewScreen(
                                        viewModel = viewModel,
                                        onNavigateBack = { currentTab = AppTab.HOME }
                                    )
                                }
                                AppTab.EXPORT -> {
                                    ExportScreen(
                                        viewModel = viewModel
                                    )
                                }
                            }
                        }
                    }
                }

                // Screen Capture Permission Denied Explanation Dialog
                if (showCaptureDeniedDialog) {
                    AlertDialog(
                        onDismissRequest = { showCaptureDeniedDialog = false },
                        title = { Text("Screen Capture Permission Needed") },
                        text = {
                            Text("UPI Note Logger requires screen capture access to take a screenshot only when you tap the floating overlay button. No audio or continuous video is recorded, and no data leaves your device.")
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    showCaptureDeniedDialog = false
                                    launchMediaProjectionIntent(screenCaptureLauncher)
                                }
                            ) {
                                Text("Try Again")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showCaptureDeniedDialog = false }) {
                                Text("Cancel")
                            }
                        }
                    )
                }
            }
        }
    }

    private fun launchMediaProjectionIntent(launcher: androidx.activity.result.ActivityResultLauncher<Intent>) {
        val mpManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        launcher.launch(mpManager.createScreenCaptureIntent())
    }
}

@Composable
fun ProfessionalPolishNavBar(
    currentTab: AppTab,
    onTabSelected: (AppTab) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(76.dp)
            .background(PolishBackground)
            .border(
                width = 1.dp,
                color = PolishCardBorder,
                shape = RoundedCornerShape(0.dp)
            )
            .navigationBarsPadding()
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Tab 1: Home
        NavBarItem(
            label = "Home",
            icon = Icons.Default.Home,
            selected = currentTab == AppTab.HOME,
            onClick = { onTabSelected(AppTab.HOME) },
            testTag = "nav_home_tab"
        )

        // Tab 2: Review
        NavBarItem(
            label = "Review",
            icon = Icons.Default.Description,
            selected = currentTab == AppTab.REVIEW,
            onClick = { onTabSelected(AppTab.REVIEW) },
            testTag = "nav_review_tab"
        )

        // Tab 3: Export
        NavBarItem(
            label = "Export",
            icon = Icons.Default.FileDownload,
            selected = currentTab == AppTab.EXPORT,
            onClick = { onTabSelected(AppTab.EXPORT) },
            testTag = "nav_export_tab"
        )
    }
}

@Composable
fun NavBarItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (selected) {
            Box(
                modifier = Modifier
                    .width(64.dp)
                    .height(32.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFD1E4FF)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = PolishPrimaryDark,
                    modifier = Modifier.size(20.dp)
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .width(64.dp)
                    .height(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = PolishTextSecondary.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                color = if (selected) PolishPrimaryDark else PolishTextSecondary.copy(alpha = 0.7f)
            )
        )
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
}
