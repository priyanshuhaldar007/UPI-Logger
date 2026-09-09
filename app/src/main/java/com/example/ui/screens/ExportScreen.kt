package com.example.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.TransactionEntry
import com.example.export.CsvExporter
import com.example.ui.components.EditTransactionDialog
import com.example.ui.components.ImageViewerDialog
import com.example.ui.theme.AppleCardBorder
import com.example.ui.theme.AppleDarkCardBorder
import com.example.ui.theme.AppleRadius
import com.example.ui.theme.AppleSpacing
import com.example.ui.theme.AppleStatusMergedBg
import com.example.ui.theme.AppleStatusMergedFg
import com.example.ui.theme.AppleStatusWaitingBg
import com.example.ui.theme.AppleStatusWaitingFg
import com.example.ui.theme.AppleSwipeReview
import com.example.ui.theme.AppleTypography
import com.example.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun ExportScreen(
    viewModel: MainViewModel,
    onDismissRequest: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val darkTheme = isSystemInDarkTheme()
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    val offsetY = remember { Animatable(0f) }
    val dismissThresholdPx = with(density) { 100.dp.toPx() }

    val allTransactions by viewModel.allTransactions.collectAsStateWithLifecycle()
    val totalCount by viewModel.totalCount.collectAsStateWithLifecycle()
    val reviewedCount = remember(allTransactions) { allTransactions.count { it.isReviewed } }

    var onlyReviewed by remember { mutableStateOf(false) }
    var isExporting by remember { mutableStateOf(false) }

    // QA Table row selection for inline modal correction
    var selectedEntryForEdit by remember { mutableStateOf<TransactionEntry?>(null) }
    var selectedImageForViewer by remember { mutableStateOf<Triple<TransactionEntry, String, String>?>(null) }

    val exportEntries = remember(allTransactions, onlyReviewed) {
        if (onlyReviewed) allTransactions.filter { it.isReviewed } else allTransactions
    }

    val blankNotesCount = remember(exportEntries) { exportEntries.count { it.note.isBlank() } }
    val unmergedCount = remember(exportEntries) { exportEntries.count { !it.isMerged } }

    val tableScrollState = rememberScrollState()

    val cardBorder = if (darkTheme) AppleDarkCardBorder else AppleCardBorder

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .offset { IntOffset(0, offsetY.value.roundToInt().coerceAtLeast(0)) }
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Dedicated drag handle bar at the very top of the screen, outside and above the scrollable Column
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 4.dp)
                    .then(
                        if (onDismissRequest != null) {
                            Modifier.pointerInput(Unit) {
                                detectVerticalDragGestures(
                                    onDragEnd = {
                                        coroutineScope.launch {
                                            if (offsetY.value > dismissThresholdPx) {
                                                onDismissRequest()
                                            } else {
                                                offsetY.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow))
                                            }
                                        }
                                    },
                                    onDragCancel = {
                                        coroutineScope.launch {
                                            offsetY.animateTo(0f)
                                        }
                                    },
                                    onVerticalDrag = { change, dragAmount ->
                                        change.consume()
                                        coroutineScope.launch {
                                            offsetY.snapTo((offsetY.value + dragAmount).coerceAtLeast(0f))
                                        }
                                    }
                                )
                            }
                        } else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                )
            }

            // Scrollable Content Column below the drag handle - only .verticalScroll, no competing drag gestures
            val contentScrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(contentScrollState)
                    .padding(bottom = 84.dp) // Leave space for sticky export action bar
            ) {
            // Header Bar
            Surface(
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AppleSpacing.md, vertical = AppleSpacing.sm)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Pre-Export QA Table",
                                style = AppleTypography.LargeTitle.copy(color = MaterialTheme.colorScheme.onSurface)
                            )
                            Text(
                                text = "Verify columns and tap any row to edit before CSV export",
                                style = AppleTypography.Caption.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }

                        if (onDismissRequest != null) {
                            IconButton(onClick = onDismissRequest) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(AppleSpacing.xs))

                    // Controls & QA Warning Indicators Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Scope switch
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(AppleSpacing.xs),
                            modifier = Modifier.clickable { onlyReviewed = !onlyReviewed }
                        ) {
                            Switch(
                                checked = onlyReviewed,
                                onCheckedChange = { onlyReviewed = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = AppleSwipeReview,
                                    uncheckedThumbColor = Color.White,
                                    uncheckedTrackColor = if (darkTheme) Color(0xFF39393D) else Color(0xFFE5E5EA)
                                )
                            )
                            Text(
                                text = if (onlyReviewed) "Verified Only ($reviewedCount)" else "All Entries ($totalCount)",
                                style = AppleTypography.CaptionEmphasized.copy(color = MaterialTheme.colorScheme.onSurface)
                            )
                        }

                        // QA badges
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(AppleSpacing.xs),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (blankNotesCount > 0) {
                                Surface(
                                    shape = RoundedCornerShape(AppleRadius.chip),
                                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = null,
                                            modifier = Modifier.size(12.dp),
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                        Text(
                                            text = "$blankNotesCount blank notes",
                                            style = AppleTypography.CaptionEmphasized.copy(
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        )
                                    }
                                }
                            }

                            if (unmergedCount > 0) {
                                Surface(
                                    shape = RoundedCornerShape(AppleRadius.chip),
                                    color = AppleStatusWaitingBg,
                                    border = BorderStroke(1.dp, cardBorder)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.HourglassEmpty,
                                            contentDescription = null,
                                            modifier = Modifier.size(12.dp),
                                            tint = AppleStatusWaitingFg
                                        )
                                        Text(
                                            text = "$unmergedCount unmerged",
                                            style = AppleTypography.CaptionEmphasized.copy(
                                                fontSize = 11.sp,
                                                color = AppleStatusWaitingFg
                                            )
                                        )
                                    }
                                }
                            }

                            if (blankNotesCount == 0 && unmergedCount == 0 && exportEntries.isNotEmpty()) {
                                Surface(
                                    shape = RoundedCornerShape(AppleRadius.chip),
                                    color = AppleStatusMergedBg,
                                    border = BorderStroke(1.dp, cardBorder)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            modifier = Modifier.size(12.dp),
                                            tint = AppleStatusMergedFg
                                        )
                                        Text(
                                            text = "All Data Clean",
                                            style = AppleTypography.CaptionEmphasized.copy(
                                                fontSize = 11.sp,
                                                color = AppleStatusMergedFg
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (exportEntries.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(AppleSpacing.xl),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(AppleSpacing.sm)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Text(
                            text = if (onlyReviewed) "No verified entries yet" else "No transactions recorded yet",
                            style = AppleTypography.Subtitle.copy(color = MaterialTheme.colorScheme.onSurface)
                        )
                        Text(
                            text = if (onlyReviewed) "Verify transactions in the Review screen or toggle 'Verified Only' off above." else "Capture Screen A and Screen B receipts to populate records.",
                            style = AppleTypography.Caption.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                        Spacer(modifier = Modifier.height(AppleSpacing.xs))
                        OutlinedButton(
                            onClick = {
                                viewModel.insertSamplePair(context)
                                Toast.makeText(context, "Added sample test pair", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(AppleRadius.chip)
                        ) {
                            Icon(Icons.Default.Science, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(AppleSpacing.xs))
                            Text("Generate Sample Data")
                        }
                    }
                }
            } else {
                // QA Table Container with horizontal & vertical scroll
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(AppleSpacing.sm)
                        .clip(RoundedCornerShape(AppleRadius.card))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(AppleRadius.card))
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .horizontalScroll(tableScrollState)
                    ) {
                        Column(modifier = Modifier.width(IntrinsicSize.Max)) {
                            // Table Header Row
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = AppleSpacing.sm, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TableCell(text = "STATUS", width = 90.dp, isHeader = true)
                                    TableCell(text = "DATE / TIME", width = 140.dp, isHeader = true)
                                    TableCell(text = "AMOUNT (₹)", width = 110.dp, isHeader = true)
                                    TableCell(text = "NOTE / REMARK", width = 180.dp, isHeader = true)
                                    TableCell(text = "PAYEE", width = 140.dp, isHeader = true)
                                    TableCell(text = "UPI VPA", width = 150.dp, isHeader = true)
                                    TableCell(text = "UPI REF ID", width = 140.dp, isHeader = true)
                                    TableCell(text = "METHOD", width = 110.dp, isHeader = true)
                                    TableCell(text = "ACTION", width = 70.dp, isHeader = true)
                                }
                            }

                            // Table Data Rows
                            Column(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                exportEntries.forEach { entry ->
                                    val hasBlankNote = entry.note.isBlank()
                                    val isWaiting = !entry.isMerged

                                    val rowBg = when {
                                        hasBlankNote -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.08f)
                                        isWaiting -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                                        else -> Color.Transparent
                                    }

                                    Surface(
                                        color = rowBg,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { selectedEntryForEdit = entry }
                                            .testTag("qa_table_row_${entry.id}")
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .border(
                                                    BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                                )
                                                .padding(horizontal = AppleSpacing.sm, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Merge Status Cell
                                            Box(
                                                modifier = Modifier.width(90.dp),
                                                contentAlignment = Alignment.CenterStart
                                            ) {
                                                if (entry.isMerged) {
                                                    Surface(
                                                        shape = RoundedCornerShape(AppleRadius.badge),
                                                        color = AppleStatusMergedBg
                                                    ) {
                                                        Text(
                                                            text = "MERGED",
                                                            style = AppleTypography.CaptionEmphasized.copy(
                                                                color = AppleStatusMergedFg,
                                                                fontSize = 10.sp
                                                            ),
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                } else {
                                                    Surface(
                                                        shape = RoundedCornerShape(AppleRadius.badge),
                                                        color = AppleStatusWaitingBg
                                                    ) {
                                                        Text(
                                                            text = "WAITING",
                                                            style = AppleTypography.CaptionEmphasized.copy(
                                                                color = AppleStatusWaitingFg,
                                                                fontSize = 10.sp
                                                            ),
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                            }

                                            // Date
                                            TableCell(
                                                text = if (entry.date.isNotEmpty()) entry.date else "—",
                                                width = 140.dp
                                            )

                                            // Amount
                                            TableCell(
                                                text = if (entry.amount.isNotEmpty()) "₹${entry.amount}" else "—",
                                                width = 110.dp,
                                                isEmphasized = true
                                            )

                                            // Note (with blank warning if missing)
                                            Box(
                                                modifier = Modifier.width(180.dp),
                                                contentAlignment = Alignment.CenterStart
                                            ) {
                                                if (entry.note.isNotBlank()) {
                                                    Text(
                                                        text = entry.note,
                                                        style = AppleTypography.CaptionEmphasized.copy(color = MaterialTheme.colorScheme.onSurface),
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                } else {
                                                    Surface(
                                                        shape = RoundedCornerShape(AppleRadius.badge),
                                                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                                                    ) {
                                                        Text(
                                                            text = "[Blank Note]",
                                                            style = AppleTypography.CaptionEmphasized.copy(
                                                                color = MaterialTheme.colorScheme.error,
                                                                fontSize = 11.sp
                                                            ),
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                            }

                                            // Payee
                                            TableCell(
                                                text = if (entry.payee.isNotEmpty()) entry.payee else "—",
                                                width = 140.dp
                                            )

                                            // VPA
                                            TableCell(
                                                text = if (entry.vpa.isNotEmpty()) entry.vpa else "—",
                                                width = 150.dp,
                                                isSecondary = true
                                            )

                                            // UPI Ref ID
                                            TableCell(
                                                text = if (entry.referenceNumber.isNotEmpty()) entry.referenceNumber else "—",
                                                width = 140.dp,
                                                isSecondary = true
                                            )

                                            // Payment Method
                                            TableCell(
                                                text = if (entry.paymentMethod.isNotEmpty()) entry.paymentMethod else "UPI",
                                                width = 110.dp,
                                                isSecondary = true
                                            )

                                            // Edit Button
                                            Box(
                                                modifier = Modifier.width(70.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                IconButton(
                                                    onClick = { selectedEntryForEdit = entry },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Edit,
                                                        contentDescription = "Edit",
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Sticky Export Action Bottom Bar (Accessible directly from QA view)
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
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
                        text = "${exportEntries.size} entries ready",
                        style = AppleTypography.Subtitle.copy(color = MaterialTheme.colorScheme.onSurface)
                    )
                    Text(
                        text = if (onlyReviewed) "Verified entries filter active" else "All recorded entries included",
                        style = AppleTypography.Caption.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }

                Button(
                    onClick = {
                        if (exportEntries.isEmpty()) {
                            Toast.makeText(context, "No records to export", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        isExporting = true
                        viewModel.exportCsv(context, onlyReviewed) { result ->
                            isExporting = false
                            if (result.isSuccess) {
                                if (result.shareUri != null) {
                                    try {
                                        val shareIntent = CsvExporter.createShareIntent(result.shareUri)
                                        context.startActivity(Intent.createChooser(shareIntent, "Share CSV"))
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Could not open share sheet: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                Toast.makeText(
                                    context,
                                    "CSV successfully exported: ${result.rowCount} rows",
                                    Toast.LENGTH_LONG
                                ).show()
                            } else {
                                Toast.makeText(
                                    context,
                                    "Export error: ${result.errorMessage}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    },
                    enabled = exportEntries.isNotEmpty() && !isExporting,
                    modifier = Modifier.testTag("export_csv_sticky_button"),
                    shape = RoundedCornerShape(AppleRadius.chip),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    if (isExporting) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(AppleSpacing.xs))
                        Text("Exporting…", color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(AppleSpacing.xs))
                        Text(
                            "Export & Share CSV (${exportEntries.size})",
                            style = AppleTypography.BodyEmphasized.copy(color = MaterialTheme.colorScheme.onPrimary)
                        )
                    }
                }
            }
        }
    }

    // Modal Edit dialog for quick QA corrections without leaving this screen
    selectedEntryForEdit?.let { entry ->
        EditTransactionDialog(
            entry = entry,
            onSave = { updated ->
                viewModel.updateEntry(updated)
                selectedEntryForEdit = null
            },
            onDelete = { toDelete ->
                viewModel.deleteEntry(toDelete)
                selectedEntryForEdit = null
            },
            onManualMergeRequest = null,
            onViewImage = { path, title ->
                selectedImageForViewer = Triple(entry, path, title)
            },
            onDeleteScreenshot = { isScreenB ->
                if (isScreenB) {
                    viewModel.deleteScreenshotB(entry)
                } else {
                    viewModel.deleteScreenshotA(entry)
                }
                selectedEntryForEdit = if (isScreenB) {
                    entry.copy(screenshotBPath = null)
                } else {
                    entry.copy(screenshotAPath = null)
                }
            },
            onDismiss = { selectedEntryForEdit = null }
        )
    }

    // Image Viewer Modal
    selectedImageForViewer?.let { (entry, path, title) ->
        val initialPage = if (entry.screenshotBPath != null && path == entry.screenshotBPath) 1 else 0
        ImageViewerDialog(
            screenshotAPath = entry.screenshotAPath,
            screenshotBPath = entry.screenshotBPath,
            initialPage = initialPage,
            title = title,
            onDismiss = { selectedImageForViewer = null }
        )
    }
}

@Composable
private fun TableCell(
    text: String,
    width: androidx.compose.ui.unit.Dp,
    isHeader: Boolean = false,
    isEmphasized: Boolean = false,
    isSecondary: Boolean = false
) {
    Box(
        modifier = Modifier.width(width),
        contentAlignment = Alignment.CenterStart
    ) {
        val style = when {
            isHeader -> AppleTypography.CaptionEmphasized.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            isEmphasized -> AppleTypography.BodyEmphasized.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 13.sp
            )
            isSecondary -> AppleTypography.Caption.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
            else -> AppleTypography.Body.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 12.sp
            )
        }
        Text(
            text = text,
            style = style,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
