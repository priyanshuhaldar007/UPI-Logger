package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.AppleAccent
import com.example.ui.theme.AppleBackground
import com.example.ui.theme.AppleCardBorder
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
import com.example.ui.theme.AppleSurface
import com.example.ui.theme.AppleSurfaceElevated
import com.example.ui.theme.AppleSurfaceTranslucent
import com.example.ui.theme.AppleSwipeReview
import com.example.ui.theme.AppleTextPrimary
import com.example.ui.theme.AppleTextSecondary
import com.example.ui.theme.AppleTextTertiary
import com.example.ui.theme.AppleTypography
import com.example.ui.theme.LocalReduceMotion
import com.example.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExportScreen(
    viewModel: MainViewModel,
    onDismissRequest: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val darkTheme = isSystemInDarkTheme()
    val reduceMotion = LocalReduceMotion.current
    val coroutineScope = rememberCoroutineScope()

    val allTransactions by viewModel.allTransactions.collectAsStateWithLifecycle()
    val totalCount by viewModel.totalCount.collectAsStateWithLifecycle()
    val reviewedCount = remember(allTransactions) { allTransactions.count { it.isReviewed } }

    var onlyReviewed by remember { mutableStateOf(false) }
    var isExporting by remember { mutableStateOf(false) }

    val previewColumns = listOf(
        "Date", "Time", "Amount", "Payee", "VPA", "Note", "UPI Ref", "Payment Method", "super.money ID", "Merge Status"
    )

    val bgColor = if (darkTheme) AppleDarkBackground else AppleBackground
    val cardSurface = if (darkTheme) AppleDarkSurfaceTranslucent else AppleSurfaceTranslucent
    val cardElevated = if (darkTheme) AppleDarkSurfaceElevated else AppleSurfaceElevated
    val cardBorder = if (darkTheme) AppleDarkCardBorder else AppleCardBorder
    val textPrimary = if (darkTheme) AppleDarkTextPrimary else AppleTextPrimary
    val textSecondary = if (darkTheme) AppleDarkTextSecondary else AppleTextSecondary
    val textTertiary = if (darkTheme) AppleDarkTextTertiary else AppleTextTertiary
    val accentColor = if (darkTheme) AppleDarkAccent else AppleAccent

    val offsetY = remember { Animatable(0f) }
    val density = LocalDensity.current
    val dismissThresholdPx = with(density) { 120.dp.toPx() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        // Physical Sheet Container with Spring Physics and Drag Handling
        Column(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(0, offsetY.value.roundToInt().coerceAtLeast(0)) }
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragEnd = {
                            coroutineScope.launch {
                                if (offsetY.value > dismissThresholdPx && onDismissRequest != null) {
                                    onDismissRequest()
                                }
                                // Spring-snap back with physical bounce
                                offsetY.animateTo(
                                    targetValue = 0f,
                                    animationSpec = AppleMotion.swipeSpring(reduceMotion)
                                )
                            }
                        },
                        onDragCancel = {
                            coroutineScope.launch {
                                offsetY.animateTo(
                                    targetValue = 0f,
                                    animationSpec = AppleMotion.swipeSpring(reduceMotion)
                                )
                            }
                        },
                        onVerticalDrag = { _, dragAmount ->
                            coroutineScope.launch {
                                val current = offsetY.value
                                // Apply rubber-band resistance when pulled downward
                                val resistance = if (current > 0) 0.55f else 0.2f
                                offsetY.snapTo((current + dragAmount * resistance).coerceAtLeast(0f))
                            }
                        }
                    )
                }
                .verticalScroll(rememberScrollState())
                .padding(horizontal = AppleSpacing.md, vertical = AppleSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(AppleSpacing.md)
        ) {
            // Apple Sheet Grab Handle
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = AppleSpacing.xs),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(5.dp)
                        .clip(CircleShape)
                        .background(textTertiary.copy(alpha = 0.4f))
                )
            }

            // Screen Header
            Column {
                Text(
                    text = "Export Transactions",
                    style = AppleTypography.LargeTitle.copy(color = textPrimary)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Generate standard RFC-4180 CSV spreadsheets locally with UTF-8 BOM.",
                    style = AppleTypography.Caption.copy(color = textSecondary)
                )
            }

            // Export Configuration Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(AppleRadius.card),
                colors = CardDefaults.cardColors(containerColor = cardSurface),
                border = BorderStroke(1.dp, cardBorder)
            ) {
                Column(modifier = Modifier.padding(AppleSpacing.md)) {
                    Text(
                        text = "Data Scope",
                        style = AppleTypography.Subtitle.copy(color = textPrimary)
                    )

                    Spacer(modifier = Modifier.height(AppleSpacing.sm))

                    // Intentional iOS-style Switch Setting Row
                    Surface(
                        shape = RoundedCornerShape(AppleRadius.chip),
                        color = cardElevated,
                        border = BorderStroke(1.dp, cardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onlyReviewed = !onlyReviewed }
                                .padding(horizontal = AppleSpacing.md, vertical = AppleSpacing.sm),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Export Verified Records Only",
                                    style = AppleTypography.BodyEmphasized.copy(color = textPrimary)
                                )
                                Text(
                                    text = if (onlyReviewed) {
                                        "Exporting $reviewedCount reviewed entries"
                                    } else {
                                        "Exporting all $totalCount recorded entries"
                                    },
                                    style = AppleTypography.Caption.copy(color = textSecondary)
                                )
                            }

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
                        }
                    }

                    Spacer(modifier = Modifier.height(AppleSpacing.md))

                    // Primary Export Action Button
                    Button(
                        onClick = {
                            if (allTransactions.isEmpty()) {
                                Toast.makeText(context, "No transactions recorded to export", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            isExporting = true
                            viewModel.exportCsv(context, onlyReviewed) { result ->
                                isExporting = false
                                if (result.isSuccess) {
                                    Toast.makeText(
                                        context,
                                        "CSV exported: ${result.rowCount} records saved",
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("export_csv_action_button"),
                        shape = RoundedCornerShape(AppleRadius.sheet),
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        enabled = !isExporting
                    ) {
                        if (isExporting) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(AppleSpacing.xs))
                            Text(
                                "Generating CSV…",
                                style = AppleTypography.BodyEmphasized.copy(color = Color.White)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.FileDownload,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(AppleSpacing.xs))
                            Text(
                                "Export & Share CSV",
                                style = AppleTypography.BodyEmphasized.copy(color = Color.White)
                            )
                        }
                    }
                }
            }

            // Columns Breakdown Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(AppleRadius.card),
                colors = CardDefaults.cardColors(containerColor = cardSurface),
                border = BorderStroke(1.dp, cardBorder)
            ) {
                Column(modifier = Modifier.padding(AppleSpacing.md)) {
                    Text(
                        text = "Included Spreadsheet Columns",
                        style = AppleTypography.Subtitle.copy(color = textPrimary)
                    )
                    Spacer(modifier = Modifier.height(AppleSpacing.xxs))
                    Text(
                        text = "Clean RFC-4180 formatting with UTF-8 BOM compatibility for Excel & Sheets:",
                        style = AppleTypography.Caption.copy(color = textSecondary)
                    )

                    Spacer(modifier = Modifier.height(AppleSpacing.sm))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(AppleSpacing.xs),
                        verticalArrangement = Arrangement.spacedBy(AppleSpacing.xs)
                    ) {
                        previewColumns.forEach { col ->
                            Surface(
                                shape = RoundedCornerShape(AppleRadius.chip),
                                color = cardElevated,
                                border = BorderStroke(1.dp, cardBorder)
                            ) {
                                Text(
                                    text = col,
                                    style = AppleTypography.CaptionEmphasized.copy(
                                        color = if (col == "Merge Status") AppleStatusMergedFg else textPrimary
                                    ),
                                    modifier = Modifier.padding(
                                        horizontal = AppleSpacing.sm,
                                        vertical = AppleSpacing.xxs
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Test Sample Generator Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(AppleRadius.card),
                colors = CardDefaults.cardColors(containerColor = cardSurface),
                border = BorderStroke(1.dp, cardBorder)
            ) {
                Column(modifier = Modifier.padding(AppleSpacing.md)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(AppleSpacing.xs)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Science,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Validation & Testing",
                            style = AppleTypography.Subtitle.copy(color = textPrimary)
                        )
                    }

                    Spacer(modifier = Modifier.height(AppleSpacing.xxs))

                    Text(
                        text = "Generate synthetic transaction captures to test spreadsheet compatibility and verify column layouts without live payments.",
                        style = AppleTypography.Caption.copy(color = textSecondary)
                    )

                    Spacer(modifier = Modifier.height(AppleSpacing.sm))

                    OutlinedButton(
                        onClick = {
                            viewModel.insertSamplePair(context)
                            Toast.makeText(context, "Added synthetic sample pair", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(AppleRadius.chip),
                        border = BorderStroke(1.dp, cardBorder)
                    ) {
                        Text(
                            "Generate Sample Pair",
                            style = AppleTypography.CaptionEmphasized.copy(color = accentColor)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(AppleSpacing.xl))
        }
    }
}
