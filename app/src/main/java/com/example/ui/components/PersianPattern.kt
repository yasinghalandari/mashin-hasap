package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Draws an understated, bespoke Persian geometric Shamseh (eight-pointed star / Girih)
 * motif with a soft turquoise radial glow for atmospheric Iranian elegance.
 */
@Composable
fun PersianShamsehBackdrop(
    accentColor: Color,
    secondaryColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val center = Offset(size.width * 0.16f, size.height * 0.35f)
        val maxRadius = min(size.width, size.height) * 0.72f

        // Subtle turquoise ambient glow
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    accentColor.copy(alpha = 0.11f),
                    secondaryColor.copy(alpha = 0.04f),
                    Color.Transparent
                ),
                center = center,
                radius = maxRadius * 1.45f
            ),
            radius = maxRadius * 1.45f,
            center = center
        )

        // Concentric Shamseh geometric rings
        val stroke = Stroke(width = 1.4f)
        for (ring in 1..2) {
            val r = maxRadius * (0.45f + ring * 0.22f)
            val squareSize = r * 1.414f
            val topLeft = Offset(center.x - squareSize / 2f, center.y - squareSize / 2f)
            drawRect(
                color = accentColor.copy(alpha = 0.07f),
                topLeft = topLeft,
                size = androidx.compose.ui.geometry.Size(squareSize, squareSize),
                style = stroke
            )
            rotate(degrees = 45f, pivot = center) {
                drawRect(
                    color = secondaryColor.copy(alpha = 0.06f),
                    topLeft = topLeft,
                    size = androidx.compose.ui.geometry.Size(squareSize, squareSize),
                    style = stroke
                )
            }
        }

        // Inner 8-pointed rosette star
        val rosettePath = Path()
        val outerR = maxRadius * 0.36f
        val innerR = maxRadius * 0.19f
        val points = 16
        for (i in 0 until points) {
            val angle = (i * Math.PI * 2.0 / points)
            val radius = if (i % 2 == 0) outerR else innerR
            val x = center.x + (radius * cos(angle)).toFloat()
            val y = center.y + (radius * sin(angle)).toFloat()
            if (i == 0) rosettePath.moveTo(x, y) else rosettePath.lineTo(x, y)
        }
        rosettePath.close()
        drawPath(
            path = rosettePath,
            color = accentColor.copy(alpha = 0.08f),
            style = stroke
        )
    }
}
