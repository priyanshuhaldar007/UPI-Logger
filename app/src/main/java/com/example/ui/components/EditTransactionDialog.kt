package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.CallMerge
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.TransactionEntry
import com.example.ui.theme.PolishCardBorder
import com.example.ui.theme.PolishMergedContainer
import com.example.ui.theme.PolishMergedText
import com.example.ui.theme.PolishPrimary
import com.example.ui.theme.PolishTextMuted
import com.example.ui.theme.PolishTextPrimary
import com.example.ui.theme.PolishTextSecondary
import com.example.ui.theme.PolishWaitingContainer
import com.example.ui.theme.PolishWaitingText
import java.io.File

@Composable
fun EditTransactionDialog(
    entry: TransactionEntry,
    onSave: (TransactionEntry) -> Unit,
    onDelete: (TransactionEntry) -> Unit,
    onManualMergeRequest: ((TransactionEntry) -> Unit)? = null,
    onViewImage: (filePath: String, title: String) -> Unit,
    onDeleteScreenshot: ((isScreenB: Boolean) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var note by remember(entry) { mutableStateOf(entry.note) }
    var payee by remember(entry) { mutableStateOf(entry.payee) }
    var vpa by remember(entry) { mutableStateOf(entry.vpa) }
    var amount by remember(entry) { mutableStateOf(entry.amount) }
    var referenceNumber by remember(entry) { mutableStateOf(entry.referenceNumber) }
    var paymentMethod by remember(entry) { mutableStateOf(entry.paymentMethod) }
    var superMoneyId by remember(entry) { mutableStateOf(entry.superMoneyTransactionId) }
    var isReviewed by remember(entry) { mutableStateOf(entry.isReviewed) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var screenshotToDeleteIsScreenB by remember { mutableStateOf<Boolean?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(24.dp))
                .border(1.dp, PolishCardBorder, RoundedCornerShape(24.dp))
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Transaction Details",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF001D36)
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            // Status Badge
                            if (entry.isMerged) {
                                Box(
                                    modifier = Modifier
                                        .background(PolishMergedContainer, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "MERGED",
                                        color = PolishMergedText,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .background(PolishCardBorder, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "WAITING",
                                        color = PolishTextSecondary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        Text(
                            text = if (entry.date.isNotEmpty()) entry.date else "Captured locally",
                            style = MaterialTheme.typography.bodySmall,
                            color = PolishTextMuted
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = PolishTextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Screenshots section
                Text(
                    text = "Attached Screen Captures",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = PolishTextSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Screen A
                    val fileA = entry.screenshotAPath?.let { File(it) }
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(110.dp)
                            .clickable(enabled = fileA?.exists() == true) {
                                entry.screenshotAPath?.let {
                                    onViewImage(it, "Screen A (Payment Result)")
                                }
                            },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F9FB)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, PolishCardBorder)
                    ) {
                        if (fileA?.exists() == true) {
                            Box(modifier = Modifier.fillMaxWidth()) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context).data(fileA).crossfade(true).build(),
                                    contentDescription = "Screen A",
                                    modifier = Modifier.fillMaxWidth().height(110.dp),
                                    contentScale = ContentScale.Crop
                                )
                                Text(
                                    text = "Screen A",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .background(Color.Black.copy(alpha = 0.6f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                                if (onDeleteScreenshot != null) {
                                    Surface(
                                        color = Color.Black.copy(alpha = 0.65f),
                                        shape = CircleShape,
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(4.dp)
                                            .size(26.dp)
                                            .clickable { screenshotToDeleteIsScreenB = false }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete Screen A Screenshot",
                                            tint = Color.White,
                                            modifier = Modifier.padding(5.dp)
                                        )
                                    }
                                }
                            }
                        } else {
                            Column(
                                modifier = Modifier.fillMaxWidth().height(110.dp),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.Image, contentDescription = null, tint = PolishTextMuted)
                                Text("No Screen A", style = MaterialTheme.typography.labelSmall, color = PolishTextMuted)
                            }
                        }
                    }

                    // Screen B
                    val fileB = entry.screenshotBPath?.let { File(it) }
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(110.dp)
                            .clickable(enabled = fileB?.exists() == true) {
                                entry.screenshotBPath?.let {
                                    onViewImage(it, "Screen B (Note Details)")
                                }
                            },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F9FB)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, PolishCardBorder)
                    ) {
                        if (fileB?.exists() == true) {
                            Box(modifier = Modifier.fillMaxWidth()) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context).data(fileB).crossfade(true).build(),
                                    contentDescription = "Screen B",
                                    modifier = Modifier.fillMaxWidth().height(110.dp),
                                    contentScale = ContentScale.Crop
                                )
                                Text(
                                    text = "Screen B (Note)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .background(Color.Black.copy(alpha = 0.6f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                                if (onDeleteScreenshot != null) {
                                    Surface(
                                        color = Color.Black.copy(alpha = 0.65f),
                                        shape = CircleShape,
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(4.dp)
                                            .size(26.dp)
                                            .clickable { screenshotToDeleteIsScreenB = true }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete Screen B Screenshot",
                                            tint = Color.White,
                                            modifier = Modifier.padding(5.dp)
                                        )
                                    }
                                }
                            }
                        } else {
                            Column(
                                modifier = Modifier.fillMaxWidth().height(110.dp),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.Image, contentDescription = null, tint = PolishTextMuted)
                                Text(
                                    text = if (entry.isMerged) "No Screen B" else "Missing Screen B",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (entry.isMerged) PolishTextMuted else PolishWaitingText
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Form Fields
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note (Description)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it },
                        label = { Text("Amount (₹)") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = payee,
                        onValueChange = { payee = it },
                        label = { Text("Payee Name") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = vpa,
                    onValueChange = { vpa = it },
                    label = { Text("UPI VPA (e.g. name@okhdfcbank)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Reference Number with Copy Button
                OutlinedTextField(
                    value = referenceNumber,
                    onValueChange = { referenceNumber = it },
                    label = { Text("UPI Reference ID") },
                    trailingIcon = {
                        if (referenceNumber.isNotEmpty()) {
                            IconButton(onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("UPI Ref", referenceNumber))
                                Toast.makeText(context, "Copied reference number", Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy Reference")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = paymentMethod,
                        onValueChange = { paymentMethod = it },
                        label = { Text("Payment Method") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = superMoneyId,
                        onValueChange = { superMoneyId = it },
                        label = { Text("super.money Txn ID") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Reviewed Checkbox
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isReviewed = !isReviewed }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isReviewed,
                        onCheckedChange = { isReviewed = it },
                        colors = CheckboxDefaults.colors(checkedColor = PolishPrimary)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Mark as Reviewed",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = PolishTextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Manual merge button if not merged
                    if (!entry.isMerged && onManualMergeRequest != null) {
                        OutlinedButton(
                            onClick = {
                                onDismiss()
                                onManualMergeRequest(entry)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.CallMerge, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Merge", fontSize = 12.sp)
                        }
                    }

                    // Delete button
                    OutlinedButton(
                        onClick = { showDeleteConfirm = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFBA1A1A)),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Delete", fontSize = 12.sp)
                    }

                    // Save changes button
                    Button(
                        onClick = {
                            val updated = entry.copy(
                                note = note.trim(),
                                payee = payee.trim(),
                                vpa = vpa.trim(),
                                amount = amount.trim(),
                                referenceNumber = referenceNumber.trim(),
                                paymentMethod = paymentMethod.trim(),
                                superMoneyTransactionId = superMoneyId.trim(),
                                isReviewed = isReviewed,
                                updatedAt = System.currentTimeMillis()
                            )
                            onSave(updated)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PolishPrimary),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save", fontSize = 12.sp)
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Transaction") },
            text = { Text("Are you sure you want to delete this transaction record? This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete(entry)
                        onDismiss()
                    }
                ) {
                    Text("Delete", color = Color(0xFFBA1A1A))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (screenshotToDeleteIsScreenB != null) {
        val isScreenB = screenshotToDeleteIsScreenB == true
        val label = if (isScreenB) "Screen B (Note Details)" else "Screen A (Payment Result)"
        AlertDialog(
            onDismissRequest = { screenshotToDeleteIsScreenB = null },
            title = { Text("Delete $label Screenshot?") },
            text = {
                Text("Are you sure you want to delete this screenshot from device storage? The extracted transaction data and notes will remain saved.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteScreenshot?.invoke(isScreenB)
                        screenshotToDeleteIsScreenB = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { screenshotToDeleteIsScreenB = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
