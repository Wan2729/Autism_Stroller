package com.example.autismstroller.reusables

import android.graphics.BlurMaskFilter
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun Modifier.customShadow(
    color: Color = Color.Black,
    borderRadius: Dp = 0.dp,
    blurRadius: Dp = 0.dp,
    offsetY: Dp = 0.dp,
    offsetX: Dp = 0.dp,
    spread: Dp = 0f.dp,
    modifier: Modifier = Modifier
) = this.then(
    modifier.drawBehind {
        this.drawIntoCanvas {
            val paint = Paint()
            val frameworkPaint = paint.asFrameworkPaint()
            val spreadPixel = spread.toPx()
            val leftPixel = (0f - spreadPixel) + offsetX.toPx()
            val topPixel = (0f - spreadPixel) + offsetY.toPx()
            val rightPixel = (this.size.width + spreadPixel) + offsetX.toPx()
            val bottomPixel = (this.size.height + spreadPixel) + offsetY.toPx()

            if (blurRadius != 0.dp) {
                frameworkPaint.maskFilter =
                    (BlurMaskFilter(blurRadius.toPx(), BlurMaskFilter.Blur.NORMAL))
            }

            frameworkPaint.color = color.toArgb()
            it.drawRoundRect(
                left = leftPixel,
                top = topPixel,
                right = rightPixel,
                bottom = bottomPixel,
                radiusX = borderRadius.toPx(),
                radiusY = borderRadius.toPx(),
                paint
            )
        }
    }
)

fun Modifier.figmaDropShadow(
    color: Color = Color(0x55000000), // Figma default: black with alpha
    blurRadius: Dp = 10.dp,
    offsetX: Dp = 0.dp,
    offsetY: Dp = 4.dp,
    borderRadius: Dp = 0.dp,
) = this.drawBehind {
    val paint = Paint().asFrameworkPaint().apply {
        isAntiAlias = true
        this.color = color.toArgb()
        maskFilter = BlurMaskFilter(blurRadius.toPx(), BlurMaskFilter.Blur.NORMAL)
    }

    val left = 0f + offsetX.toPx()
    val top = 0f + offsetY.toPx()
    val right = size.width + offsetX.toPx()
    val bottom = size.height + offsetY.toPx()

    drawContext.canvas.nativeCanvas.drawRoundRect(
        left,
        top,
        right,
        bottom,
        borderRadius.toPx(),
        borderRadius.toPx(),
        paint
    )
}