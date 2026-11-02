package com.studyplanner.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import kotlin.math.ceil

@Composable
fun BarChart(
    values: List<Float>,
    labels: List<String>,
    color: Color,
    modifier: Modifier = Modifier,
    height: Dp = 200.dp,
    formatValue: (Float) -> String = { value ->
        if (value % 1f == 0f) value.toInt().toString()
        else String.format(Locale.US, "%.1f", value)
    },
) {
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val density = LocalDensity.current

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height),
    ) {
        val left = 36.dp.toPx()
        val right = 8.dp.toPx()
        val top = 10.dp.toPx()
        val bottom = 24.dp.toPx()
        val plotWidth = (size.width - left - right).coerceAtLeast(1f)
        val plotHeight = (size.height - top - bottom).coerceAtLeast(1f)
        val rawMax = (values.maxOrNull() ?: 0f).coerceAtLeast(0f)
        // Small values (for example a single minute) keep a readable bar instead of
        // being flattened against a whole-hour scale.
        val maxValue = when {
            rawMax <= 0f -> 1f
            rawMax <= 10f -> ceil(rawMax)
            rawMax <= 60f -> ceil(rawMax / 10f) * 10f
            rawMax <= 240f -> ceil(rawMax / 30f) * 30f
            else -> ceil(rawMax / 60f) * 60f
        }.coerceAtLeast(1f)

        val axisPaint = android.graphics.Paint().apply {
            this.color = labelColor.toArgb()
            textSize = with(density) { 10.sp.toPx() }
            isAntiAlias = true
            textAlign = android.graphics.Paint.Align.RIGHT
        }
        val xPaint = android.graphics.Paint().apply {
            this.color = labelColor.toArgb()
            textSize = with(density) { 10.sp.toPx() }
            isAntiAlias = true
            textAlign = android.graphics.Paint.Align.CENTER
        }

        for (i in 0..2) {
            val fraction = i / 2f
            val y = top + plotHeight * (1f - fraction)
            drawLine(
                color = gridColor,
                start = Offset(left, y),
                end = Offset(size.width - right, y),
                strokeWidth = 1.dp.toPx(),
            )
            drawContext.canvas.nativeCanvas.drawText(
                formatValue(maxValue * fraction),
                left - 6.dp.toPx(),
                y + 4.dp.toPx(),
                axisPaint,
            )
        }

        if (values.isEmpty()) return@Canvas

        val slot = plotWidth / values.size
        val barWidth = (slot * 0.62f).coerceAtLeast(2.dp.toPx())

        values.forEachIndexed { index, value ->
            if (value <= 0f) return@forEachIndexed
            val barHeight = (value / maxValue) * plotHeight
            val x = left + slot * index + (slot - barWidth) / 2f
            drawRoundRect(
                color = color,
                topLeft = Offset(x, top + plotHeight - barHeight),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx()),
            )
        }

        val step = when {
            values.size <= 12 -> 1
            else -> 5
        }
        values.indices.forEach { index ->
            if (index != 0 && (index + 1) % step != 0) return@forEach
            val x = left + slot * index + slot / 2f
            drawContext.canvas.nativeCanvas.drawText(
                labels.getOrElse(index) { "" },
                x,
                size.height - 6.dp.toPx(),
                xPaint,
            )
        }
    }
}
