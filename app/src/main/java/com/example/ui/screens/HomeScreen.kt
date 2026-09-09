package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CallMerge
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.TransactionEntry
import com.example.ui.components.EditTransactionDialog
import com.example.ui.components.ExportCsvDialog
import com.example.ui.components.ImageViewerDialog
import com.example.ui.components.ManualMergeDialog
import com.example.ui.theme.AppleAccent
import com.example.ui.theme.AppleAccentLight
import com.example.ui.theme.AppleBackground
import com.example.ui.theme.AppleCardBorder
import com.example.ui.theme.AppleCardInnerBorder
import com.example.ui.theme.AppleDarkAccent
import com.example.ui.theme.AppleDarkBackground
import com.example.ui.theme.AppleDarkCardBorder
import com.example.ui.theme.AppleDarkSurface
import com.example.ui.theme.AppleDarkSurfaceElevated
import com.example.ui.theme.AppleDarkSurfaceTranslucent
import com.example.ui.theme.AppleDarkTextPrimary
import com.example.ui.theme.AppleDarkTextSecondary
import com.example.ui.theme.AppleDarkTextTertiary
import com.example.ui.theme.AppleMotion
import com.example.ui.theme.AppleRadius
import com.example.ui.theme.AppleSpacing
import com.example.ui.theme.AppleStatusMergedBg
import com.example.ui.theme.AppleStatusMergedFg
import com.example.ui.theme.AppleStatusScreenABg
import com.example.ui.theme.AppleStatusScreenAFg
import com.example.ui.theme.AppleStatusScreenBBg
import com.example.ui.theme.AppleStatusScreenBFg
import com.example.ui.theme.AppleSurface
import com.example.ui.theme.AppleSurfaceElevated
import com.example.ui.theme.AppleSurfaceTranslucent
import com.example.ui.theme.AppleSwipeReview
import com.example.ui.theme.AppleSwipeUnreview
import com.example.ui.theme.AppleTextPrimary
import com.example.ui.theme.AppleTextSecondary
import com.example.ui.theme.AppleTextTertiary
import com.example.ui.theme.AppleTypography
import com.example.ui.theme.LocalReduceMotion
import com.example.ui.viewmodel.MainViewModel
import java.io.File

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onStartCaptureSession: () -> Unit,
    onStopCaptureSession: () -> Unit,
    onNavigateToReview: () -> Unit,
    onNavigateToExport: () -> Unit
) {
    val context = LocalContext.current
    val darkTheme = isSystemInDarkTheme()
    val reduceMotion = LocalReduceMotion.current

    val totalCount by viewModel.totalCount.collectAsStateWithLifecycle()
    val unreviewedCount by viewModel.unreviewedCount.collectAsStateWithLifecycle()
    val allTransactions by viewModel.allTransactions.collectAsStateWithLifecycle()
    val isServiceRunning by viewModel.isCaptureServiceRunning.collectAsStateWithLifecycle()
    val reprocessProgress by viewModel.reprocessProgress.collectAsStateWithLifecycle()
    val reprocessSummary by viewModel.reprocessSummary.collectAsStateWithLifecycle()

    var showReanalyzeConfirmDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var selectedEntryForDetail by remember { mutableStateOf<TransactionEntry?>(null) }
    var selectedImageForViewer by remember { mutableStateOf<Triple<TransactionEntry, String, String>?>(null) }
    var entryForManualMerge by remember { mutableStateOf<TransactionEntry?>(null) }
    var hasOverlayPermission by remember { mutableStateOf(viewModel.hasOverlayPermission(context)) }

    val recentTransactions = remember(allTransactions) {
        allTransactions.take(8)
    }

    val bgColor = if (darkTheme) AppleDarkBackground else AppleBackground
    val cardSurface = if (darkTheme) AppleDarkSurfaceTranslucent else AppleSurfaceTranslucent
    val cardElevated = if (darkTheme) AppleDarkSurfaceElevated else AppleSurfaceElevated
    val cardBorder = if (darkTheme) AppleDarkCardBorder else AppleCardBorder
    val textPrimary = if (darkTheme) AppleDarkTextPrimary else AppleTextPrimary
    val textSecondary = if (darkTheme) AppleDarkTextSecondary else AppleTextSecondary
    val textTertiary = if (darkTheme) AppleDarkTextTertiary else AppleTextTertiary
    val accentColor = if (darkTheme) AppleDarkAccent else AppleAccent

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        // --- Large Confident Header Treatment ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = AppleSpacing.lg,
                    end = AppleSpacing.lg,
                    top = AppleSpacing.xl,
                    bottom = AppleSpacing.sm
                ),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "UPI Note Logger",
                    style = AppleTypography.LargeTitle.copy(color = textPrimary)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isServiceRunning) AppleSwipeReview else textTertiary)
                    )
                    Spacer(modifier = Modifier.width(AppleSpacing.xxs))
                    Text(
                        text = if (isServiceRunning) "Capture Overlay Active" else "Local-Only Financial Utility",
                        style = AppleTypography.CaptionEmphasized.copy(
                            color = if (isServiceRunning) AppleSwipeReview else textSecondary
                        )
                    )
                }
            }

            // Discreet Test Sample Generator Button
            Surface(
                shape = CircleShape,
                color = cardElevated,
                border = BorderStroke(1.dp, cardBorder),
                modifier = Modifier
                    .size(42.dp)
                    .clickable { viewModel.insertSamplePair(context) }
                    .testTag("header_sample_button")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Generate Test Capture",
                        tint = textSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // --- Main Screen Content ---
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = AppleSpacing.md),
            verticalArrangement = Arrangement.spacedBy(AppleSpacing.sm)
        ) {
            // Overlay Permission Warning Banner (if permission missing)
            if (!hasOverlayPermission) {
                Surface(
                    shape = RoundedCornerShape(AppleRadius.card),
                    color = if (darkTheme) Color(0xFF331B1B) else Color(0xFFFFECEB),
                    border = BorderStroke(1.dp, Color(0xFFFF5252).copy(alpha = 0.3f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("overlay_permission_card")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(AppleSpacing.md),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFFF3B30),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(AppleSpacing.sm))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Overlay Permission Required",
                                style = AppleTypography.Subtitle.copy(fontSize = 15.sp),
                                color = textPrimary
                            )
                            Text(
                                text = "Required to display floating capture buttons over super.money.",
                                style = AppleTypography.Caption,
                                color = textSecondary
                            )
                        }
                        Button(
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                    val intent = Intent(
                                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        Uri.parse("package:${context.packageName}")
                                    )
                                    context.startActivity(intent)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF3B30)),
                            shape = RoundedCornerShape(AppleRadius.chip)
                        ) {
                            Text("Enable", style = AppleTypography.CaptionEmphasized, color = Color.White)
                        }
                    }
                }
            }

            // --- HERO: Primary Capture Action (Capsule Button & Status Card) ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(AppleRadius.card),
                colors = CardDefaults.cardColors(containerColor = cardSurface),
                border = BorderStroke(1.dp, cardBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(AppleSpacing.md)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "SCREEN CAPTURE",
                                style = AppleTypography.CaptionEmphasized.copy(
                                    letterSpacing = 1.sp,
                                    color = accentColor
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isServiceRunning) "Overlay Active" else "Ready to Record",
                                style = AppleTypography.Title.copy(color = textPrimary)
                            )
                        }

                        // iOS-style Refined Switch Component
                        Switch(
                            checked = isServiceRunning,
                            onCheckedChange = { start ->
                                if (start) {
                                    hasOverlayPermission = viewModel.hasOverlayPermission(context)
                                    if (hasOverlayPermission) {
                                        onStartCaptureSession()
                                    } else {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                            val intent = Intent(
                                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                                Uri.parse("package:${context.packageName}")
                                            )
                                            context.startActivity(intent)
                                        }
                                    }
                                } else {
                                    onStopCaptureSession()
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = AppleSwipeReview,
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = if (darkTheme) Color(0xFF39393D) else Color(0xFFE5E5EA)
                            ),
                            modifier = Modifier.testTag("capture_session_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(AppleSpacing.xs))

                    Text(
                        text = if (isServiceRunning) {
                            "Tap 'A' on the payment receipt, view transaction details, and tap 'B' to merge the note."
                        } else {
                            "Launch floating buttons over your screen to extract note and transaction details in two taps."
                        },
                        style = AppleTypography.Body.copy(color = textSecondary)
                    )

                    Spacer(modifier = Modifier.height(AppleSpacing.md))

                    // Prominent Capsule Primary Button
                    Button(
                        onClick = {
                            if (isServiceRunning) {
                                onStopCaptureSession()
                            } else {
                                hasOverlayPermission = viewModel.hasOverlayPermission(context)
                                if (hasOverlayPermission) {
                                    onStartCaptureSession()
                                } else {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                        val intent = Intent(
                                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                            Uri.parse("package:${context.packageName}")
                                        )
                                        context.startActivity(intent)
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("start_capture_button"),
                        shape = RoundedCornerShape(AppleRadius.sheet),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isServiceRunning) Color(0xFFFF3B30) else accentColor
                        )
                    ) {
                        Icon(
                            imageVector = if (isServiceRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(AppleSpacing.xs))
                        Text(
                            text = if (isServiceRunning) "Stop Capture Session" else "Start Capture Session",
                            style = AppleTypography.BodyEmphasized.copy(color = Color.White)
                        )
                    }
                }
            }

            // --- Unreviewed Status Pill & Glanceable Stats Row ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppleSpacing.xs)
            ) {
                // Glanceable Unreviewed Pill / Card
                Surface(
                    modifier = Modifier
                        .weight(1.3f)
                        .clickable { onNavigateToReview() }
                        .testTag("stat_pending_card"),
                    shape = RoundedCornerShape(AppleRadius.card),
                    color = if (unreviewedCount > 0) {
                        if (darkTheme) Color(0xFF2C2216) else Color(0xFFFFF7ED)
                    } else cardSurface,
                    border = BorderStroke(
                        1.dp,
                        if (unreviewedCount > 0) AppleSwipeUnreview.copy(alpha = 0.4f) else cardBorder
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = AppleSpacing.md, vertical = AppleSpacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "$unreviewedCount",
                                style = AppleTypography.NumericHero.copy(
                                    color = if (unreviewedCount > 0) AppleSwipeUnreview else textSecondary
                                )
                            )
                            Text(
                                text = "Needs Review",
                                style = AppleTypography.CaptionEmphasized.copy(
                                    color = if (unreviewedCount > 0) AppleSwipeUnreview else textSecondary
                                )
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = if (unreviewedCount > 0) AppleSwipeUnreview else textTertiary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Total Entries Pill
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigateToReview() }
                        .testTag("stat_total_card"),
                    shape = RoundedCornerShape(AppleRadius.card),
                    color = cardSurface,
                    border = BorderStroke(1.dp, cardBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = AppleSpacing.md, vertical = AppleSpacing.sm),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "$totalCount",
                            style = AppleTypography.NumericHero.copy(color = textPrimary)
                        )
                        Text(
                            text = "Total Records",
                            style = AppleTypography.CaptionEmphasized.copy(color = textSecondary)
                        )
                    }
                }

                // Quick CSV Export Pill
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigateToExport() }
                        .testTag("stat_export_card"),
                    shape = RoundedCornerShape(AppleRadius.card),
                    color = cardSurface,
                    border = BorderStroke(1.dp, cardBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = AppleSpacing.md, vertical = AppleSpacing.sm),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Export CSV",
                            style = AppleTypography.CaptionEmphasized.copy(color = accentColor)
                        )
                    }
                }
            }

            // --- Recent Activity Section Container ---
            Surface(
                shape = RoundedCornerShape(topStart = AppleRadius.sheet, topEnd = AppleRadius.sheet),
                color = cardSurface,
                border = BorderStroke(1.dp, cardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Section header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = AppleSpacing.md, vertical = AppleSpacing.sm),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Recent Activity",
                            style = AppleTypography.Subtitle.copy(color = textPrimary)
                        )

                        TextButton(
                            onClick = onNavigateToReview,
                            modifier = Modifier.testTag("home_review_all_button")
                        ) {
                            Text(
                                text = "View All",
                                style = AppleTypography.CaptionEmphasized.copy(color = accentColor)
                            )
                        }
                    }

                    if (recentTransactions.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(AppleSpacing.lg),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(AppleSpacing.xs)
                            ) {
                                Text(
                                    text = "No Captures Recorded Yet",
                                    style = AppleTypography.Subtitle.copy(color = textPrimary)
                                )
                                Text(
                                    text = "Start a capture session to record transaction notes effortlessly.",
                                    style = AppleTypography.Caption.copy(color = textSecondary)
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(horizontal = AppleSpacing.md),
                            verticalArrangement = Arrangement.spacedBy(AppleSpacing.xs)
                        ) {
                            items(recentTransactions, key = { it.id }) { item ->
                                AppleRecentActivityRow(
                                    entry = item,
                                    darkTheme = darkTheme,
                                    onClick = { selectedEntryForDetail = item },
                                    onImageClick = { path, title ->
                                        selectedImageForViewer = Triple(item, path, title)
                                    }
                                )
                            }

                            // Quiet Maintenance Action at Bottom of Scroll
                            item {
                                Spacer(modifier = Modifier.height(AppleSpacing.sm))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = AppleSpacing.sm),
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    OutlinedButton(
                                        onClick = { showReanalyzeConfirmDialog = true },
                                        shape = RoundedCornerShape(AppleRadius.chip),
                                        border = BorderStroke(1.dp, cardBorder),
                                        modifier = Modifier.testTag("reanalyze_all_captures_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = textSecondary
                                        )
                                        Spacer(modifier = Modifier.width(AppleSpacing.xs))
                                        Text(
                                            text = "Re-analyze All Captures",
                                            style = AppleTypography.CaptionEmphasized.copy(color = textSecondary)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(AppleSpacing.xl))
                            }
                        }
                    }
                }
            }
        }
    }

    // Detail / In-place Editor Modal
    selectedEntryForDetail?.let { entry ->
        EditTransactionDialog(
            entry = entry,
            onSave = { updated ->
                viewModel.updateEntry(updated)
                selectedEntryForDetail = null
            },
            onDelete = { toDelete ->
                viewModel.deleteEntry(toDelete)
                selectedEntryForDetail = null
            },
            onManualMergeRequest = { toMerge ->
                entryForManualMerge = toMerge
            },
            onViewImage = { path, title ->
                selectedImageForViewer = Triple(entry, path, title)
            },
            onDeleteScreenshot = { isScreenB ->
                if (isScreenB) {
                    viewModel.deleteScreenshotB(entry)
                } else {
                    viewModel.deleteScreenshotA(entry)
                }
                selectedEntryForDetail = if (isScreenB) {
                    entry.copy(screenshotBPath = null)
                } else {
                    entry.copy(screenshotAPath = null)
                }
            },
            onDismiss = { selectedEntryForDetail = null }
        )
    }

    // Fullscreen Screenshot Viewer
    selectedImageForViewer?.let { (entry, path, title) ->
        ImageViewerDialog(
            filePath = path,
            title = title,
            onDismiss = { selectedImageForViewer = null },
            onDelete = {
                viewModel.deleteScreenshotByPath(entry, path)
                if (selectedEntryForDetail?.id == entry.id) {
                    selectedEntryForDetail = when {
                        entry.screenshotAPath == path -> entry.copy(screenshotAPath = null)
                        entry.screenshotBPath == path -> entry.copy(screenshotBPath = null)
                        else -> entry
                    }
                }
                selectedImageForViewer = null
            }
        )
    }

    // Manual Merge Dialog
    entryForManualMerge?.let { entry ->
        ManualMergeDialog(
            primaryEntry = entry,
            onLoadCandidates = { id -> viewModel.getUnmergedEntries(id) },
            onMerge = { idA, idB ->
                viewModel.manualMerge(idA, idB)
                entryForManualMerge = null
            },
            onDismiss = { entryForManualMerge = null }
        )
    }

    // Export CSV Dialog
    if (showExportDialog) {
        val reviewedCount = allTransactions.count { it.isReviewed }
        ExportCsvDialog(
            totalEntriesCount = totalCount,
            reviewedEntriesCount = reviewedCount,
            onExport = { ctx, onlyRev, callback ->
                viewModel.exportCsv(ctx, onlyRev, callback)
            },
            onDismiss = { showExportDialog = false }
        )
    }

    // Re-analyze Confirmation Dialog (Apple Dialog Styling)
    if (showReanalyzeConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showReanalyzeConfirmDialog = false },
            shape = RoundedCornerShape(AppleRadius.sheet),
            title = {
                Text(
                    text = "Re-analyze All Captures?",
                    style = AppleTypography.Title.copy(color = textPrimary)
                )
            },
            text = {
                Text(
                    text = "This will re-run OCR and parsing on every stored capture using the latest crop areas and parser rules, and retroactively merge any leftover unpaired entries.",
                    style = AppleTypography.Body.copy(color = textSecondary)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showReanalyzeConfirmDialog = false
                        viewModel.reprocessAllEntries(context)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                    shape = RoundedCornerShape(AppleRadius.chip)
                ) {
                    Text("Re-analyze", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showReanalyzeConfirmDialog = false }) {
                    Text("Cancel", color = textSecondary)
                }
            }
        )
    }

    // Re-analyze Progress Dialog
    if (reprocessProgress.isReprocessing) {
        AlertDialog(
            onDismissRequest = { /* Modal */ },
            shape = RoundedCornerShape(AppleRadius.sheet),
            properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
            title = {
                Text(
                    text = "Re-analyzing Captures…",
                    style = AppleTypography.Title.copy(color = textPrimary)
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = AppleSpacing.xs),
                    verticalArrangement = Arrangement.spacedBy(AppleSpacing.sm)
                ) {
                    val progressLabel = if (reprocessProgress.total > 0) {
                        "Processing ${reprocessProgress.current} of ${reprocessProgress.total}"
                    } else {
                        "Preparing captures…"
                    }
                    Text(
                        text = progressLabel,
                        style = AppleTypography.Body.copy(color = textSecondary)
                    )
                    LinearProgressIndicator(
                        progress = {
                            if (reprocessProgress.total > 0) {
                                reprocessProgress.current.toFloat() / reprocessProgress.total.coerceAtLeast(1)
                            } else 0f
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = accentColor,
                        trackColor = cardBorder
                    )
                }
            },
            confirmButton = {}
        )
    }

    // Re-analyze Summary Dialog
    reprocessSummary?.let { summary ->
        AlertDialog(
            onDismissRequest = { viewModel.clearReprocessSummary() },
            shape = RoundedCornerShape(AppleRadius.sheet),
            title = {
                Text(
                    text = "Re-analysis Complete",
                    style = AppleTypography.Title.copy(color = textPrimary)
                )
            },
            text = {
                Text(
                    text = "Reprocessed ${summary.totalEntries} entries (${summary.reprocessedFromImage} from saved images, ${summary.reprocessedFromTextOnly} from stored text only, ${summary.missingScreenshotFiles} screenshots were missing). Merged ${summary.newlyMergedPairs} additional pairs.",
                    style = AppleTypography.Body.copy(color = textSecondary)
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.clearReprocessSummary() },
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                    shape = RoundedCornerShape(AppleRadius.chip)
                ) {
                    Text("Done", color = Color.White)
                }
            }
        )
    }
}

/**
 * Apple-style Recent Activity Row with quiet badge and clean typography.
 */
@Composable
fun AppleRecentActivityRow(
    entry: TransactionEntry,
    darkTheme: Boolean,
    onClick: () -> Unit,
    onImageClick: (String, String) -> Unit
) {
    val context = LocalContext.current
    val screenshotFile = (entry.screenshotBPath ?: entry.screenshotAPath)?.let { File(it) }

    val rowBg = if (darkTheme) AppleDarkSurfaceElevated else AppleBackground
    val borderCol = if (darkTheme) AppleDarkCardBorder else AppleCardBorder
    val textPri = if (darkTheme) AppleDarkTextPrimary else AppleTextPrimary
    val textSec = if (darkTheme) AppleDarkTextSecondary else AppleTextSecondary
    val textTer = if (darkTheme) AppleDarkTextTertiary else AppleTextTertiary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AppleRadius.card))
            .background(rowBg)
            .border(1.dp, borderCol, RoundedCornerShape(AppleRadius.card))
            .clickable(onClick = onClick)
            .padding(AppleSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppleSpacing.sm)
    ) {
        // Thumbnail Image
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(AppleRadius.chip))
                .background(borderCol)
                .clickable {
                    val path = entry.screenshotBPath ?: entry.screenshotAPath
                    if (path != null) {
                        onImageClick(path, "Transaction Screenshot")
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            if (screenshotFile?.exists() == true) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(screenshotFile)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Capture Preview",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Layers,
                    contentDescription = null,
                    tint = textTer,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Center Details
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val displayAmount = if (entry.amount.isNotEmpty()) {
                    if (entry.amount.startsWith("₹")) entry.amount else "₹${entry.amount}"
                } else "₹ --"

                Text(
                    text = displayAmount,
                    style = AppleTypography.NumericHero.copy(fontSize = 18.sp, color = textPri)
                )

                // Quiet status badge
                val (badgeBg, badgeFg, badgeText) = when {
                    entry.isMerged -> Triple(AppleStatusMergedBg, AppleStatusMergedFg, "Merged")
                    entry.sourceScreenType == "SCREEN_B" -> Triple(AppleStatusScreenBBg, AppleStatusScreenBFg, "Screen B")
                    else -> Triple(AppleStatusScreenABg, AppleStatusScreenAFg, "Screen A")
                }

                Surface(
                    shape = RoundedCornerShape(AppleRadius.chip),
                    color = badgeBg
                ) {
                    Text(
                        text = badgeText,
                        style = AppleTypography.CaptionEmphasized.copy(color = badgeFg, fontSize = 10.sp),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            val subtitle = when {
                entry.payee.isNotEmpty() -> "Paid to: ${entry.payee}"
                entry.vpa.isNotEmpty() -> "VPA: ${entry.vpa}"
                entry.referenceNumber.isNotEmpty() -> "Ref: ${entry.referenceNumber}"
                else -> "Captured Screen"
            }
            Text(
                text = subtitle,
                style = AppleTypography.Caption.copy(color = textSec),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (entry.note.isNotEmpty()) {
                Text(
                    text = "\"${entry.note}\"",
                    style = AppleTypography.CaptionEmphasized.copy(
                        fontStyle = FontStyle.Italic,
                        color = textSec
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}
