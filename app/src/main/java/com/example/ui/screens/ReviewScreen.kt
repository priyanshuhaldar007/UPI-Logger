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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CallMerge
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.RestartAlt
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
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
import com.example.ui.components.AppVisualMode
import com.example.ui.components.ModeIcon
import com.example.ui.theme.AppleRadius
import com.example.ui.theme.AppleSpacing
import com.example.ui.theme.AppleStatusMergedBg
import com.example.ui.theme.AppleStatusMergedFg
import com.example.ui.theme.AppleStatusScreenABg
import com.example.ui.theme.AppleStatusScreenAFg
import com.example.ui.theme.AppleStatusScreenBBg
import com.example.ui.theme.AppleStatusScreenBFg
import com.example.ui.theme.AppleStatusWaitingBg
import com.example.ui.theme.AppleStatusWaitingFg
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

enum class ReviewPresentationMode {
    STACK,
    LIST
}

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

    var presentationMode by remember { mutableStateOf(ReviewPresentationMode.STACK) }
    var stackIndex by remember(activeFilter, searchQuery) { mutableIntStateOf(0) }

    var selectedImageForViewer by remember { mutableStateOf<Triple<TransactionEntry, String, String>?>(null) }
    var entryForManualMerge by remember { mutableStateOf<TransactionEntry?>(null) }
    var entryToDelete by remember { mutableStateOf<TransactionEntry?>(null) }
    var screenshotToDelete by remember { mutableStateOf<Pair<TransactionEntry, Boolean>?>(null) }
    var showDeleteAllScreenshotsConfirm by remember { mutableStateOf(false) }

    val bgColor = if (darkTheme) AppleDarkBackground else AppleBackground
    val cardSurface = if (darkTheme) AppleDarkSurfaceTranslucent else AppleSurfaceTranslucent
    val textPrimary = MaterialTheme.colorScheme.onSurface
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant
    val accentColor = if (darkTheme) AppleDarkAccent else AppleAccent

    Scaffold(
        containerColor = bgColor,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(AppleSpacing.sm)
                    ) {
                        ModeIcon(mode = AppVisualMode.REVIEW, size = 32.dp)
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
                    // Segmented Toggle for Catch-Up Card Stack vs List View
                    Surface(
                        shape = RoundedCornerShape(AppleRadius.chip),
                        color = if (darkTheme) AppleDarkSurfaceElevated else AppleSurface,
                        border = BorderStroke(1.dp, if (darkTheme) AppleDarkCardBorder else AppleCardBorder)
                    ) {
                        Row(modifier = Modifier.padding(2.dp)) {
                            // Stack (Catch Up)
                            Surface(
                                shape = RoundedCornerShape(AppleRadius.chip),
                                color = if (presentationMode == ReviewPresentationMode.STACK) accentColor else Color.Transparent,
                                modifier = Modifier
                                    .clickable { presentationMode = ReviewPresentationMode.STACK }
                                    .testTag("toggle_catchup_view")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Layers,
                                        contentDescription = null,
                                        tint = if (presentationMode == ReviewPresentationMode.STACK) Color.White else textSecondary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        "Catch Up",
                                        style = AppleTypography.CaptionEmphasized.copy(
                                            color = if (presentationMode == ReviewPresentationMode.STACK) Color.White else textSecondary,
                                            fontSize = 11.sp
                                        )
                                    )
                                }
                            }

                            // List View
                            Surface(
                                shape = RoundedCornerShape(AppleRadius.chip),
                                color = if (presentationMode == ReviewPresentationMode.LIST) accentColor else Color.Transparent,
                                modifier = Modifier
                                    .clickable { presentationMode = ReviewPresentationMode.LIST }
                                    .testTag("toggle_list_view")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FormatListBulleted,
                                        contentDescription = null,
                                        tint = if (presentationMode == ReviewPresentationMode.LIST) Color.White else textSecondary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        "List",
                                        style = AppleTypography.CaptionEmphasized.copy(
                                            color = if (presentationMode == ReviewPresentationMode.LIST) Color.White else textSecondary,
                                            fontSize = 11.sp
                                        )
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(AppleSpacing.xxs))

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
            // --- Search Field (Apple Capsule Styling with Theme-Aware Colors) ---
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
                        style = AppleTypography.Body.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear Search",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(AppleRadius.chip),
                textStyle = AppleTypography.Body.copy(color = MaterialTheme.colorScheme.onSurface),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
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
                    modifier = Modifier.testTag("filter_needs_review_chip")
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

            Spacer(modifier = Modifier.height(AppleSpacing.xs))

            if (presentationMode == ReviewPresentationMode.STACK) {
                // ==============================================================
                // CATCH UP CARD STACK MODE (Slack Catch Up Style Presentation)
                // ==============================================================
                val isCaughtUp = transactions.isEmpty() || stackIndex >= transactions.size

                if (isCaughtUp) {
                    // All Caught Up State
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(AppleSpacing.xl),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(AppleSpacing.md)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = AppleSwipeReview.copy(alpha = 0.15f),
                                modifier = Modifier.size(88.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = AppleSwipeReview,
                                        modifier = Modifier.size(48.dp)
                                    )
                                }
                            }

                            Text(
                                text = "All Caught Up!",
                                style = AppleTypography.LargeTitle.copy(color = textPrimary)
                            )

                            Text(
                                text = if (transactions.isEmpty()) {
                                    if (searchQuery.isNotEmpty()) {
                                        "No entries match your search query."
                                    } else {
                                        "No transactions recorded for this filter."
                                    }
                                } else {
                                    "You've completed review of all ${transactions.size} entries in this stack."
                                },
                                style = AppleTypography.Body.copy(color = textSecondary),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(horizontal = AppleSpacing.md)
                            )

                            Spacer(modifier = Modifier.height(AppleSpacing.xs))

                            Row(horizontalArrangement = Arrangement.spacedBy(AppleSpacing.sm)) {
                                if (transactions.isNotEmpty()) {
                                    OutlinedButton(
                                        onClick = { stackIndex = 0 },
                                        shape = RoundedCornerShape(AppleRadius.chip),
                                        border = BorderStroke(1.dp, if (darkTheme) AppleDarkCardBorder else AppleCardBorder)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.RestartAlt,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = accentColor
                                        )
                                        Spacer(modifier = Modifier.width(AppleSpacing.xxs))
                                        Text("Start Over", color = accentColor)
                                    }
                                }

                                Button(
                                    onClick = { presentationMode = ReviewPresentationMode.LIST },
                                    shape = RoundedCornerShape(AppleRadius.chip),
                                    colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FormatListBulleted,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(AppleSpacing.xxs))
                                    Text("Switch to List", color = Color.White)
                                }
                            }
                        }
                    }
                } else {
                    // Position Indicator & Progress Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = AppleSpacing.md, vertical = AppleSpacing.xxs),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${stackIndex + 1} of ${transactions.size}",
                            style = AppleTypography.CaptionEmphasized.copy(color = textSecondary)
                        )

                        LinearProgressIndicator(
                            progress = { (stackIndex + 1).toFloat() / transactions.size },
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = AppleSpacing.md)
                                .height(4.dp)
                                .clip(CircleShape),
                            color = accentColor,
                            trackColor = if (darkTheme) AppleDarkSurfaceElevated else AppleSurface
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            IconButton(
                                onClick = { if (stackIndex > 0) stackIndex-- },
                                enabled = stackIndex > 0,
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ChevronLeft,
                                    contentDescription = "Previous Card",
                                    tint = if (stackIndex > 0) textPrimary else textSecondary.copy(alpha = 0.3f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            IconButton(
                                onClick = { if (stackIndex < transactions.size - 1) stackIndex++ },
                                enabled = stackIndex < transactions.size - 1,
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = "Next Card",
                                    tint = if (stackIndex < transactions.size - 1) textPrimary else textSecondary.copy(alpha = 0.3f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    // Card Stack Container
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = AppleSpacing.md, vertical = AppleSpacing.xs),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        // Card 3 peek (if exists)
                        val thirdCard = transactions.getOrNull(stackIndex + 2)
                        if (thirdCard != null) {
                            PeekCard(
                                entry = thirdCard,
                                offsetY = (-24).dp,
                                scale = 0.88f,
                                alpha = 0.35f,
                                darkTheme = darkTheme
                            )
                        }

                        // Card 2 peek (if exists)
                        val nextCard = transactions.getOrNull(stackIndex + 1)
                        if (nextCard != null) {
                            PeekCard(
                                entry = nextCard,
                                offsetY = (-12).dp,
                                scale = 0.94f,
                                alpha = 0.65f,
                                darkTheme = darkTheme
                            )
                        }

                        // Card 1: Top Focused Card (Interactive)
                        val currentCard = transactions[stackIndex]
                        key(currentCard.id) {
                            CatchUpTopCard(
                                entry = currentCard,
                                darkTheme = darkTheme,
                                reduceMotion = reduceMotion,
                                onSave = { updated -> viewModel.updateEntry(updated) },
                                onToggleReviewed = {
                                    viewModel.toggleReviewed(currentCard)
                                    if (stackIndex < transactions.size - 1) {
                                        stackIndex++
                                    } else {
                                        stackIndex = transactions.size
                                    }
                                },
                                onDelete = {
                                    entryToDelete = currentCard
                                },
                                onManualMerge = { entryForManualMerge = currentCard },
                                onViewImage = { path, title ->
                                    selectedImageForViewer = Triple(currentCard, path, title)
                                },
                                onDeleteScreenshotA = {
                                    screenshotToDelete = Pair(currentCard, false)
                                },
                                onDeleteScreenshotB = {
                                    screenshotToDelete = Pair(currentCard, true)
                                }
                            )
                        }
                    }

                    // Quick Action Triage Bar for Catch Up (Tappable alternatives to swiping)
                    val currentCard = transactions[stackIndex]
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = AppleSpacing.md, vertical = AppleSpacing.xs),
                        shape = RoundedCornerShape(AppleRadius.chip),
                        color = if (darkTheme) AppleDarkSurfaceElevated else AppleSurface,
                        border = BorderStroke(1.dp, if (darkTheme) AppleDarkCardBorder else AppleCardBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = AppleSpacing.sm, vertical = AppleSpacing.xs),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Delete button (reveals delete)
                            TextButton(
                                onClick = { entryToDelete = currentCard },
                                colors = ButtonDefaults.textButtonColors(contentColor = AppleSwipeDelete)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(AppleSpacing.xxs))
                                Text("Delete (Swipe Left)", fontSize = 12.sp)
                            }

                            // Review toggle action
                            Button(
                                onClick = {
                                    viewModel.toggleReviewed(currentCard)
                                    if (stackIndex < transactions.size - 1) {
                                        stackIndex++
                                    } else {
                                        stackIndex = transactions.size
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (currentCard.isReviewed) AppleSwipeUnreview else AppleSwipeReview
                                ),
                                shape = RoundedCornerShape(AppleRadius.chip)
                            ) {
                                Icon(
                                    imageVector = if (currentCard.isReviewed) Icons.Default.Undo else Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(AppleSpacing.xxs))
                                Text(
                                    text = if (currentCard.isReviewed) "Needs Review" else "Reviewed (Swipe Right)",
                                    fontSize = 12.sp,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            } else {
                // ==============================================================
                // LIST VIEW MODE (Scrolling presentation for mass-scanning)
                // ==============================================================
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
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = AppleSwipeReview.copy(alpha = 0.5f)
                            )
                            Text(
                                text = "No Transactions Found",
                                style = AppleTypography.Subtitle.copy(color = textPrimary)
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
    }

    // Full Screen Image Viewer
    selectedImageForViewer?.let { (entry, path, title) ->
        val initialPage = if (entry.screenshotBPath != null && path == entry.screenshotBPath) 1 else 0
        ImageViewerDialog(
            screenshotAPath = entry.screenshotAPath,
            screenshotBPath = entry.screenshotBPath,
            initialPage = initialPage,
            title = title,
            onDismiss = { selectedImageForViewer = null },
            onDelete = { targetPath ->
                viewModel.deleteScreenshotByPath(entry, targetPath)
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
                    "This will delete the screenshot file for $screenLabel from device storage. The OCR-parsed data will be kept.",
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
                    shape = RoundedCornerShape(AppleRadius.chip)
                ) {
                    Text("Delete File", color = Color.White)
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
    entryForManualMerge?.let { entry ->
        ManualMergeDialog(
            primaryEntry = entry,
            onLoadCandidates = { id -> viewModel.getUnmergedEntries(id) },
            onMerge = { primaryId, secondaryId ->
                viewModel.manualMerge(primaryId, secondaryId)
                entryForManualMerge = null
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
                        if (stackIndex >= transactions.size - 1 && stackIndex > 0) {
                            stackIndex--
                        }
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
 * Visual representation of a card peeking from behind the focused top card.
 */
@Composable
private fun PeekCard(
    entry: TransactionEntry,
    offsetY: androidx.compose.ui.unit.Dp,
    scale: Float,
    alpha: Float,
    darkTheme: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .offset(y = offsetY)
            .scale(scale)
            .graphicsLayer { this.alpha = alpha },
        shape = RoundedCornerShape(AppleRadius.card),
        colors = CardDefaults.cardColors(
            containerColor = if (darkTheme) AppleDarkSurfaceElevated else AppleSurface
        ),
        border = BorderStroke(1.dp, if (darkTheme) AppleDarkCardBorder else AppleCardBorder)
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
                Text(
                    text = if (entry.amount.isNotEmpty()) "₹${entry.amount}" else "₹—",
                    style = AppleTypography.LargeTitle.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 20.sp
                    )
                )
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
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = entry.payee.ifEmpty { entry.vpa.ifEmpty { "Transaction" } },
                style = AppleTypography.CaptionEmphasized.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (entry.note.isNotEmpty()) {
                Text(
                    text = "\"${entry.note}\"",
                    style = AppleTypography.Caption.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * Top Card in Catch Up Stack with spring-fling gestures.
 */
@Composable
private fun CatchUpTopCard(
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
    val coroutineScope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }
    val density = LocalDensity.current
    val commitThresholdPx = with(density) { 96.dp.toPx() }
    val screenWidthPx = with(density) { 450.dp.toPx() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AppleRadius.card))
    ) {
        // Swipe action background behind card
        val currentOffset = offsetX.value
        if (currentOffset > 0) {
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

        // Foreground top card
        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .pointerInput(entry.id) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            val dragVal = offsetX.value
                            coroutineScope.launch {
                                if (dragVal > commitThresholdPx) {
                                    offsetX.animateTo(
                                        targetValue = screenWidthPx,
                                        animationSpec = AppleMotion.swipeSpring(reduceMotion)
                                    )
                                    onToggleReviewed()
                                } else if (dragVal < -commitThresholdPx) {
                                    offsetX.animateTo(
                                        targetValue = -screenWidthPx,
                                        animationSpec = AppleMotion.swipeSpring(reduceMotion)
                                    )
                                    onDelete()
                                } else {
                                    offsetX.animateTo(
                                        targetValue = 0f,
                                        animationSpec = AppleMotion.swipeSpring(reduceMotion)
                                    )
                                }
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
                                val current = offsetX.value
                                val resistance = if (abs(current) > commitThresholdPx) 0.6f else 0.85f
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
                onDeleteScreenshotB = onDeleteScreenshotB,
                isScrollableInCard = true
            )
        }
    }
}

/**
 * Apple-style Swipeable Container in List View with Spring Physics.
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
        // Background reveal
        val currentOffset = offsetX.value
        if (currentOffset > 0) {
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

        // Foreground Card
        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .pointerInput(entry.id) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            val dragVal = offsetX.value
                            coroutineScope.launch {
                                if (dragVal > commitThresholdPx) {
                                    onToggleReviewed()
                                } else if (dragVal < -commitThresholdPx) {
                                    onDelete()
                                }
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
                onDeleteScreenshotB = onDeleteScreenshotB,
                isScrollableInCard = false
            )
        }
    }
}

/**
 * Apple HIG Transaction Card Content with Swipeable Screenshot Pager
 * and Theme-Aware Text Contrast tokens.
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
    onDeleteScreenshotB: (() -> Unit)?,
    isScrollableInCard: Boolean = false
) {
    val context = LocalContext.current

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

    val cardBg = if (darkTheme) AppleDarkSurfaceElevated else AppleSurface
    val cardBorder = if (darkTheme) AppleDarkCardBorder else AppleCardBorder
    val textPrimary = MaterialTheme.colorScheme.onSurface
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant
    val accentColor = if (darkTheme) AppleDarkAccent else AppleAccent

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("transaction_card_${entry.id}"),
        shape = RoundedCornerShape(AppleRadius.card),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(1.dp, cardBorder)
    ) {
        val columnModifier = if (isScrollableInCard) {
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(AppleSpacing.md)
        } else {
            Modifier
                .fillMaxWidth()
                .padding(AppleSpacing.md)
        }

        Column(modifier = columnModifier) {
            // --- Header Row: Amount (Hero), Status Badges & Quick Reviewed Checkbox ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    // Hero Amount Field
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it },
                        label = { Text("Amount", style = AppleTypography.Caption) },
                        prefix = {
                            Text(
                                "₹ ",
                                style = AppleTypography.LargeTitle.copy(
                                    fontSize = 24.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                        },
                        textStyle = AppleTypography.LargeTitle.copy(
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("amount_input_${entry.id}"),
                        shape = RoundedCornerShape(AppleRadius.chip),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedContainerColor = if (darkTheme) AppleDarkSurface else AppleSurface,
                            unfocusedContainerColor = Color.Transparent,
                            focusedBorderColor = accentColor,
                            unfocusedBorderColor = cardBorder
                        )
                    )
                }

                Spacer(modifier = Modifier.width(AppleSpacing.sm))

                // Status Badges & Review Toggle Icon
                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(AppleRadius.badge),
                            color = if (entry.isMerged) AppleStatusMergedBg else AppleStatusWaitingBg
                        ) {
                            Text(
                                text = if (entry.isMerged) "MERGED" else "WAITING",
                                style = AppleTypography.CaptionEmphasized.copy(
                                    color = if (entry.isMerged) AppleStatusMergedFg else AppleStatusWaitingFg,
                                    fontSize = 10.sp
                                ),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(AppleSpacing.xs))

                        IconButton(
                            onClick = onToggleReviewed,
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("toggle_reviewed_button_${entry.id}")
                        ) {
                            Icon(
                                imageVector = if (entry.isReviewed) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                contentDescription = if (entry.isReviewed) "Mark unreviewed" else "Mark reviewed",
                                tint = if (entry.isReviewed) AppleSwipeReview else textSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = if (entry.isReviewed) "Verified" else "Needs Review",
                        style = AppleTypography.Caption.copy(
                            color = if (entry.isReviewed) AppleSwipeReview else textSecondary,
                            fontSize = 11.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(AppleSpacing.sm))

            // --- Hero Note Field ---
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = {
                    Text(
                        "Note / Remark",
                        style = AppleTypography.CaptionEmphasized.copy(color = accentColor)
                    )
                },
                textStyle = AppleTypography.Subtitle.copy(color = MaterialTheme.colorScheme.onSurface),
                placeholder = {
                    Text(
                        "Add a note for this transaction…",
                        style = AppleTypography.Body.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("note_input_${entry.id}"),
                shape = RoundedCornerShape(AppleRadius.chip),
                minLines = 2,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedContainerColor = if (darkTheme) AppleDarkSurface else AppleSurface,
                    unfocusedContainerColor = if (darkTheme) AppleDarkSurface else AppleSurface.copy(alpha = 0.6f),
                    focusedBorderColor = accentColor,
                    unfocusedBorderColor = if (darkTheme) AppleDarkCardBorder else AppleCardBorder
                )
            )

            Spacer(modifier = Modifier.height(AppleSpacing.sm))

            // --- Secondary Metadata Section: Payee, VPA, Ref Number, Payment Method ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppleSpacing.xs)
            ) {
                OutlinedTextField(
                    value = payee,
                    onValueChange = { payee = it },
                    label = { Text("Payee", style = AppleTypography.Caption) },
                    textStyle = AppleTypography.Body.copy(color = MaterialTheme.colorScheme.onSurface),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("payee_input_${entry.id}"),
                    shape = RoundedCornerShape(AppleRadius.chip),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedContainerColor = if (darkTheme) AppleDarkSurface else AppleSurface,
                        unfocusedContainerColor = Color.Transparent,
                        focusedBorderColor = accentColor,
                        unfocusedBorderColor = cardBorder
                    )
                )

                OutlinedTextField(
                    value = vpa,
                    onValueChange = { vpa = it },
                    label = { Text("UPI VPA", style = AppleTypography.Caption) },
                    textStyle = AppleTypography.NumericSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("vpa_input_${entry.id}"),
                    shape = RoundedCornerShape(AppleRadius.chip),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        focusedContainerColor = if (darkTheme) AppleDarkSurface else AppleSurface,
                        unfocusedContainerColor = Color.Transparent,
                        focusedBorderColor = accentColor,
                        unfocusedBorderColor = cardBorder
                    )
                )
            }

            Spacer(modifier = Modifier.height(AppleSpacing.xs))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppleSpacing.xs)
            ) {
                OutlinedTextField(
                    value = ref,
                    onValueChange = { ref = it },
                    label = { Text("UPI Ref / ID", style = AppleTypography.Caption) },
                    textStyle = AppleTypography.NumericSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                    modifier = Modifier
                        .weight(1.2f)
                        .testTag("ref_input_${entry.id}"),
                    shape = RoundedCornerShape(AppleRadius.chip),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        focusedContainerColor = if (darkTheme) AppleDarkSurface else AppleSurface,
                        unfocusedContainerColor = Color.Transparent,
                        focusedBorderColor = accentColor,
                        unfocusedBorderColor = cardBorder
                    )
                )

                OutlinedTextField(
                    value = paymentMethod,
                    onValueChange = { paymentMethod = it },
                    label = { Text("Method", style = AppleTypography.Caption) },
                    textStyle = AppleTypography.Caption.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("payment_method_input_${entry.id}"),
                    shape = RoundedCornerShape(AppleRadius.chip),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        focusedContainerColor = if (darkTheme) AppleDarkSurface else AppleSurface,
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
                    textStyle = AppleTypography.NumericSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(AppleRadius.chip),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        focusedContainerColor = if (darkTheme) AppleDarkSurface else AppleSurface,
                        unfocusedContainerColor = Color.Transparent,
                        focusedBorderColor = accentColor,
                        unfocusedBorderColor = cardBorder
                    )
                )
            }

            // Save Edits Floating Spring Action
            AnimatedVisibility(
                visible = hasUnsavedChanges,
                enter = fadeIn(animationSpec = AppleMotion.functionalSpring(reduceMotion)),
                exit = fadeOut(animationSpec = AppleMotion.functionalSpring(reduceMotion))
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

            // --- ITEM 2: SWIPEABLE SCREENSHOT PAGER WITHIN EACH CARD ---
            if (entry.screenshotAPath != null || entry.screenshotBPath != null) {
                Spacer(modifier = Modifier.height(AppleSpacing.sm))
                ScreenshotHorizontalPager(
                    entry = entry,
                    darkTheme = darkTheme,
                    onViewImage = onViewImage,
                    onDeleteScreenshotA = onDeleteScreenshotA,
                    onDeleteScreenshotB = onDeleteScreenshotB
                )
            }

            Spacer(modifier = Modifier.height(AppleSpacing.sm))

            // --- Collapsed/Expandable Raw OCR Text ---
            Surface(
                color = if (darkTheme) AppleDarkSurface else AppleBackground,
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
                            style = AppleTypography.CaptionEmphasized.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                        Icon(
                            imageVector = if (isRawOcrExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (isRawOcrExpanded) "Collapse" else "Expand",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    if (isRawOcrExpanded) {
                        Spacer(modifier = Modifier.height(AppleSpacing.xs))
                        Text(
                            text = entry.rawOcrText.ifBlank { "(No raw OCR text available)" },
                            style = AppleTypography.NumericSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
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
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

private data class ScreenshotPageItem(
    val title: String,
    val isScreenB: Boolean,
    val path: String,
    val onDelete: (() -> Unit)?
)

/**
 * ITEM 2: Swipeable screenshot pager within each card.
 * Bounded horizontal touch target ensures swiping on the screenshot navigates pages,
 * while swiping on the card body/form triggers card-level review/delete action.
 */
@Composable
fun ScreenshotHorizontalPager(
    entry: TransactionEntry,
    darkTheme: Boolean,
    onViewImage: (filePath: String, title: String) -> Unit,
    onDeleteScreenshotA: (() -> Unit)?,
    onDeleteScreenshotB: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val pages = remember(entry.screenshotAPath, entry.screenshotBPath) {
        buildList {
            if (entry.screenshotAPath != null) {
                add(
                    ScreenshotPageItem(
                        title = "Screen A: ₹ Amount",
                        isScreenB = false,
                        path = entry.screenshotAPath,
                        onDelete = onDeleteScreenshotA
                    )
                )
            }
            if (entry.screenshotBPath != null) {
                add(
                    ScreenshotPageItem(
                        title = "Screen B: Note",
                        isScreenB = true,
                        path = entry.screenshotBPath,
                        onDelete = onDeleteScreenshotB
                    )
                )
            }
        }
    }

    if (pages.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { pages.size })

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(AppleRadius.chip))
                .background(if (darkTheme) AppleDarkSurface else AppleBackground)
                .border(
                    BorderStroke(1.dp, if (darkTheme) AppleDarkCardBorder else AppleCardBorder),
                    RoundedCornerShape(AppleRadius.chip)
                )
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { pageIndex ->
                val page = pages[pageIndex]
                val file = File(page.path)

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { onViewImage(page.path, page.title) }
                ) {
                    if (file.exists()) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(file)
                                .crossfade(true)
                                .build(),
                            contentDescription = page.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Image,
                                    contentDescription = null,
                                    tint = AppleTextTertiary,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.height(AppleSpacing.xxs))
                                Text(
                                    text = "Screenshot unavailable",
                                    style = AppleTypography.Caption.copy(color = AppleTextTertiary)
                                )
                            }
                        }
                    }

                    // Top Left Screen Badge: "Screen A: ₹ Amount" vs "Screen B: Note"
                    Surface(
                        color = Color.Black.copy(alpha = 0.72f),
                        shape = RoundedCornerShape(AppleRadius.chip),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(AppleSpacing.xs)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = AppleSpacing.xs, vertical = AppleSpacing.xxs),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (page.isScreenB) AppleStatusScreenBFg else AppleStatusScreenAFg)
                            )
                            Spacer(modifier = Modifier.width(AppleSpacing.xxs))
                            Text(
                                text = page.title,
                                color = Color.White,
                                style = AppleTypography.CaptionEmphasized.copy(fontSize = 11.sp)
                            )
                        }
                    }

                    // Top Right Delete Action for Screenshot
                    if (page.onDelete != null) {
                        Surface(
                            color = Color.Black.copy(alpha = 0.65f),
                            shape = CircleShape,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(AppleSpacing.xs)
                                .size(28.dp)
                                .clickable { page.onDelete.invoke() }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete ${page.title}",
                                    tint = Color.White,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Dot indicators when multiple screenshots exist
            if (pages.size > 1) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = AppleSpacing.xs),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(pages.size) { index ->
                        val isSelected = pagerState.currentPage == index
                        val dotWidth = if (isSelected) 16.dp else 6.dp
                        val dotColor = if (isSelected) Color.White else Color.White.copy(alpha = 0.45f)
                        Box(
                            modifier = Modifier
                                .height(6.dp)
                                .width(dotWidth)
                                .clip(CircleShape)
                                .background(dotColor)
                        )
                    }
                }
            }
        }
    }
}
