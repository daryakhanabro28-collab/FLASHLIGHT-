package com.example.ui.clock

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import java.util.*
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AnalogClock(
    calendar: Calendar,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.size(280.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = (size.minDimension / 2f) * 0.9f

            // Outer dial circle
            drawCircle(
                color = accentColor.copy(alpha = 0.2f),
                radius = radius,
                center = center,
                style = Stroke(width = 3.dp.toPx())
            )

            // Hour tick marks
            for (i in 0 until 12) {
                val angleRad = Math.toRadians((i * 30 - 90).toDouble())
                val isMajor = i % 3 == 0
                val tickLength = if (isMajor) 16.dp.toPx() else 8.dp.toPx()
                val tickWidth = if (isMajor) 3.dp.toPx() else 1.5.dp.toPx()

                val start = Offset(
                    (center.x + (radius - tickLength) * cos(angleRad)).toFloat(),
                    (center.y + (radius - tickLength) * sin(angleRad)).toFloat()
                )
                val end = Offset(
                    (center.x + radius * cos(angleRad)).toFloat(),
                    (center.y + radius * sin(angleRad)).toFloat()
                )

                drawLine(
                    color = if (isMajor) accentColor else accentColor.copy(alpha = 0.5f),
                    start = start,
                    end = end,
                    strokeWidth = tickWidth,
                    cap = StrokeCap.Round
                )
            }

            val hours = calendar.get(Calendar.HOUR)
            val minutes = calendar.get(Calendar.MINUTE)
            val seconds = calendar.get(Calendar.SECOND)
            val millis = calendar.get(Calendar.MILLISECOND)

            val smoothSeconds = seconds + millis / 1000f
            val smoothMinutes = minutes + smoothSeconds / 60f
            val smoothHours = hours + smoothMinutes / 60f

            // Hour Hand
            val hourAngle = Math.toRadians((smoothHours * 30.0 - 90.0))
            val hourLength = radius * 0.5f
            val hourEnd = Offset(
                (center.x + hourLength * cos(hourAngle)).toFloat(),
                (center.y + hourLength * sin(hourAngle)).toFloat()
            )
            drawLine(
                color = Color.White,
                start = center,
                end = hourEnd,
                strokeWidth = 5.dp.toPx(),
                cap = StrokeCap.Round
            )

            // Minute Hand
            val minuteAngle = Math.toRadians((smoothMinutes * 6.0 - 90.0))
            val minuteLength = radius * 0.72f
            val minuteEnd = Offset(
                (center.x + minuteLength * cos(minuteAngle)).toFloat(),
                (center.y + minuteLength * sin(minuteAngle)).toFloat()
            )
            drawLine(
                color = accentColor,
                start = center,
                end = minuteEnd,
                strokeWidth = 3.5.dp.toPx(),
                cap = StrokeCap.Round
            )

            // Second Hand
            val secondAngle = Math.toRadians((smoothSeconds * 6.0 - 90.0))
            val secondLength = radius * 0.85f
            val secondEnd = Offset(
                (center.x + secondLength * cos(secondAngle)).toFloat(),
                (center.y + secondLength * sin(secondAngle)).toFloat()
            )
            drawLine(
                color = Color(0xFFFF3366),
                start = center,
                end = secondEnd,
                strokeWidth = 1.8.dp.toPx(),
                cap = StrokeCap.Round
            )

            // Center Pin
            drawCircle(
                color = Color.White,
                radius = 5.dp.toPx(),
                center = center
            )
            drawCircle(
                color = Color(0xFFFF3366),
                radius = 2.5.dp.toPx(),
                center = center
            )
        }
    }
}
