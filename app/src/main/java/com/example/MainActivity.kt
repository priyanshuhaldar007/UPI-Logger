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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.ui.theme.AppleAccent
import com.example.ui.theme.AppleBackground
import com.example.ui.theme.AppleCardBorder
import com.example.ui.theme.AppleDarkAccent
import com.example.ui.theme.AppleDarkBackground
import com.example.ui.theme.AppleDarkCardBorder
import com.example.ui.theme.AppleDarkSurface
import com.example.ui.theme.AppleDarkSurfaceElevated
import com.example.ui.theme.AppleDarkSurfaceTranslucent
import com.example.ui.theme.AppleDarkTextPrimary
import com.example.ui.theme.AppleDarkTextSecondary
import com.example.ui.theme.AppleDarkTextTertiary
import com.example.ui.theme.AppleRadius
import com.example.ui.theme.AppleSpacing
import com.example.ui.theme.AppleSurface
import com.example.ui.theme.AppleSurfaceElevated
import com.example.ui.theme.AppleSurfaceTranslucent
import com.example.ui.theme.AppleSwipeReview
import com.example.ui.theme.AppleTextPrimary
import com.example.ui.theme.AppleTextSecondary
import com.example.ui.theme.AppleTextTertiary
import com.example.ui.theme.AppleTypography
import com.example.ui.theme.LocalReduceMotion
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
            val reduceMotion by viewModel.reduceMotion.collectAsStateWithLifecycle()

            CompositionLocalProvider(LocalReduceMotion provides reduceMotion) {
                UpiNoteLoggerTheme {
                    val snackbarHostState = remember { SnackbarHostState() }
                    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()
                    val isServiceRunning by viewModel.isCaptureServiceRunning.collectAsStateWithLifecycle()
                    val darkTheme = isSystemInDarkTheme()

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

                    val bgColor = if (darkTheme) AppleDarkBackground else AppleBackground
                    val accentCol = if (darkTheme) AppleDarkAccent else AppleAccent

                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        containerColor = bgColor,
                        snackbarHost = { SnackbarHost(snackbarHostState) },
                        bottomBar = {
                            AppleNavBar(
                                currentTab = currentTab,
                                darkTheme = darkTheme,
                                onTabSelected = { currentTab = it }
                            )
                        },
                        floatingActionButton = {
                            // Elevated circular Apple Glass Capture Trigger
                            FloatingActionButton(
                                onClick = {
                                    if (isServiceRunning) {
                                        Toast.makeText(this@MainActivity, "Floating overlay is currently active", Toast.LENGTH_SHORT).show()
                                    } else {
                                        startCaptureFlow()
                                    }
                                },
                                containerColor = if (isServiceRunning) AppleSwipeReview else accentCol,
                                contentColor = Color.White,
                                shape = CircleShape,
                                elevation = FloatingActionButtonDefaults.elevation(
                                    defaultElevation = 6.dp,
                                    pressedElevation = 2.dp
                                ),
                                modifier = Modifier
                                    .size(54.dp)
                                    .testTag("fab_capture_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Layers,
                                    contentDescription = "Capture Overlay Action",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
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
                            shape = RoundedCornerShape(AppleRadius.sheet),
                            title = {
                                Text(
                                    "Screen Capture Permission Needed",
                                    style = AppleTypography.Title.copy(
                                        color = if (darkTheme) AppleDarkTextPrimary else AppleTextPrimary
                                    )
                                )
                            },
                            text = {
                                Text(
                                    "UPI Note Logger captures only the single payment receipt frame when you tap the floating button. No audio or continuous video is recorded, and no personal data leaves your device.",
                                    style = AppleTypography.Body.copy(
                                        color = if (darkTheme) AppleDarkTextSecondary else AppleTextSecondary
                                    )
                                )
                            },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        showCaptureDeniedDialog = false
                                        launchMediaProjectionIntent(screenCaptureLauncher)
                                    }
                                ) {
                                    Text("Grant Access", style = AppleTypography.BodyEmphasized.copy(color = accentCol))
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showCaptureDeniedDialog = false }) {
                                    Text("Cancel", style = AppleTypography.Body.copy(color = AppleTextSecondary))
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    private fun launchMediaProjectionIntent(launcher: androidx.activity.result.ActivityResultLauncher<Intent>) {
        val mpManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        launcher.launch(mpManager.createScreenCaptureIntent())
    }
}

/**
 * Apple HIG Translucent Navigation Tab Bar
 */
@Composable
fun AppleNavBar(
    currentTab: AppTab,
    darkTheme: Boolean,
    onTabSelected: (AppTab) -> Unit
) {
    val barBg = if (darkTheme) AppleDarkSurfaceTranslucent else AppleSurfaceTranslucent
    val borderCol = if (darkTheme) AppleDarkCardBorder else AppleCardBorder

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        color = barBg,
        border = BorderStroke(0.5.dp, borderCol)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = AppleSpacing.lg),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppleNavBarItem(
                label = "Home",
                icon = Icons.Default.Home,
                selected = currentTab == AppTab.HOME,
                darkTheme = darkTheme,
                onClick = { onTabSelected(AppTab.HOME) },
                testTag = "nav_home_tab"
            )

            AppleNavBarItem(
                label = "Review",
                icon = Icons.Default.Description,
                selected = currentTab == AppTab.REVIEW,
                darkTheme = darkTheme,
                onClick = { onTabSelected(AppTab.REVIEW) },
                testTag = "nav_review_tab"
            )

            AppleNavBarItem(
                label = "Export",
                icon = Icons.Default.FileDownload,
                selected = currentTab == AppTab.EXPORT,
                darkTheme = darkTheme,
                onClick = { onTabSelected(AppTab.EXPORT) },
                testTag = "nav_export_tab"
            )
        }
    }
}

@Composable
fun AppleNavBarItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    darkTheme: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    val accentCol = if (darkTheme) AppleDarkAccent else AppleAccent
    val unselectedCol = if (darkTheme) AppleDarkTextTertiary else AppleTextTertiary

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(AppleRadius.chip))
            .clickable(onClick = onClick)
            .padding(horizontal = AppleSpacing.md, vertical = AppleSpacing.xxs)
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) accentCol else unselectedCol,
            modifier = Modifier.size(22.dp)
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = label,
            style = AppleTypography.CaptionEmphasized.copy(
                fontSize = 10.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                color = if (selected) accentCol else unselectedCol
            )
        )
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
}

