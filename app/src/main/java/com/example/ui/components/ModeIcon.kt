package com.example.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R

/**
 * Supported capture and application visual modes.
 */
enum class AppVisualMode(
    val title: String,
    val description: String,
    @DrawableRes val drawableRes: Int
) {
    COMBINED(
        title = "Dual Capture (Merged)",
        description = "Combines payment confirmation (Screen A) and remark note (Screen B)",
        drawableRes = R.drawable.ic_mode_combined
    ),
    SCREEN_A(
        title = "Screen A (Payment)",
        description = "Primary payment confirmation with ₹ amount and payee VPA",
        drawableRes = R.drawable.ic_mode_screen_a
    ),
    SCREEN_B(
        title = "Screen B (Note / Details)",
        description = "Transaction details receipt with custom note and remarks",
        drawableRes = R.drawable.ic_mode_screen_b
    ),
    REVIEW(
        title = "Verification & Review",
        description = "QA validation and verification status for logged records",
        drawableRes = R.drawable.ic_mode_review
    ),
    EXPORT(
        title = "CSV & Sheet Export",
        description = "Tabular data export and spreadsheet generation",
        drawableRes = R.drawable.ic_mode_export
    )
}

/**
 * Renders the custom-designed mode squircle icon matching the app's visual identity.
 */
@Composable
fun ModeIcon(
    mode: AppVisualMode,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    elevation: Dp = 2.dp
) {
    Image(
        painter = painterResource(id = mode.drawableRes),
        contentDescription = mode.title,
        modifier = modifier
            .size(size)
            .shadow(elevation, RoundedCornerShape(size * 0.22f), clip = false)
            .clip(RoundedCornerShape(size * 0.22f))
    )
}
