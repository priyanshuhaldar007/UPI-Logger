package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.SuggestionChip
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.PolishBackground
import com.example.ui.theme.PolishCardBorder
import com.example.ui.theme.PolishPrimary
import com.example.ui.theme.PolishPrimaryBorder
import com.example.ui.theme.PolishPrimaryContainer
import com.example.ui.theme.PolishPrimaryDark
import com.example.ui.theme.PolishSurface
import com.example.ui.theme.PolishTextMuted
import com.example.ui.theme.PolishTextPrimary
import com.example.ui.theme.PolishTextSecondary
import com.example.ui.viewmodel.MainViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExportScreen(
    viewModel: MainViewModel
) {
    val context = LocalContext.current
    val allTransactions by viewModel.allTransactions.collectAsStateWithLifecycle()
    val totalCount by viewModel.totalCount.collectAsStateWithLifecycle()
    val storageStats by viewModel.storageStats.collectAsStateWithLifecycle()
    val reviewedCount = remember(allTransactions) { allTransactions.count { it.isReviewed } }

    var onlyReviewed by remember { mutableStateOf(false) }
    var isExporting by remember { mutableStateOf(false) }
    var showDeleteAllScreenshotsConfirm by remember { mutableStateOf(false) }

    val previewColumns = listOf(
        "Date", "Time", "Amount", "Payee", "VPA", "Note", "UPI Ref", "Payment Method", "super.money ID", "Status"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PolishBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Column {
            Text(
                text = "Export Transactions",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.5).sp,
                    color = PolishPrimaryDark
                )
            )
            Text(
                text = "Generate spreadsheets or share data locally",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = PolishTextSecondary
                )
            )
        }

        // Export Configuration Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = PolishSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, PolishCardBorder)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "CSV Export Configuration",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = PolishTextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Option 1: All Entries
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onlyReviewed = false }
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = !onlyReviewed,
                        onClick = { onlyReviewed = false },
                        colors = RadioButtonDefaults.colors(selectedColor = PolishPrimary)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Export All Transactions",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = PolishTextPrimary
                        )
                        Text(
                            text = "$totalCount total captured records",
                            style = MaterialTheme.typography.bodySmall,
                            color = PolishTextSecondary
                        )
                    }
                }

                // Option 2: Reviewed Only
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onlyReviewed = true }
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = onlyReviewed,
                        onClick = { onlyReviewed = true },
                        colors = RadioButtonDefaults.colors(selectedColor = PolishPrimary)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Export Reviewed Only",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = PolishTextPrimary
                        )
                        Text(
                            text = "$reviewedCount verified records",
                            style = MaterialTheme.typography.bodySmall,
                            color = PolishTextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Export Button
                Button(
                    onClick = {
                        if (allTransactions.isEmpty()) {
                            Toast.makeText(context, "No transactions to export", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        isExporting = true
                        viewModel.exportCsv(context, onlyReviewed) { result ->
                            isExporting = false
                            if (result.isSuccess) {
                                Toast.makeText(context, "CSV saved: ${result.rowCount} records exported", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "Export error: ${result.errorMessage}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("export_csv_action_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PolishPrimary),
                    enabled = !isExporting
                ) {
                    if (isExporting) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Exporting...")
                    } else {
                        Icon(imageVector = Icons.Default.FileDownload, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Export & Share CSV")
                    }
                }
            }
        }

        // Columns Breakdown Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = PolishSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, PolishCardBorder)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Exported Columns",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = PolishTextPrimary
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Standard RFC-4180 CSV with sanitized headers and quoted strings:",
                    style = MaterialTheme.typography.bodySmall,
                    color = PolishTextSecondary
                )

                Spacer(modifier = Modifier.height(12.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    previewColumns.forEach { col ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFF0F4F9))
                                .border(1.dp, PolishCardBorder, RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = col,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Medium,
                                    color = PolishTextPrimary
                                )
                            )
                        }
                    }
                }
            }
        }

        // Test Simulation Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = PolishPrimaryContainer),
            border = androidx.compose.foundation.BorderStroke(1.dp, PolishPrimaryBorder)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Science,
                        contentDescription = null,
                        tint = PolishPrimaryDark,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Sandbox Simulation",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = PolishPrimaryDark
                        )
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Generate a sample Screen A (Payment Result) and Screen B (Details Sheet with Note) to test the automated matching algorithm.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF003355)
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedButton(
                    onClick = { viewModel.insertSamplePair(context) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("generate_sample_pair_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PolishPrimaryDark),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, PolishPrimaryDark)
                ) {
                    Text("Generate Test Screen A + B Pair", fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // Screenshot Storage Cleanup Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = PolishSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, PolishCardBorder)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = null,
                        tint = if (storageStats.count > 0) Color(0xFFBA1A1A) else PolishTextMuted,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Screenshot Storage",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = PolishTextPrimary
                        )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Device storage used:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = PolishTextSecondary
                    )
                    Text(
                        text = "${storageStats.count} screenshots • ${storageStats.formattedSize}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = PolishTextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Free up internal storage space by removing raw screenshot images. All extracted notes, amounts, dates, and CSV records will remain intact.",
                    style = MaterialTheme.typography.bodySmall,
                    color = PolishTextSecondary
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedButton(
                    onClick = { showDeleteAllScreenshotsConfirm = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("delete_all_screenshots_export_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFBA1A1A)
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFBA1A1A)),
                    enabled = storageStats.count > 0
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (storageStats.count > 0) "Delete All Screenshots (${storageStats.count})" else "No Screenshots Stored",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Privacy Guarantee Card
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = PolishTextMuted,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "100% Local Storage • Zero Network Access • Privacy First",
                style = MaterialTheme.typography.labelSmall,
                color = PolishTextMuted
            )
        }

        Spacer(modifier = Modifier.height(60.dp))
    }

    if (showDeleteAllScreenshotsConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteAllScreenshotsConfirm = false },
            title = { Text("Delete All Screenshots?") },
            text = {
                Text("Are you sure you want to delete all ${storageStats.count} screenshot images (${storageStats.formattedSize})? This frees up space on your device. Your notes and transaction records will remain saved.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteAllScreenshotsConfirm = false
                        viewModel.deleteAllScreenshots()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_delete_all_export_btn")
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
}
