package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.ui.draw.shadow
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
import com.example.ui.theme.PolishBackground
import com.example.ui.theme.PolishCardBorder
import com.example.ui.theme.PolishCardInnerBorder
import com.example.ui.theme.PolishMergedContainer
import com.example.ui.theme.PolishMergedText
import com.example.ui.theme.PolishPrimary
import com.example.ui.theme.PolishPrimaryBorder
import com.example.ui.theme.PolishPrimaryContainer
import com.example.ui.theme.PolishPrimaryDark
import com.example.ui.theme.PolishSurface
import com.example.ui.theme.PolishSurfaceVariant
import com.example.ui.theme.PolishTextMuted
import com.example.ui.theme.PolishTextPrimary
import com.example.ui.theme.PolishTextSecondary
import com.example.ui.theme.PolishWaitingContainer
import com.example.ui.theme.PolishWaitingText
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PolishBackground)
    ) {
        // --- Header ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, top = 28.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "UPI Note Logger",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.5).sp,
                        color = PolishPrimaryDark
                    )
                )
                Text(
                    text = "Local-only utility",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = PolishTextSecondary
                    )
                )
            }

            // Circular header action badge (Simulate/Info)
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(PolishSurface)
                    .border(1.dp, PolishCardBorder, CircleShape)
                    .clickable {
                        viewModel.insertSamplePair(context)
                    }
                    .testTag("header_sample_button"),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .border(2.dp, PolishTextSecondary, RoundedCornerShape(4.dp))
                )
            }
        }

        // Main content area
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Permission Banner (if needed)
            if (!hasOverlayPermission) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("overlay_permission_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = PolishWaitingContainer),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFB4AB))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = PolishWaitingText,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Overlay Permission Required",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = PolishWaitingText
                            )
                            Text(
                                text = "Enable to show floating button over your UPI app.",
                                style = MaterialTheme.typography.bodySmall,
                                color = PolishTextPrimary
                            )
                        }
                        TextButton(
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                    val intent = Intent(
                                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        Uri.parse("package:${context.packageName}")
                                    )
                                    context.startActivity(intent)
                                }
                            }
                        ) {
                            Text("Enable", color = PolishWaitingText, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // --- Capture Session Hero Card ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(PolishPrimaryContainer)
                    .border(1.dp, PolishPrimaryBorder, RoundedCornerShape(28.dp))
                    .padding(20.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column {
                            Text(
                                text = "CAPTURE SESSION",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.2.sp,
                                    color = PolishPrimaryDark.copy(alpha = 0.7f)
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isServiceRunning) "Service is Active" else "Service is Inactive",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = PolishPrimaryDark
                                )
                            )
                        }

                        // Modern Custom Switch / Pill Toggle
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
                                checkedTrackColor = PolishPrimary,
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = Color(0xFFBAC7D5)
                            ),
                            modifier = Modifier.testTag("capture_session_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = if (isServiceRunning) {
                            "Floating capture button is active! Open super.money, tap it on Screen A, view more, and tap on Screen B."
                        } else {
                            "Toggle to enable the floating capture button over your UPI app."
                        },
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 13.sp,
                            color = Color(0xFF003355),
                            lineHeight = 18.sp
                        )
                    )
                }
            }

            // --- Stats 3-Column Row ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Pending Card
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigateToReview() }
                        .testTag("stat_pending_card"),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = PolishSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PolishCardBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp, horizontal = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "$unreviewedCount",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Light,
                                color = PolishPrimary
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Pending",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = PolishTextSecondary
                            )
                        )
                    }
                }

                // Total Card
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigateToReview() }
                        .testTag("stat_total_card"),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = PolishSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PolishCardBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp, horizontal = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "$totalCount",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Light,
                                color = PolishTextSecondary
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Total",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = PolishTextSecondary
                            )
                        )
                    }
                }

                // Export Card
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigateToExport() }
                        .testTag("stat_export_card"),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = PolishSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PolishCardBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp, horizontal = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "CSV",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Light,
                                color = PolishMergedText
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Export",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = PolishTextSecondary
                            )
                        )
                    }
                }
            }

            // --- Re-analyze All Captures Button ---
            OutlinedButton(
                onClick = { showReanalyzeConfirmDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .testTag("reanalyze_all_captures_button"),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, PolishCardBorder),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = PolishSurface,
                    contentColor = PolishPrimary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = PolishPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Re-analyze All Captures",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = PolishPrimary
                    )
                )
            }

            // --- Recent Activity Section Container ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                    .background(PolishSurface)
                    .border(
                        width = 1.dp,
                        color = PolishCardBorder,
                        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
                    )
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Section header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Recent Activity",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Medium,
                                color = PolishTextPrimary
                            )
                        )

                        TextButton(
                            onClick = onNavigateToReview,
                            modifier = Modifier.testTag("home_review_all_button")
                        ) {
                            Text(
                                text = "Review All",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = PolishPrimary
                                )
                            )
                        }
                    }

                    // Activity Items list
                    if (recentTransactions.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "No captured transactions yet",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = PolishTextSecondary
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                    onClick = { viewModel.insertSamplePair(context) },
                                    colors = ButtonDefaults.buttonColors(containerColor = PolishPrimary),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Create Test Sample Pair")
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(recentTransactions, key = { it.id }) { item ->
                                RecentActivityItemCard(
                                    entry = item,
                                    onClick = { selectedEntryForDetail = item },
                                    onImageClick = { path, title ->
                                        selectedImageForViewer = Triple(item, path, title)
                                    }
                                )
                            }
                            item {
                                Spacer(modifier = Modifier.height(80.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    // Detail / Edit Dialog
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

    // Re-analyze Confirmation Dialog
    if (showReanalyzeConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showReanalyzeConfirmDialog = false },
            title = {
                Text(
                    text = "Re-analyze All Captures?",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Text(
                    text = "This will re-run OCR and parsing on every stored capture using the latest crop areas and parser rules, and retroactively merge any leftover unpaired entries. This may take a while for large datasets and re-reads stored screenshots.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = PolishTextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showReanalyzeConfirmDialog = false
                        viewModel.reprocessAllEntries(context)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PolishPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Re-analyze")
                }
            },
            dismissButton = {
                TextButton(onClick = { showReanalyzeConfirmDialog = false }) {
                    Text("Cancel", color = PolishTextSecondary)
                }
            }
        )
    }

    // Re-analyze Progress Dialog
    if (reprocessProgress.isReprocessing) {
        AlertDialog(
            onDismissRequest = { /* Cannot dismiss while running */ },
            properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
            title = {
                Text(
                    text = "Re-analyzing Captures...",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val progressLabel = if (reprocessProgress.total > 0) {
                        "Reprocessing ${reprocessProgress.current} of ${reprocessProgress.total}"
                    } else {
                        "Preparing captures..."
                    }
                    Text(
                        text = progressLabel,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium,
                            color = PolishTextPrimary
                        )
                    )
                    if (reprocessProgress.total > 0) {
                        LinearProgressIndicator(
                            progress = { reprocessProgress.current.toFloat() / reprocessProgress.total.coerceAtLeast(1) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp),
                            color = PolishPrimary,
                            trackColor = PolishPrimaryContainer
                        )
                    } else {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp),
                            color = PolishPrimary,
                            trackColor = PolishPrimaryContainer
                        )
                    }
                }
            },
            confirmButton = {}
        )
    }

    // Re-analyze Summary Dialog
    reprocessSummary?.let { summary ->
        AlertDialog(
            onDismissRequest = { viewModel.clearReprocessSummary() },
            title = {
                Text(
                    text = "Re-analysis Complete",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Text(
                    text = "Reprocessed ${summary.totalEntries} entries (${summary.reprocessedFromImage} from saved images, ${summary.reprocessedFromTextOnly} from stored text only, ${summary.missingScreenshotFiles} screenshots were missing). Merged ${summary.newlyMergedPairs} additional pairs.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = PolishTextPrimary
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.clearReprocessSummary() },
                    colors = ButtonDefaults.buttonColors(containerColor = PolishPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Done")
                }
            }
        )
    }
}

@Composable
fun RecentActivityItemCard(
    entry: TransactionEntry,
    onClick: () -> Unit,
    onImageClick: (String, String) -> Unit
) {
    val context = LocalContext.current
    val screenshotFile = (entry.screenshotBPath ?: entry.screenshotAPath)?.let { File(it) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFF7F9FB))
            .border(1.dp, PolishCardInnerBorder, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Thumbnail Image
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFDEE3EB))
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
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF74777F).copy(alpha = 0.25f))
                )
            }
        }

        // Center Content
        Column(modifier = Modifier.weight(1f)) {
            // Amount & Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val displayAmount = if (entry.amount.isNotEmpty()) "₹${entry.amount}" else "₹ --"
                Text(
                    text = displayAmount,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = PolishTextPrimary
                    )
                )

                if (entry.isMerged) {
                    Box(
                        modifier = Modifier
                            .background(PolishMergedContainer, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "MERGED",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = PolishMergedText
                            )
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFDEE3EB), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "WAITING",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = PolishTextSecondary
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Payee / Reference
            val subtitle = when {
                entry.payee.isNotEmpty() -> "Paid to: ${entry.payee}"
                entry.vpa.isNotEmpty() -> "Paid to: ${entry.vpa}"
                entry.referenceNumber.isNotEmpty() -> "Ref: ${entry.referenceNumber}"
                else -> "Captured Screen"
            }
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp,
                    color = PolishTextSecondary
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Note (Italic) or Missing Warning
            if (entry.note.isNotEmpty()) {
                Text(
                    text = "\"${entry.note}\"",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.sp,
                        fontStyle = FontStyle.Italic,
                        color = PolishTextMuted
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
            } else if (!entry.isMerged) {
                Text(
                    text = "Missing Screen B (Note)",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 10.sp,
                        color = PolishWaitingText
                    ),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}
