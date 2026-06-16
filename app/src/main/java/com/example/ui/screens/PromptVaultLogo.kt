package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun PromptVaultLogo(
    modifier: Modifier = Modifier.size(36.dp)
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        
        // Let's draw the isometric box.
        val cx = w / 2f
        val cy = h / 2f
        
        // Scale factor:
        val r = w * 0.46f
        val dx = r * 0.866f // cos(30 degrees)
        val dy = r * 0.5f   // sin(30 degrees)
        
        // Define key isometric 3D vertices:
        val pCenter = Offset(cx, cy)
        val pTop = Offset(cx, cy - r)
        val pBottom = Offset(cx, cy + r)
        val pLeftTop = Offset(cx - dx, cy - dy)
        val pLeftBottom = Offset(cx - dx, cy + dy)
        val pRightTop = Offset(cx + dx, cy - dy)
        val pRightBottom = Offset(cx + dx, cy + dy)
        
        // 1. Top face path (rhombus: pTop -> pRightTop -> pCenter -> pLeftTop)
        val topFace = Path().apply {
            moveTo(pTop.x, pTop.y)
            lineTo(pRightTop.x, pRightTop.y)
            lineTo(pCenter.x, pCenter.y)
            lineTo(pLeftTop.x, pLeftTop.y)
            close()
        }
        
        // 2. Right face path (rhombus: pCenter -> pRightTop -> pRightBottom -> pBottom)
        val rightFace = Path().apply {
            moveTo(pCenter.x, pCenter.y)
            lineTo(pRightTop.x, pRightTop.y)
            lineTo(pRightBottom.x, pRightBottom.y)
            lineTo(pBottom.x, pBottom.y)
            close()
        }

        // 3. Left face outline/surface
        val leftFace = Path().apply {
            moveTo(pLeftTop.x, pLeftTop.y)
            lineTo(pCenter.x, pCenter.y)
            lineTo(pBottom.x, pBottom.y)
            lineTo(pLeftBottom.x, pLeftBottom.y)
            close()
        }

        // --- Shading resembling the image ---
        // Top and right surfaces are shaded in beautiful #A8B7C6 slate-blue tint from the logo
        drawPath(path = topFace, color = Color(0xFFA8B7C6))
        drawPath(path = rightFace, color = Color(0xFFA8B7C6).copy(alpha = 0.82f)) // slight 3D shadow depth
        drawPath(path = leftFace, color = Color(0xFFA8B7C6).copy(alpha = 0.5f))

        // Bold black block letters "P" on Left, "V" on Right, just like the uploaded image!
        val letterColor = Color(0xFF09090B)
        
        // "P" block letter on Left plane:
        val pStem = Path().apply {
            // Skewed vertical bar on left face
            moveTo(cx - dx * 0.65f, cy + dy * 0.25f)
            lineTo(cx - dx * 0.40f, cy + dy * 0.12f)
            lineTo(cx - dx * 0.40f, cy - dy * 0.65f)
            lineTo(cx - dx * 0.65f, cy - dy * 0.52f)
            close()
        }
        drawPath(path = pStem, color = letterColor)
        
        val pLoop = Path().apply {
            // Loop of P
            moveTo(cx - dx * 0.40f, cy - dy * 0.65f)
            lineTo(cx - dx * 0.12f, cy - dy * 0.79f)
            lineTo(cx - dx * 0.12f, cy - dy * 0.25f)
            lineTo(cx - dx * 0.40f, cy - dy * 0.11f)
            close()
        }
        drawPath(path = pLoop, color = letterColor)
        
        // Outer loop cutout for "P":
        val pInner = Path().apply {
            moveTo(cx - dx * 0.34f, cy - dy * 0.52f)
            lineTo(cx - dx * 0.18f, cy - dy * 0.60f)
            lineTo(cx - dx * 0.18f, cy - dy * 0.40f)
            lineTo(cx - dx * 0.34f, cy - dy * 0.32f)
            close()
        }
        drawPath(path = pInner, color = Color(0xFFA8B7C6).copy(alpha = 0.5f)) // Match background tint of left face
        
        // "V" block letter on Right plane:
        val vLeftStem = Path().apply {
            moveTo(cx + dx * 0.12f, cy - dy * 0.79f)
            lineTo(cx + dx * 0.38f, cy - dy * 0.66f)
            lineTo(cx + dx * 0.48f, cy + dy * 0.15f)
            lineTo(cx + dx * 0.22f, cy + dy * 0.02f)
            close()
        }
        drawPath(path = vLeftStem, color = letterColor)

        val vRightStem = Path().apply {
            moveTo(cx + dx * 0.65f, cy - dy * 0.52f)
            lineTo(cx + dx * 0.38f, cy - dy * 0.66f)
            lineTo(cx + dx * 0.48f, cy + dy * 0.15f)
            lineTo(cx + dx * 0.75f, cy + dy * 0.29f)
            close()
        }
        drawPath(path = vRightStem, color = letterColor)

        // Draw crisp 3D outline frame of the wireframe cube:
        val boxStroke = Stroke(width = 3.5f)
        
        // Complete outer boundary
        val outerHex = Path().apply {
            moveTo(pTop.x, pTop.y)
            lineTo(pRightTop.x, pRightTop.y)
            lineTo(pRightBottom.x, pRightBottom.y)
            lineTo(pBottom.x, pBottom.y)
            lineTo(pLeftBottom.x, pLeftBottom.y)
            lineTo(pLeftTop.x, pLeftTop.y)
            close()
        }
        drawPath(path = outerHex, color = letterColor, style = boxStroke)
        
        // Inner corner lines radiating from center:
        drawLine(color = letterColor, start = pCenter, end = pTop, strokeWidth = 3.5f)
        drawLine(color = letterColor, start = pCenter, end = pBottom, strokeWidth = 3.5f)
        drawLine(color = letterColor, start = pCenter, end = pLeftTop, strokeWidth = 3.5f)
        drawLine(color = letterColor, start = pCenter, end = pRightTop, strokeWidth = 3.5f)
        drawLine(color = letterColor, start = pLeftTop, end = pLeftBottom, strokeWidth = 3.5f)
        drawLine(color = letterColor, start = pLeftBottom, end = pBottom, strokeWidth = 3.5f)
    }
}
