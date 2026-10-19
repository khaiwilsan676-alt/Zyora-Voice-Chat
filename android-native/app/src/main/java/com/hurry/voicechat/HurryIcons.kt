package com.hurry.voicechat

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun HurryHomeIcon(active: Boolean, modifier: Modifier = Modifier) {
    Canvas(modifier.size(25.dp)) {
        val c = if (active) HurryBlue else Color(0xFF303030)
        val stroke = 2.1.dp.toPx()
        val p = Path().apply {
            moveTo(size.width * .10f, size.height * .47f)
            lineTo(size.width * .50f, size.height * .12f)
            lineTo(size.width * .90f, size.height * .47f)
            moveTo(size.width * .20f, size.height * .40f)
            lineTo(size.width * .20f, size.height * .78f)
            quadraticBezierTo(size.width * .20f, size.height * .88f, size.width * .30f, size.height * .88f)
            lineTo(size.width * .70f, size.height * .88f)
            quadraticBezierTo(size.width * .80f, size.height * .88f, size.width * .80f, size.height * .78f)
            lineTo(size.width * .80f, size.height * .40f)
        }
        drawPath(p, c, style = Stroke(stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawRoundRect(c, Offset(size.width * .40f, size.height * .63f), Size(size.width * .20f, size.height * .25f),
            androidx.compose.ui.geometry.CornerRadius(2.5.dp.toPx()), style = Stroke(stroke, cap = StrokeCap.Round))
    }
}

@Composable
fun HurryMessageIcon(active: Boolean, modifier: Modifier = Modifier) {
    Canvas(modifier.size(30.dp)) {
        val s = size.minDimension / 36f
        val outline = Color(0xFF1D1D1F)
        val bubble = Path().apply {
            moveTo(6f*s, 10.5f*s)
            cubicTo(6f*s, 7f*s, 8.3f*s, 5f*s, 12.2f*s, 5f*s)
            lineTo(23.8f*s, 5f*s)
            cubicTo(27.7f*s, 5f*s, 30f*s, 7f*s, 30f*s, 10.5f*s)
            lineTo(30f*s, 16.5f*s)
            cubicTo(30f*s, 20f*s, 27.7f*s, 22f*s, 23.8f*s, 22f*s)
            lineTo(21f*s, 22f*s)
            lineTo(17.5f*s, 27.2f*s)
            cubicTo(17f*s, 28f*s, 15.8f*s, 28f*s, 15.2f*s, 27.2f*s)
            lineTo(12.2f*s, 22f*s)
            cubicTo(8.3f*s, 22f*s, 6f*s, 20f*s, 6f*s, 16.5f*s)
            close()
        }
        if (active) {
            drawPath(bubble, HurryBlue)
        }
        // Keep the same black outline in both states; only the bubble fill turns blue.
        drawPath(bubble, outline, style = Stroke(width = 2.4f*s, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawPath(
            Path().apply {
                moveTo(12f*s, 14.5f*s)
                cubicTo(13.5f*s, 12.5f*s, 15.5f*s, 14.5f*s, 19.5f*s, 12.5f*s)
                cubicTo(21.5f*s, 14.5f*s, 24f*s, 14.5f*s, 24f*s, 14.5f*s)
            },
            outline,
            style = Stroke(width = 1.8f*s, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

@Composable
fun HurryMeIcon(active: Boolean, modifier: Modifier = Modifier) {
    Canvas(modifier.size(25.dp)) {
        val c = if (active) HurryBlue else Color(0xFF303030)
        drawCircle(color = c, radius = size.minDimension*.22f, center = Offset(size.width*.5f,size.height*.32f), style = Stroke(width = 2.1.dp.toPx()))
        drawRoundRect(color = c, topLeft=Offset(size.width*.2f,size.height*.58f),
            size=Size(size.width*.6f,size.height*.27f),
            cornerRadius=androidx.compose.ui.geometry.CornerRadius(8.dp.toPx()),
            style=Stroke(2.1.dp.toPx()))
    }
}

@Composable
fun HurrySearchIcon(modifier: Modifier = Modifier) {
    Canvas(modifier.size(26.dp)) {
        val c = Color(0xFF222222)
        drawCircle(color = c, radius = size.minDimension*.25f, center = Offset(size.width*.446f,size.height*.446f), style = Stroke(width = 2.1.dp.toPx()))
        drawLine(color = c, start = Offset(size.width*.65f,size.height*.65f), end = Offset(size.width*.86f,size.height*.86f), strokeWidth = 2.1.dp.toPx(), cap = StrokeCap.Round)
    }
}
