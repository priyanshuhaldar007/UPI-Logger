package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CallMerge
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.TransactionEntry
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
import com.example.ui.theme.AppleSwipeDelete
import com.example.ui.theme.AppleSwipeReview
import com.example.ui.theme.AppleSwipeUnreview
import com.example.ui.theme.AppleTextPrimary
import com.example.ui.theme.AppleTextSecondary
import com.example.ui.theme.AppleTextTertiary
import com.example.ui.theme.AppleTypography
import com.example.ui.theme.LocalReduceMotion
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.ReviewFilter
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val darkTheme = isSystemInDarkTheme()
    val reduceMotion = LocalReduceMotion.current

    val transactions by viewModel.filteredTransactions.collectAsStateWithLifecycle()
    val activeFilter by viewModel.activeFilter.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val storageStats by viewModel.storageStats.collectAsStateWithLifecycle()

    var selectedImageForViewer by remember { mutableStateOf<Triple<TransactionEntry, String, String>?>(null) }
    var entryForManualMerge by remember { mutableStateOf<TransactionEntry?>(null) }
    var entryToDelete by remember { mutableStateOf<TransactionEntry?>(null) }
    var screenshotToDelete by remember { mutableStateOf<Pair<TransactionEntry, Boolean>?>(null) }
    var showDeleteAllScreenshotsConfirm by remember { mutableStateOf(false) }

    val bgColor = if (darkTheme) AppleDarkBackground else AppleBackground
    val cardSurface = if (darkTheme) AppleDarkSurfaceTranslucent else AppleSurfaceTranslucent
    val textPrimary = if (darkTheme) AppleDarkTextPrimary else AppleTextPrimary
    val textSecondary = if (darkTheme) AppleDarkTextSecondary else AppleTextSecondary
    val accentColor = if (darkTheme) AppleDarkAccent else AppleAccent

    Scaffold(
        containerColor = bgColor,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Review & Edit",
                            style = AppleTypography.Title.copy(color = textPrimary)
                        )
                        Text(
                            text = "${transactions.size} entries",
                            style = AppleTypography.Caption.copy(color = textSecondary)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("review_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Home",
                            tint = accentColor
                        )
                    }
                },
                actions = {
                    if (storageStats.count > 0) {
                        IconButton(
                            onClick = { showDeleteAllScreenshotsConfirm = true },
                            modifier = Modifier.testTag("delete_all_screenshots_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Delete all screenshots (${storageStats.count})",
                                tint = textSecondary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = bgColor
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // --- Search Field (Apple Capsule Styling) ---
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppleSpacing.md, vertical = AppleSpacing.xs)
                    .testTag("review_search_input"),
                placeholder = {
                    Text(
                        "Search note, payee, amount, reference…",
                        style = AppleTypography.Body.copy(color = textSecondary)
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear Search",
                                tint = textSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(AppleRadius.chip),
                textStyle = AppleTypography.Body.copy(color = textPrimary),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = textPrimary,
                    unfocusedTextColor = textPrimary,
                    focusedContainerColor = cardSurface,
                    unfocusedContainerColor = cardSurface,
                    focusedBorderColor = accentColor,
                    unfocusedBorderColor = if (darkTheme) AppleDarkCardBorder else AppleCardBorder
                )
            )

            // --- Filter Segment Chips ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppleSpacing.md, vertical = AppleSpacing.xs),
                horizontalArrangement = Arrangement.spacedBy(AppleSpacing.xs)
            ) {
                val chipSelectedBg = if (darkTheme) AppleDarkAccent else AppleAccent
                val chipSelectedFg = Color.White
                val chipUnselectedBg = if (darkTheme) AppleDarkSurfaceElevated else AppleSurface
                val chipUnselectedFg = textSecondary

                val chipColors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = chipSelectedBg,
                    selectedLabelColor = chipSelectedFg,
                    containerColor = chipUnselectedBg,
                    labelColor = chipUnselectedFg
                )
                val chipBorder = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = false,
                    borderColor = if (darkTheme) AppleDarkCardBorder else AppleCardBorder
                )

                FilterChip(
                    selected = activeFilter == ReviewFilter.ALL,
                    onClick = { viewModel.setFilter(ReviewFilter.ALL) },
                    label = { Text("All", style = AppleTypography.CaptionEmphasized) },
                    colors = chipColors,
                    border = chipBorder,
                    shape = RoundedCornerShape(AppleRadius.chip),
                    modifier = Modifier.testTag("filter_all_chip")
                )
                FilterChip(
                    selected = activeFilter == ReviewFilter.UNREVIEWED_ONLY,
                    onClick = { viewModel.setFilter(ReviewFilter.UNREVIEWED_ONLY) },
                    label = { Text("Needs Review", style = AppleTypography.CaptionEmphasized) },
                    colors = chipColors,
                    border = chipBorder,
                    shape = RoundedCornerShape(AppleRadius.chip),
                    modifier = Modifier.testTag("filter_unreviewed_chip")
                )
                FilterChip(
                    selected = activeFilter == ReviewFilter.MERGED_ONLY,
                    onClick = { viewModel.setFilter(ReviewFilter.MERGED_ONLY) },
                    label = { Text("Merged", style = AppleTypography.CaptionEmphasized) },
                    colors = chipColors,
                    border = chipBorder,
                    shape = RoundedCornerShape(AppleRadius.chip),
                    modifier = Modifier.testTag("filter_merged_chip")
                )
                FilterChip(
                    selected = activeFilter == ReviewFilter.UNPAIRED_ONLY,
                    onClick = { viewModel.setFilter(ReviewFilter.UNPAIRED_ONLY) },
                    label = { Text("Unpaired", style = AppleTypography.CaptionEmphasized) },
                    colors = chipColors,
                    border = chipBorder,
                    shape = RoundedCornerShape(AppleRadius.chip),
                    modifier = Modifier.testTag("filter_unpaired_chip")
                )
            }

            // --- Entries List with Physical Motion & Translucent Cards ---
            if (transactions.isEmpty()) {
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
                        Surface(
                            shape = CircleShape,
                            color = if (darkTheme) AppleDarkSurfaceElevated else AppleSurface,
                            border = BorderStroke(1.dp, if (darkTheme) AppleDarkCardBorder else AppleCardBorder),
                            modifier = Modifier.size(72.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    modifier = Modifier.size(32.dp),
                                    tint = textSecondary
                                )
                            }
                        }
                        Text(
                            text = if (searchQuery.isNotEmpty()) "No Matching Entries" else "No Transactions Yet",
                            style = AppleTypography.Title.copy(color = textPrimary)
                        )
                        Text(
                            text = if (searchQuery.isNotEmpty()) {
                                "Check spelling or clear the search query to see all recorded entries."
                            } else {
                                "Transactions captured from your UPI app will appear here for review."
                            },
                            style = AppleTypography.Body.copy(color = textSecondary),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(horizontal = AppleSpacing.md)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = AppleSpacing.md),
                    verticalArrangement = Arrangement.spacedBy(AppleSpacing.sm)
                ) {
                    items(transactions, key = { it.id }) { entry ->
                        SwipeableAppleTransactionCard(
                            entry = entry,
                            darkTheme = darkTheme,
                            reduceMotion = reduceMotion,
                            onSave = { updated -> viewModel.updateEntry(updated) },
                            onToggleReviewed = { viewModel.toggleReviewed(entry) },
                            onDelete = { entryToDelete = entry },
                            onManualMerge = { entryForManualMerge = entry },
                            onViewImage = { path, title ->
                                selectedImageForViewer = Triple(entry, path, title)
                            },
                            onDeleteScreenshotA = {
                                screenshotToDelete = Pair(entry, false)
                            },
                            onDeleteScreenshotB = {
                                screenshotToDelete = Pair(entry, true)
                            }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(AppleSpacing.xl))
                    }
                }
            }
        }
    }

    // Full Screen Image Viewer
    selectedImageForViewer?.let { (entry, path, title) ->
        ImageViewerDialog(
            filePath = path,
            title = title,
            onDismiss = { selectedImageForViewer = null },
            onDelete = {
                viewModel.deleteScreenshotByPath(entry, path)
                selectedImageForViewer = null
            }
        )
    }

    // Delete All Screenshots Confirmation Dialog
    if (showDeleteAllScreenshotsConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteAllScreenshotsConfirm = false },
            shape = RoundedCornerShape(AppleRadius.sheet),
            title = {
                Text(
                    "Delete All Screenshots?",
                    style = AppleTypography.Title
                )
            },
            text = {
                Text(
                    "This will delete ${storageStats.count} screenshot files (${storageStats.formattedSize}) from device storage.\n\nAll recorded notes, amounts, and parsed transaction details remain preserved safely in your database.",
                    style = AppleTypography.Body,
                    color = textSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteAllScreenshotsConfirm = false
                        viewModel.deleteAllScreenshots()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AppleSwipeDelete),
                    shape = RoundedCornerShape(AppleRadius.chip),
                    modifier = Modifier.testTag("confirm_delete_all_screenshots_btn")
                ) {
                    Text("Delete All Files", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllScreenshotsConfirm = false }) {
                    Text("Cancel", color = accentColor)
                }
            }
        )
    }

    // Delete Single Screenshot Confirmation Dialog
    screenshotToDelete?.let { (entry, isScreenB) ->
        val screenLabel = if (isScreenB) "Screen B (Details)" else "Screen A (Receipt)"
        AlertDialog(
            onDismissRequest = { screenshotToDelete = null },
            shape = RoundedCornerShape(AppleRadius.sheet),
            title = {
                Text(
                    "Delete $screenLabel Screenshot?",
                    style = AppleTypography.Title
                )
            },
            text = {
                Text(
                    "Are you sure you want to delete this screenshot from device storage? The extracted note and transaction details will remain saved.",
                    style = AppleTypography.Body,
                    color = textSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (isScreenB) {
                            viewModel.deleteScreenshotB(entry)
                        } else {
                            viewModel.deleteScreenshotA(entry)
                        }
                        screenshotToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AppleSwipeDelete),
                    shape = RoundedCornerShape(AppleRadius.chip),
                    modifier = Modifier.testTag("confirm_delete_single_screenshot_btn")
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { screenshotToDelete = null }) {
                    Text("Cancel", color = accentColor)
                }
            }
        )
    }

    // Manual Merge Dialog
    entryForManualMerge?.let { primaryEntry ->
        ManualMergeDialog(
            primaryEntry = primaryEntry,
            onLoadCandidates = { excludeId -> viewModel.getUnmergedEntries(excludeId) },
            onMerge = { primaryId, secondaryId ->
                viewModel.manualMerge(primaryId, secondaryId)
            },
            onDismiss = { entryForManualMerge = null }
        )
    }

    // Delete Confirmation Dialog
    entryToDelete?.let { entry ->
        AlertDialog(
            onDismissRequest = { entryToDelete = null },
            shape = RoundedCornerShape(AppleRadius.sheet),
            title = {
                Text(
                    "Delete Transaction Entry?",
                    style = AppleTypography.Title
                )
            },
            text = {
                Text(
                    "Are you sure you want to delete this entry? Saved screenshot files for this capture will also be removed from storage.",
                    style = AppleTypography.Body,
                    color = textSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteEntry(entry)
                        entryToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = AppleSwipeDelete),
                    modifier = Modifier.testTag("confirm_delete_button")
                ) {
                    Text("Delete", fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { entryToDelete = null }) {
                    Text("Cancel", color = accentColor)
                }
            }
        )
    }
}

/**
 * Apple-style Swipeable Container with Physical Spring Physics.
 * Swipe Right -> Toggle Reviewed (with spring snap & action reveal).
 * Swipe Left -> Delete Action.
 * Supports interruption and rubber-banding.
 */
@Composable
fun SwipeableAppleTransactionCard(
    entry: TransactionEntry,
    darkTheme: Boolean,
    reduceMotion: Boolean,
    onSave: (TransactionEntry) -> Unit,
    onToggleReviewed: () -> Unit,
    onDelete: () -> Unit,
    onManualMerge: () -> Unit,
    onViewImage: (filePath: String, title: String) -> Unit,
    onDeleteScreenshotA: (() -> Unit)? = null,
    onDeleteScreenshotB: (() -> Unit)? = null
) {
    val coroutineScope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }
    val density = LocalDensity.current
    val commitThresholdPx = with(density) { 96.dp.toPx() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AppleRadius.card))
    ) {
        // --- Swipe Action Background Behind Card ---
        val currentOffset = offsetX.value
        if (currentOffset > 0) {
            // Right swipe reveal: Toggle Reviewed
            val reviewBg = if (entry.isReviewed) AppleSwipeUnreview else AppleSwipeReview
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(reviewBg),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = AppleSpacing.lg)
                ) {
                    Icon(
                        imageVector = if (entry.isReviewed) Icons.Default.Undo else Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(AppleSpacing.xs))
                    Text(
                        text = if (entry.isReviewed) "Mark Needs Review" else "Mark Reviewed",
                        style = AppleTypography.CaptionEmphasized.copy(color = Color.White)
                    )
                }
            }
        } else if (currentOffset < 0) {
            // Left swipe reveal: Delete
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(AppleSwipeDelete),
                contentAlignment = Alignment.CenterEnd
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(end = AppleSpacing.lg)
                ) {
                    Text(
                        text = "Delete",
                        style = AppleTypography.CaptionEmphasized.copy(color = Color.White)
                    )
                    Spacer(modifier = Modifier.width(AppleSpacing.xs))
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // --- Foreground Card with Physical Spring & Direct Gestures ---
        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .pointerInput(entry.id) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            val dragVal = offsetX.value
                            coroutineScope.launch {
                                if (dragVal > commitThresholdPx) {
                                    // Trigger Review toggle with physical bounce
                                    onToggleReviewed()
                                } else if (dragVal < -commitThresholdPx) {
                                    // Trigger Delete with physical bounce
                                    onDelete()
                                }
                                // Spring back naturally to resting position
                                offsetX.animateTo(
                                    targetValue = 0f,
                                    animationSpec = AppleMotion.swipeSpring(reduceMotion)
                                )
                            }
                        },
                        onDragCancel = {
                            coroutineScope.launch {
                                offsetX.animateTo(
                                    targetValue = 0f,
                                    animationSpec = AppleMotion.swipeSpring(reduceMotion)
                                )
                            }
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            coroutineScope.launch {
                                // Apply slight rubber band resistance if dragging beyond threshold
                                val current = offsetX.value
                                val resistance = if (abs(current) > commitThresholdPx) 0.45f else 0.85f
                                offsetX.snapTo(current + dragAmount * resistance)
                            }
                        }
                    )
                }
        ) {
            AppleTransactionCardContent(
                entry = entry,
                darkTheme = darkTheme,
                reduceMotion = reduceMotion,
                onSave = onSave,
                onToggleReviewed = onToggleReviewed,
                onDelete = onDelete,
                onManualMerge = onManualMerge,
                onViewImage = onViewImage,
                onDeleteScreenshotA = onDeleteScreenshotA,
                onDeleteScreenshotB = onDeleteScreenshotB
            )
        }
    }
}

/**
 * Apple HIG Transaction Card Content:
 * - Translucent glass/layered surface.
 * - Note and Amount as Hero fields.
 * - Reference numbers, VPA, and IDs recede visually in tabular/monospace styling.
 * - Quiet, muted merge badge.
 * - Collapsible raw OCR with spring expansion.
 * - In-place direct field editing.
 */
@Composable
fun AppleTransactionCardContent(
    entry: TransactionEntry,
    darkTheme: Boolean,
    reduceMotion: Boolean,
    onSave: (TransactionEntry) -> Unit,
    onToggleReviewed: () -> Unit,
    onDelete: () -> Unit,
    onManualMerge: () -> Unit,
    onViewImage: (filePath: String, title: String) -> Unit,
    onDeleteScreenshotA: (() -> Unit)?,
    onDeleteScreenshotB: (() -> Unit)?
) {
    val context = LocalContext.current

    // In-place editable state
    var note by remember(entry.id, entry.note) { mutableStateOf(entry.note) }
    var amount by remember(entry.id, entry.amount) { mutableStateOf(entry.amount) }
    var date by remember(entry.id, entry.date) { mutableStateOf(entry.date) }
    var payee by remember(entry.id, entry.payee) { mutableStateOf(entry.payee) }
    var vpa by remember(entry.id, entry.vpa) { mutableStateOf(entry.vpa) }
    var ref by remember(entry.id, entry.referenceNumber) { mutableStateOf(entry.referenceNumber) }
    var paymentMethod by remember(entry.id, entry.paymentMethod) { mutableStateOf(entry.paymentMethod) }
    var smtxId by remember(entry.id, entry.superMoneyTransactionId) { mutableStateOf(entry.superMoneyTransactionId) }

    var isRawOcrExpanded by remember { mutableStateOf(false) }

    val hasUnsavedChanges = note != entry.note ||
            amount != entry.amount ||
            date != entry.date ||
            payee != entry.payee ||
            vpa != entry.vpa ||
            ref != entry.referenceNumber ||
            paymentMethod != entry.paymentMethod ||
            smtxId != entry.superMoneyTransactionId

    val cardBg = if (darkTheme) AppleDarkSurfaceTranslucent else AppleSurfaceTranslucent
    val cardBorder = if (darkTheme) AppleDarkCardBorder else AppleCardBorder
    val textPrimary = if (darkTheme) AppleDarkTextPrimary else AppleTextPrimary
    val textSecondary = if (darkTheme) AppleDarkTextSecondary else AppleTextSecondary
    val textTertiary = if (darkTheme) AppleDarkTextTertiary else AppleTextTertiary
    val accentColor = if (darkTheme) AppleDarkAccent else AppleAccent

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("transaction_card_${entry.id}"),
        shape = RoundedCornerShape(AppleRadius.card),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(1.dp, cardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppleSpacing.md)
        ) {
            // --- Header Row: Quiet Status Badge + Reviewed Toggle ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Quiet, Muted Informational Badge
                val (badgeBg, badgeFg, badgeText) = when {
                    entry.isMerged -> Triple(AppleStatusMergedBg, AppleStatusMergedFg, "Merged (A + B)")
                    entry.sourceScreenType == "SCREEN_B" -> Triple(AppleStatusScreenBBg, AppleStatusScreenBFg, "Screen B Only")
                    else -> Triple(AppleStatusScreenABg, AppleStatusScreenAFg, "Screen A Only")
                }

                Surface(
                    shape = RoundedCornerShape(AppleRadius.chip),
                    color = badgeBg,
                    modifier = Modifier.padding(vertical = AppleSpacing.xxs)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = AppleSpacing.xs, vertical = AppleSpacing.xxs)
                    ) {
                        if (entry.isMerged) {
                            Icon(
                                imageVector = Icons.Default.CallMerge,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = badgeFg
                            )
                            Spacer(modifier = Modifier.width(AppleSpacing.xxs))
                        }
                        Text(
                            text = badgeText,
                            style = AppleTypography.CaptionEmphasized.copy(color = badgeFg)
                        )
                    }
                }

                // Reviewed Status Pill (Interactive Checkbox & Tap Target)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onToggleReviewed() }
                ) {
                    Text(
                        text = if (entry.isReviewed) "Reviewed" else "Needs Review",
                        style = AppleTypography.CaptionEmphasized.copy(
                            color = if (entry.isReviewed) AppleSwipeReview else textSecondary
                        )
                    )
                    Checkbox(
                        checked = entry.isReviewed,
                        onCheckedChange = { onToggleReviewed() },
                        colors = CheckboxDefaults.colors(
                            checkedColor = AppleSwipeReview,
                            uncheckedColor = textTertiary
                        ),
                        modifier = Modifier.testTag("reviewed_checkbox_${entry.id}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(AppleSpacing.xs))

            // --- HERO SECTION: Amount & Note Direct In-Place Editing ---
            // Amount Hero Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount", style = AppleTypography.Caption) },
                    textStyle = AppleTypography.NumericHero.copy(color = textPrimary),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("amount_input_${entry.id}"),
                    shape = RoundedCornerShape(AppleRadius.chip),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = textPrimary,
                        unfocusedTextColor = textPrimary,
                        focusedContainerColor = if (darkTheme) AppleDarkSurfaceElevated else AppleSurface,
                        unfocusedContainerColor = Color.Transparent,
                        focusedBorderColor = accentColor,
                        unfocusedBorderColor = Color.Transparent
                    )
                )

                // Date in-place
                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("Date & Time", style = AppleTypography.Caption) },
                    textStyle = AppleTypography.CaptionEmphasized.copy(color = textSecondary),
                    modifier = Modifier
                        .weight(1.2f)
                        .padding(start = AppleSpacing.xs)
                        .testTag("date_input_${entry.id}"),
                    shape = RoundedCornerShape(AppleRadius.chip),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = textPrimary,
                        unfocusedTextColor = textSecondary,
                        focusedContainerColor = if (darkTheme) AppleDarkSurfaceElevated else AppleSurface,
                        unfocusedContainerColor = Color.Transparent,
                        focusedBorderColor = accentColor,
                        unfocusedBorderColor = Color.Transparent
                    )
                )
            }

            Spacer(modifier = Modifier.height(AppleSpacing.xs))

            // Note Hero Field (Prominent typography)
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = {
                    Text(
                        "Note / Remark",
                        style = AppleTypography.CaptionEmphasized.copy(color = accentColor)
                    )
                },
                textStyle = AppleTypography.Subtitle.copy(color = textPrimary),
                placeholder = {
                    Text(
                        "Add a note for this transaction…",
                        style = AppleTypography.Body.copy(color = textTertiary)
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("note_input_${entry.id}"),
                shape = RoundedCornerShape(AppleRadius.chip),
                minLines = 2,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = textPrimary,
                    unfocusedTextColor = textPrimary,
                    focusedContainerColor = if (darkTheme) AppleDarkSurfaceElevated else AppleSurface,
                    unfocusedContainerColor = if (darkTheme) AppleDarkSurface else AppleSurface.copy(alpha = 0.6f),
                    focusedBorderColor = accentColor,
                    unfocusedBorderColor = if (darkTheme) AppleDarkCardBorder else AppleCardBorder
                )
            )

            Spacer(modifier = Modifier.height(AppleSpacing.sm))

            // --- Secondary Metadata Section: Payee, VPA, Ref Number, Payment Method ---
            // Payee & VPA Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppleSpacing.xs)
            ) {
                OutlinedTextField(
                    value = payee,
                    onValueChange = { payee = it },
                    label = { Text("Payee", style = AppleTypography.Caption) },
                    textStyle = AppleTypography.Body.copy(color = textPrimary),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("payee_input_${entry.id}"),
                    shape = RoundedCornerShape(AppleRadius.chip),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = textPrimary,
                        unfocusedTextColor = textPrimary,
                        focusedContainerColor = if (darkTheme) AppleDarkSurfaceElevated else AppleSurface,
                        unfocusedContainerColor = Color.Transparent,
                        focusedBorderColor = accentColor,
                        unfocusedBorderColor = cardBorder
                    )
                )

                OutlinedTextField(
                    value = vpa,
                    onValueChange = { vpa = it },
                    label = { Text("UPI VPA", style = AppleTypography.Caption) },
                    textStyle = AppleTypography.NumericSmall.copy(color = textSecondary),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("vpa_input_${entry.id}"),
                    shape = RoundedCornerShape(AppleRadius.chip),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = textPrimary,
                        unfocusedTextColor = textSecondary,
                        focusedContainerColor = if (darkTheme) AppleDarkSurfaceElevated else AppleSurface,
                        unfocusedContainerColor = Color.Transparent,
                        focusedBorderColor = accentColor,
                        unfocusedBorderColor = cardBorder
                    )
                )
            }

            Spacer(modifier = Modifier.height(AppleSpacing.xs))

            // Reference Number & Payment Method Row (Recedes visually as tabular figures)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppleSpacing.xs)
            ) {
                OutlinedTextField(
                    value = ref,
                    onValueChange = { ref = it },
                    label = { Text("UPI Ref / ID", style = AppleTypography.Caption) },
                    textStyle = AppleTypography.NumericSmall.copy(color = textTertiary),
                    modifier = Modifier
                        .weight(1.2f)
                        .testTag("ref_input_${entry.id}"),
                    shape = RoundedCornerShape(AppleRadius.chip),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = textPrimary,
                        unfocusedTextColor = textTertiary,
                        focusedContainerColor = if (darkTheme) AppleDarkSurfaceElevated else AppleSurface,
                        unfocusedContainerColor = Color.Transparent,
                        focusedBorderColor = accentColor,
                        unfocusedBorderColor = cardBorder
                    )
                )

                OutlinedTextField(
                    value = paymentMethod,
                    onValueChange = { paymentMethod = it },
                    label = { Text("Method", style = AppleTypography.Caption) },
                    textStyle = AppleTypography.Caption.copy(color = textSecondary),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("payment_method_input_${entry.id}"),
                    shape = RoundedCornerShape(AppleRadius.chip),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = textPrimary,
                        unfocusedTextColor = textSecondary,
                        focusedContainerColor = if (darkTheme) AppleDarkSurfaceElevated else AppleSurface,
                        unfocusedContainerColor = Color.Transparent,
                        focusedBorderColor = accentColor,
                        unfocusedBorderColor = cardBorder
                    )
                )
            }

            if (smtxId.isNotEmpty()) {
                Spacer(modifier = Modifier.height(AppleSpacing.xs))
                OutlinedTextField(
                    value = smtxId,
                    onValueChange = { smtxId = it },
                    label = { Text("Super.Money ID", style = AppleTypography.Caption) },
                    textStyle = AppleTypography.NumericSmall.copy(color = textTertiary),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(AppleRadius.chip),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = textPrimary,
                        unfocusedTextColor = textTertiary,
                        focusedContainerColor = if (darkTheme) AppleDarkSurfaceElevated else AppleSurface,
                        unfocusedContainerColor = Color.Transparent,
                        focusedBorderColor = accentColor,
                        unfocusedBorderColor = cardBorder
                    )
                )
            }

            // Save Edits Floating Spring Action
            AnimatedVisibility(
                visible = hasUnsavedChanges,
                enter = fadeIn(animationSpec = AppleMotion.functionalSpring<Float>(reduceMotion)),
                exit = fadeOut(animationSpec = AppleMotion.functionalSpring<Float>(reduceMotion))
            ) {
                Column {
                    Spacer(modifier = Modifier.height(AppleSpacing.sm))
                    Button(
                        onClick = {
                            onSave(
                                entry.copy(
                                    note = note,
                                    amount = amount,
                                    date = date,
                                    payee = payee,
                                    vpa = vpa,
                                    referenceNumber = ref,
                                    paymentMethod = paymentMethod,
                                    superMoneyTransactionId = smtxId
                                )
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("save_edits_button_${entry.id}"),
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        shape = RoundedCornerShape(AppleRadius.chip)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(AppleSpacing.xs))
                        Text(
                            "Save Changes",
                            style = AppleTypography.BodyEmphasized.copy(color = Color.White)
                        )
                    }
                }
            }

            // --- Screenshot Thumbnails Row ---
            if (entry.screenshotAPath != null || entry.screenshotBPath != null) {
                Spacer(modifier = Modifier.height(AppleSpacing.sm))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppleSpacing.xs)
                ) {
                    entry.screenshotAPath?.let { path ->
                        AppleThumbnailBox(
                            filePath = path,
                            label = "Screen A",
                            darkTheme = darkTheme,
                            onClick = { onViewImage(path, "Screen A (Receipt)") },
                            onDelete = onDeleteScreenshotA,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    entry.screenshotBPath?.let { path ->
                        AppleThumbnailBox(
                            filePath = path,
                            label = "Screen B",
                            darkTheme = darkTheme,
                            onClick = { onViewImage(path, "Screen B (Details)") },
                            onDelete = onDeleteScreenshotB,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(AppleSpacing.sm))

            // --- Collapsed/Expandable Raw OCR Text (Physical Spring Animation) ---
            Surface(
                color = if (darkTheme) AppleDarkSurfaceElevated else AppleBackground,
                shape = RoundedCornerShape(AppleRadius.chip),
                border = BorderStroke(1.dp, if (darkTheme) AppleDarkCardBorder else AppleCardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(AppleRadius.chip))
                    .clickable { isRawOcrExpanded = !isRawOcrExpanded }
                    .animateContentSize(animationSpec = AppleMotion.standardSpring<IntSize>(reduceMotion))
            ) {
                Column(modifier = Modifier.padding(AppleSpacing.sm)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Raw OCR Text",
                            style = AppleTypography.CaptionEmphasized.copy(color = textSecondary)
                        )
                        Icon(
                            imageVector = if (isRawOcrExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (isRawOcrExpanded) "Collapse" else "Expand",
                            tint = textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    if (isRawOcrExpanded) {
                        Spacer(modifier = Modifier.height(AppleSpacing.xs))
                        Text(
                            text = entry.rawOcrText.ifBlank { "(No raw OCR text available)" },
                            style = AppleTypography.NumericSmall.copy(color = textTertiary),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(AppleSpacing.xs))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = {
                                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    cm.setPrimaryClip(ClipData.newPlainText("Raw OCR Text", entry.rawOcrText))
                                    Toast.makeText(context, "Copied text to clipboard", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = accentColor
                                )
                                Spacer(modifier = Modifier.width(AppleSpacing.xxs))
                                Text(
                                    "Copy",
                                    style = AppleTypography.CaptionEmphasized.copy(color = accentColor)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(AppleSpacing.sm))

            // --- Card Footer Actions: Manual Merge & Direct Delete ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!entry.isMerged) {
                    OutlinedButton(
                        onClick = onManualMerge,
                        shape = RoundedCornerShape(AppleRadius.chip),
                        border = BorderStroke(1.dp, if (darkTheme) AppleDarkCardBorder else AppleCardBorder),
                        modifier = Modifier.testTag("manual_merge_button_${entry.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CallMerge,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = accentColor
                        )
                        Spacer(modifier = Modifier.width(AppleSpacing.xxs))
                        Text(
                            "Manual Merge",
                            style = AppleTypography.CaptionEmphasized.copy(color = accentColor)
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.testTag("delete_entry_button_${entry.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Entry",
                        tint = textTertiary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/**
 * Apple-style Screenshot Thumbnail Box with subtle depth and clean label badge.
 */
@Composable
fun AppleThumbnailBox(
    filePath: String,
    label: String,
    darkTheme: Boolean,
    onClick: () -> Unit,
    onDelete: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val file = File(filePath)
    Surface(
        modifier = modifier
            .height(84.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(AppleRadius.chip),
        color = if (darkTheme) AppleDarkSurfaceElevated else AppleBackground,
        border = BorderStroke(1.dp, if (darkTheme) AppleDarkCardBorder else AppleCardBorder)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (file.exists()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(file)
                        .crossfade(true)
                        .build(),
                    contentDescription = label,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = null,
                        tint = AppleTextTertiary
                    )
                }
            }

            // Subtle Label Badge
            Surface(
                color = Color.Black.copy(alpha = 0.65f),
                shape = RoundedCornerShape(bottomEnd = AppleRadius.chip),
                modifier = Modifier.align(Alignment.TopStart)
            ) {
                Text(
                    text = label,
                    color = Color.White,
                    style = AppleTypography.CaptionEmphasized,
                    modifier = Modifier.padding(horizontal = AppleSpacing.xs, vertical = AppleSpacing.xxs)
                )
            }

            // Delete Thumbnail Action
            if (onDelete != null) {
                Surface(
                    color = Color.Black.copy(alpha = 0.65f),
                    shape = CircleShape,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(AppleSpacing.xxs)
                        .size(26.dp)
                        .clickable { onDelete() }
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete $label Screenshot",
                        tint = Color.White,
                        modifier = Modifier.padding(5.dp)
                    )
                }
            }
        }
    }
}
