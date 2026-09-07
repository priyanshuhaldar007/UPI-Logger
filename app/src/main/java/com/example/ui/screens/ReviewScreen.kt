package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CallMerge
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Note
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ElevatedCard
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.TransactionEntry
import com.example.ui.components.ImageViewerDialog
import com.example.ui.components.ManualMergeDialog
import com.example.ui.theme.PolishBackground
import com.example.ui.theme.PolishCardBorder
import com.example.ui.theme.PolishCardInnerBorder
import com.example.ui.theme.PolishMergedContainer
import com.example.ui.theme.PolishMergedText
import com.example.ui.theme.PolishPrimary
import com.example.ui.theme.PolishPrimaryContainer
import com.example.ui.theme.PolishPrimaryDark
import com.example.ui.theme.PolishSurface
import com.example.ui.theme.PolishTextMuted
import com.example.ui.theme.PolishTextPrimary
import com.example.ui.theme.PolishTextSecondary
import com.example.ui.theme.PolishWaitingContainer
import com.example.ui.theme.PolishWaitingText
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.ReviewFilter
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val transactions by viewModel.filteredTransactions.collectAsStateWithLifecycle()
    val activeFilter by viewModel.activeFilter.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val storageStats by viewModel.storageStats.collectAsStateWithLifecycle()

    var selectedImageForViewer by remember { mutableStateOf<Triple<TransactionEntry, String, String>?>(null) }
    var entryForManualMerge by remember { mutableStateOf<TransactionEntry?>(null) }
    var entryToDelete by remember { mutableStateOf<TransactionEntry?>(null) }
    var screenshotToDelete by remember { mutableStateOf<Pair<TransactionEntry, Boolean>?>(null) }
    var showDeleteAllScreenshotsConfirm by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = PolishBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Review & Edit Entries",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = PolishPrimaryDark
                        )
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("review_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Home",
                            tint = PolishPrimaryDark
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
                                tint = PolishPrimaryDark
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PolishBackground
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // --- Search Bar ---
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .testTag("review_search_input"),
                placeholder = { Text("Search by Note, Payee, Amount, Ref...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = PolishTextSecondary) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear Search", tint = PolishTextSecondary)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = PolishTextPrimary,
                    unfocusedTextColor = PolishTextPrimary,
                    focusedContainerColor = PolishSurface,
                    unfocusedContainerColor = PolishSurface,
                    focusedBorderColor = PolishPrimary,
                    unfocusedBorderColor = PolishCardBorder
                )
            )

            // --- Filter Chips ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val chipColors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = PolishPrimaryContainer,
                    selectedLabelColor = PolishPrimaryDark,
                    containerColor = PolishSurface,
                    labelColor = PolishTextSecondary
                )
                val chipBorder = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = false,
                    borderColor = PolishCardBorder
                )

                FilterChip(
                    selected = activeFilter == ReviewFilter.ALL,
                    onClick = { viewModel.setFilter(ReviewFilter.ALL) },
                    label = { Text("All") },
                    colors = chipColors,
                    border = chipBorder,
                    modifier = Modifier.testTag("filter_all_chip")
                )
                FilterChip(
                    selected = activeFilter == ReviewFilter.UNREVIEWED_ONLY,
                    onClick = { viewModel.setFilter(ReviewFilter.UNREVIEWED_ONLY) },
                    label = { Text("Needs Review") },
                    colors = chipColors,
                    border = chipBorder,
                    modifier = Modifier.testTag("filter_unreviewed_chip")
                )
                FilterChip(
                    selected = activeFilter == ReviewFilter.MERGED_ONLY,
                    onClick = { viewModel.setFilter(ReviewFilter.MERGED_ONLY) },
                    label = { Text("Merged") },
                    colors = chipColors,
                    border = chipBorder,
                    modifier = Modifier.testTag("filter_merged_chip")
                )
                FilterChip(
                    selected = activeFilter == ReviewFilter.UNPAIRED_ONLY,
                    onClick = { viewModel.setFilter(ReviewFilter.UNPAIRED_ONLY) },
                    label = { Text("Unpaired") },
                    colors = chipColors,
                    border = chipBorder,
                    modifier = Modifier.testTag("filter_unpaired_chip")
                )
            }

            // --- Entries List ---
            if (transactions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Note,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (searchQuery.isNotEmpty()) "No transactions matching \"$searchQuery\"" else "No captured transactions yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Start a capture session on the Home screen to capture screens from your UPI app.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(transactions, key = { it.id }) { entry ->
                        TransactionCard(
                            entry = entry,
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
                        Spacer(modifier = Modifier.height(24.dp))
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
            title = { Text("Delete All Screenshots?") },
            text = {
                Text("This will permanently delete ${storageStats.count} screenshot files (${storageStats.formattedSize}) from device storage to free up space.\n\nAll your recorded notes, transaction amounts, and details will remain safely saved in the database.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteAllScreenshotsConfirm = false
                        viewModel.deleteAllScreenshots()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_delete_all_screenshots_btn")
                ) {
                    Text("Delete All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllScreenshotsConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Single Screenshot Confirmation Dialog
    screenshotToDelete?.let { (entry, isScreenB) ->
        val screenLabel = if (isScreenB) "Screen B (Note Details)" else "Screen A (Payment Result)"
        AlertDialog(
            onDismissRequest = { screenshotToDelete = null },
            title = { Text("Delete $screenLabel Screenshot?") },
            text = {
                Text("Are you sure you want to delete this screenshot from device storage? The transaction data and note will remain saved.")
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
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_delete_single_screenshot_btn")
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { screenshotToDelete = null }) {
                    Text("Cancel")
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
            title = { Text("Delete Transaction Entry?") },
            text = {
                Text("Are you sure you want to delete this entry? Saved screenshots for this capture will also be deleted from device storage.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteEntry(entry)
                        entryToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.testTag("confirm_delete_button")
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { entryToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun TransactionCard(
    entry: TransactionEntry,
    onSave: (TransactionEntry) -> Unit,
    onToggleReviewed: () -> Unit,
    onDelete: () -> Unit,
    onManualMerge: () -> Unit,
    onViewImage: (filePath: String, title: String) -> Unit,
    onDeleteScreenshotA: (() -> Unit)? = null,
    onDeleteScreenshotB: (() -> Unit)? = null
) {
    val context = LocalContext.current

    // Local editable fields
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

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("transaction_card_${entry.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = PolishSurface
        ),
        border = BorderStroke(1.dp, PolishCardBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Status badge, Date, and Reviewed Checkbox
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val badgeTextColor = when {
                        entry.isMerged -> PolishMergedText
                        entry.sourceScreenType == "SCREEN_B" -> Color(0xFF5B4FA6)
                        else -> PolishPrimaryDark
                    }
                    val badgeBgColor = when {
                        entry.isMerged -> PolishMergedContainer
                        entry.sourceScreenType == "SCREEN_B" -> Color(0xFFEDE7F6)
                        else -> PolishPrimaryContainer
                    }
                    val badgeText = when {
                        entry.isMerged -> "Merged (Screen A + B)"
                        entry.sourceScreenType == "SCREEN_B" -> "Screen B Only"
                        else -> "Screen A Only"
                    }

                    Box(
                        modifier = Modifier
                            .background(badgeBgColor, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = badgeText,
                            color = badgeTextColor,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (entry.isReviewed) "Reviewed" else "Mark Reviewed",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (entry.isReviewed) FontWeight.SemiBold else FontWeight.Normal
                        ),
                        color = if (entry.isReviewed) PolishMergedText else PolishTextSecondary
                    )
                    Checkbox(
                        checked = entry.isReviewed,
                        onCheckedChange = { onToggleReviewed() },
                        colors = CheckboxDefaults.colors(
                            checkedColor = PolishPrimary
                        ),
                        modifier = Modifier.testTag("reviewed_checkbox_${entry.id}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // --- Screenshots Thumbnails Row ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Screen A Thumbnail
                entry.screenshotAPath?.let { path ->
                    ThumbnailBox(
                        filePath = path,
                        label = "Screen A",
                        onClick = { onViewImage(path, "Screen A (Transaction Result)") },
                        onDelete = onDeleteScreenshotA,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Screen B Thumbnail
                entry.screenshotBPath?.let { path ->
                    ThumbnailBox(
                        filePath = path,
                        label = "Screen B",
                        onClick = { onViewImage(path, "Screen B (Details Bottom Sheet)") },
                        onDelete = onDeleteScreenshotB,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // --- NOTE (HIGHEST PRIORITY FIELD) ---
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Note,
                            contentDescription = null,
                            tint = Color(0xFFD97706),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Note / Remark (Primary Field)", fontWeight = FontWeight.Bold)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("note_input_${entry.id}"),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = PolishTextPrimary,
                    unfocusedTextColor = PolishTextPrimary
                ),
                minLines = 2
            )

            Spacer(modifier = Modifier.height(8.dp))

            // --- Amount and Date Row ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("amount_input_${entry.id}"),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = PolishTextPrimary,
                        unfocusedTextColor = PolishTextPrimary
                    ),
                    singleLine = true
                )

                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("Date & Time") },
                    modifier = Modifier
                        .weight(1.4f)
                        .testTag("date_input_${entry.id}"),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = PolishTextPrimary,
                        unfocusedTextColor = PolishTextPrimary
                    ),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // --- Payee Name and VPA Row ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = payee,
                    onValueChange = { payee = it },
                    label = { Text("Payee Name") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("payee_input_${entry.id}"),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = PolishTextPrimary,
                        unfocusedTextColor = PolishTextPrimary
                    ),
                    singleLine = true
                )

                OutlinedTextField(
                    value = vpa,
                    onValueChange = { vpa = it },
                    label = { Text("VPA") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("vpa_input_${entry.id}"),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = PolishTextPrimary,
                        unfocusedTextColor = PolishTextPrimary
                    ),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // --- Reference Number and Payment Method Row ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = ref,
                    onValueChange = { ref = it },
                    label = { Text("UPI Ref / Txn ID") },
                    modifier = Modifier
                        .weight(1.2f)
                        .testTag("ref_input_${entry.id}"),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = PolishTextPrimary,
                        unfocusedTextColor = PolishTextPrimary
                    ),
                    singleLine = true
                )

                OutlinedTextField(
                    value = paymentMethod,
                    onValueChange = { paymentMethod = it },
                    label = { Text("Payment Method") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("payment_method_input_${entry.id}"),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = PolishTextPrimary,
                        unfocusedTextColor = PolishTextPrimary
                    ),
                    singleLine = true
                )
            }

            if (smtxId.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = smtxId,
                    onValueChange = { smtxId = it },
                    label = { Text("Super.Money Txn ID") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = PolishTextPrimary,
                        unfocusedTextColor = PolishTextPrimary
                    ),
                    singleLine = true
                )
            }

            // Save Edits Button if modified
            AnimatedVisibility(visible = hasUnsavedChanges) {
                Column {
                    Spacer(modifier = Modifier.height(10.dp))
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
                            .height(48.dp)
                            .testTag("save_edits_button_${entry.id}"),
                        colors = ButtonDefaults.buttonColors(containerColor = PolishPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save Edited Fields", fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // --- Collapsed/Expandable Raw OCR Text Section ---
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isRawOcrExpanded = !isRawOcrExpanded }
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Raw OCR Text Output",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Icon(
                            imageVector = if (isRawOcrExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (isRawOcrExpanded) "Collapse" else "Expand"
                        )
                    }

                    if (isRawOcrExpanded) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = entry.rawOcrText.ifBlank { "(No OCR text captured)" },
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = {
                                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    cm.setPrimaryClip(ClipData.newPlainText("Raw OCR Text", entry.rawOcrText))
                                    Toast.makeText(context, "Copied raw text to clipboard", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Copy Text")
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // --- Footer Actions: Manual Merge & Delete ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!entry.isMerged) {
                    OutlinedButton(
                        onClick = onManualMerge,
                        modifier = Modifier.testTag("manual_merge_button_${entry.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CallMerge,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Manual Merge")
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
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun ThumbnailBox(
    filePath: String,
    label: String,
    onClick: () -> Unit,
    onDelete: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val file = File(filePath)
    Card(
        modifier = modifier
            .height(90.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
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
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Overlay label badge
            Surface(
                color = Color.Black.copy(alpha = 0.65f),
                shape = RoundedCornerShape(bottomEnd = 6.dp),
                modifier = Modifier.align(Alignment.TopStart)
            ) {
                Text(
                    text = label,
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            // Delete screenshot button
            if (onDelete != null) {
                Surface(
                    color = Color.Black.copy(alpha = 0.65f),
                    shape = CircleShape,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
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
