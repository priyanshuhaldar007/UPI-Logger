package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.TransactionEntry

@Composable
fun ManualMergeDialog(
    primaryEntry: TransactionEntry,
    onLoadCandidates: suspend (Long) -> List<TransactionEntry>,
    onMerge: (primaryId: Long, secondaryId: Long) -> Unit,
    onDismiss: () -> Unit
) {
    var candidates by remember { mutableStateOf<List<TransactionEntry>>(emptyList()) }
    var selectedCandidateId by remember { mutableStateOf<Long?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(primaryEntry.id) {
        isLoading = true
        candidates = onLoadCandidates(primaryEntry.id)
        isLoading = false
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Manual Merge Transaction")
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Select another capture to merge with this entry. Fields will be combined and both screenshots retained:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (isLoading) {
                    Text("Finding unmerged entries...")
                } else if (candidates.isEmpty()) {
                    Text(
                        text = "No other unmerged entries found to pair with.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 350.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(candidates, key = { it.id }) { candidate ->
                            val isSelected = selectedCandidateId == candidate.id
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedCandidateId = candidate.id },
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    }
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { selectedCandidateId = candidate.id }
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        val displayTitle = when {
                                            candidate.note.isNotEmpty() -> "Note: ${candidate.note}"
                                            candidate.amount.isNotEmpty() -> "Amount: ${candidate.amount}"
                                            candidate.payee.isNotEmpty() -> "Payee: ${candidate.payee}"
                                            else -> "Capture #${candidate.id}"
                                        }
                                        Text(
                                            text = displayTitle,
                                            style = MaterialTheme.typography.titleSmall
                                        )
                                        if (candidate.referenceNumber.isNotEmpty()) {
                                            Text(
                                                text = "Ref: ${candidate.referenceNumber}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        if (candidate.date.isNotEmpty()) {
                                            Text(
                                                text = candidate.date,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    selectedCandidateId?.let { secondaryId ->
                        onMerge(primaryEntry.id, secondaryId)
                    }
                    onDismiss()
                },
                enabled = selectedCandidateId != null,
                modifier = Modifier.testTag("confirm_merge_button")
            ) {
                Text("Merge Entries")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_merge_button")
            ) {
                Text("Cancel")
            }
        }
    )
}
