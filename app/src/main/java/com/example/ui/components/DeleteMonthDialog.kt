package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.AppleRadius
import com.example.ui.theme.AppleSpacing
import com.example.ui.theme.AppleTypography
import com.example.ui.viewmodel.MainViewModel
import java.text.DateFormatSymbols
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DeleteMonthDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val calendar = remember { Calendar.getInstance() }
    val initialYear = remember { calendar.get(Calendar.YEAR) }
    val initialMonth = remember { calendar.get(Calendar.MONTH) }

    var selectedYear by remember { mutableIntStateOf(initialYear) }
    var selectedMonth by remember { mutableIntStateOf(initialMonth) }

    var matchingCount by remember { mutableStateOf<Int?>(null) }
    var isChecking by remember { mutableStateOf(false) }
    var showIrreversibleConfirmation by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }

    val monthNames = remember { DateFormatSymbols(Locale.US).months.take(12) }
    val shortMonthNames = remember { DateFormatSymbols(Locale.US).shortMonths.take(12) }

    // Re-check count whenever year or month changes
    LaunchedEffect(selectedYear, selectedMonth) {
        isChecking = true
        matchingCount = viewModel.getTransactionCountForMonth(selectedYear, selectedMonth)
        isChecking = false
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(AppleRadius.sheet),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(AppleSpacing.md)
                .testTag("delete_month_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(AppleSpacing.lg)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(AppleSpacing.xs)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Delete a Month's Data",
                                style = AppleTypography.Title.copy(color = MaterialTheme.colorScheme.onSurface)
                            )
                            Text(
                                text = "Maintenance purge based on capture time",
                                style = AppleTypography.Caption.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(AppleSpacing.md))

                Text(
                    text = "Select Year & Month",
                    style = AppleTypography.Subtitle.copy(color = MaterialTheme.colorScheme.onSurface)
                )
                Spacer(modifier = Modifier.height(AppleSpacing.xxs))
                Text(
                    text = "Transactions are matched reliably using the capture timestamp (createdAt), avoiding inconsistent OCR date strings.",
                    style = AppleTypography.Caption.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )

                Spacer(modifier = Modifier.height(AppleSpacing.md))

                // Year Selector
                Surface(
                    shape = RoundedCornerShape(AppleRadius.chip),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = AppleSpacing.sm, vertical = AppleSpacing.xxs),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { selectedYear-- },
                            enabled = selectedYear > 2020
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChevronLeft,
                                contentDescription = "Previous Year",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Text(
                            text = "$selectedYear",
                            style = AppleTypography.BodyEmphasized.copy(
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 18.sp
                            )
                        )

                        IconButton(
                            onClick = { selectedYear++ },
                            enabled = selectedYear < 2035
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "Next Year",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(AppleSpacing.sm))

                // 12 Months Grid
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppleSpacing.xs),
                    verticalArrangement = Arrangement.spacedBy(AppleSpacing.xs)
                ) {
                    shortMonthNames.forEachIndexed { index, monthName ->
                        val isSelected = selectedMonth == index
                        val chipBg = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        }
                        val chipFg = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }

                        Surface(
                            shape = RoundedCornerShape(AppleRadius.chip),
                            color = chipBg,
                            border = if (!isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null,
                            modifier = Modifier
                                .clip(RoundedCornerShape(AppleRadius.chip))
                                .clickable { selectedMonth = index }
                                .testTag("month_chip_$index")
                        ) {
                            Text(
                                text = monthName,
                                style = AppleTypography.CaptionEmphasized.copy(color = chipFg),
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(AppleSpacing.md))

                // Count Summary Banner
                val currentMonthName = monthNames.getOrElse(selectedMonth) { "Selected Month" }
                val count = matchingCount ?: 0

                Surface(
                    shape = RoundedCornerShape(AppleRadius.card),
                    color = when {
                        isChecking -> MaterialTheme.colorScheme.surfaceVariant
                        count > 0 -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    },
                    border = BorderStroke(
                        1.dp,
                        if (count > 0) MaterialTheme.colorScheme.error.copy(alpha = 0.3f) else MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(AppleSpacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(AppleSpacing.sm)
                    ) {
                        if (isChecking) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Checking records for $currentMonthName $selectedYear…",
                                style = AppleTypography.Caption.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        } else if (count > 0) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(22.dp)
                            )
                            Column {
                                Text(
                                    text = "Found $count ${if (count == 1) "entry" else "entries"} captured in $currentMonthName $selectedYear",
                                    style = AppleTypography.CaptionEmphasized.copy(color = MaterialTheme.colorScheme.error)
                                )
                                Text(
                                    text = "Permanently removes transactions & deletes screenshot files.",
                                    style = AppleTypography.Caption.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                            }
                        } else {
                            Text(
                                text = "No transactions found captured in $currentMonthName $selectedYear.",
                                style = AppleTypography.Caption.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(AppleSpacing.lg))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppleSpacing.sm)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(AppleRadius.chip),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Text(
                            text = "Cancel",
                            style = AppleTypography.CaptionEmphasized.copy(color = MaterialTheme.colorScheme.onSurface)
                        )
                    }

                    Button(
                        onClick = { showIrreversibleConfirmation = true },
                        enabled = count > 0 && !isChecking && !isDeleting,
                        modifier = Modifier
                            .weight(1.3f)
                            .testTag("delete_month_proceed_button"),
                        shape = RoundedCornerShape(AppleRadius.chip),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteForever,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(AppleSpacing.xs))
                        Text(
                            text = "Delete $count Records",
                            style = AppleTypography.CaptionEmphasized.copy(color = MaterialTheme.colorScheme.onError)
                        )
                    }
                }
            }
        }
    }

    // Explicit Confirmation Dialog stating exact count and irreversibility
    if (showIrreversibleConfirmation) {
        val currentMonthName = monthNames.getOrElse(selectedMonth) { "Selected Month" }
        val count = matchingCount ?: 0

        AlertDialog(
            onDismissRequest = { if (!isDeleting) showIrreversibleConfirmation = false },
            title = {
                Text(
                    text = "Permanently Delete $count Entries?",
                    style = AppleTypography.Title.copy(color = MaterialTheme.colorScheme.error)
                )
            },
            text = {
                Text(
                    text = "This action is permanent and cannot be undone.\n\nAll $count transaction records captured during $currentMonthName $selectedYear, along with their attached screenshot files, will be permanently removed from device storage.",
                    style = AppleTypography.Body.copy(color = MaterialTheme.colorScheme.onSurface)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        isDeleting = true
                        viewModel.deleteMonthData(selectedYear, selectedMonth) {
                            isDeleting = false
                            showIrreversibleConfirmation = false
                            onDismiss()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    enabled = !isDeleting,
                    modifier = Modifier.testTag("confirm_delete_month_button")
                ) {
                    if (isDeleting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onError
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Deleting…")
                    } else {
                        Text("Permanently Delete", color = MaterialTheme.colorScheme.onError)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showIrreversibleConfirmation = false },
                    enabled = !isDeleting
                ) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }
}
