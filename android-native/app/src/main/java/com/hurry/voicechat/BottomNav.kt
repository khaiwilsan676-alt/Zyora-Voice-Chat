package com.hurry.voicechat

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.interaction.MutableInteractionSource

@Composable
fun HurryBottomNav(tab: HurryTab, languageCode: String = "en", onTab: (HurryTab) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Color.White)
            .navigationBarsPadding()
            .height(64.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        NavItem(HurryTab.HOME, tab, onTab, nativeText(languageCode, "home"))
        NavItem(HurryTab.MESSAGE, tab, onTab, nativeText(languageCode, "message"))
        NavItem(HurryTab.ME, tab, onTab, nativeText(languageCode, "me"))
    }
}

@Composable
private fun NavItem(
    which: HurryTab,
    selected: HurryTab,
    onTab: (HurryTab) -> Unit,
    label: String
) {
    Column(
        Modifier
            .width(72.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onTab(which) }
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        if (which == HurryTab.MESSAGE) {
            HurryMessageIcon(active = which == selected, modifier = Modifier.size(30.dp))
        } else Canvas(Modifier.size(30.dp)) {
            val s = size.minDimension / 36f
            val active = which == selected
            val fill = if (active) Color(0xFF3B82F6) else Color.White
            val stroke = Color(0xFF1D1D1F)
            fun path(block: Path.() -> Unit) = Path().apply(block)

            when (which) {
                HurryTab.HOME -> {
                    val q = path {
                        moveTo(18*s,2.8f*s)
                        cubicTo(20.2f*s,2.8f*s,30.2f*s,8.2f*s,30.2f*s,12.6f*s)
                        lineTo(30.2f*s,23.2f*s)
                        cubicTo(30.2f*s,27.8f*s,28f*s,31f*s,18f*s,31f*s)
                        cubicTo(8f*s,31f*s,5.8f*s,27.8f*s,5.8f*s,23.2f*s)
                        lineTo(5.8f*s,12.6f*s)
                        cubicTo(5.8f*s,8.2f*s,15.8f*s,2.8f*s,18f*s,2.8f*s)
                        close()
                    }
                    drawPath(path = q, color = fill)
                    drawPath(path = q, color = stroke, style = Stroke(2.4f*s, cap = StrokeCap.Round, join = StrokeJoin.Round))
                    drawPath(path = path {
                        moveTo(12.2f*s,14.2f*s)
                        cubicTo(13.3f*s,12.6f*s,14.9f*s,12.1f*s,16.8f*s,13.4f*s)
                    }, stroke, style = Stroke(1.8f*s, cap = StrokeCap.Round))
                    drawPath(path = path {
                        moveTo(11.2f*s,20.8f*s)
                        cubicTo(12.5f*s,24.2f*s,21f*s,25.6f*s,24.3f*s,20.2f*s)
                    }, stroke, style = Stroke(1.8f*s, cap = StrokeCap.Round))
                }
                HurryTab.MESSAGE -> {
                    val q = path {
                        moveTo(6f*s,10.5f*s)
                        cubicTo(6f*s,7f*s,8.3f*s,5f*s,12.2f*s,5f*s)
                        lineTo(23.8f*s,5f*s)
                        cubicTo(27.7f*s,5f*s,30f*s,7f*s,30f*s,10.5f*s)
                        lineTo(30f*s,16.5f*s)
                        cubicTo(30f*s,20f*s,27.7f*s,22f*s,23.8f*s,22f*s)
                        lineTo(21f*s,22f*s)
                        lineTo(17.5f*s,27.2f*s)
                        cubicTo(17f*s,28f*s,15.8f*s,28f*s,15.2f*s,27.2f*s)
                        lineTo(12.2f*s,22f*s)
                        cubicTo(8.3f*s,22f*s,6f*s,20f*s,6f*s,16.5f*s)
                        close()
                    }
                    drawPath(path = q, color = fill)
                    drawPath(path = q, color = stroke, style = Stroke(2.4f*s))
                    drawPath(path = path {
                        moveTo(12f*s,14.5f*s)
                        cubicTo(13.5f*s,12.5f*s,15.5f*s,14.5f*s,19.5f*s,12.5f*s)
                        cubicTo(21.5f*s,14.5f*s,24f*s,14.5f*s,24f*s,14.5f*s)
                    }, stroke, style = Stroke(1.8f*s, cap = StrokeCap.Round))
                }
                HurryTab.ME -> {
                    val q = path {
                        moveTo(18f*s,4.5f*s)
                        cubicTo(23.5f*s,4.5f*s,28f*s,8.5f*s,27.2f*s,13.8f*s)
                        lineTo(26.2f*s,19.8f*s)
                        cubicTo(26f*s,21.2f*s,27.2f*s,22.5f*s,28.6f*s,23.1f*s)
                        cubicTo(30.6f*s,24f*s,31f*s,26.2f*s,29f*s,27.5f*s)
                        cubicTo(27.5f*s,28.5f*s,25f*s,28.8f*s,22f*s,28.8f*s)
                        lineTo(14f*s,28.8f*s)
                        cubicTo(11f*s,28.8f*s,8.5f*s,28.5f*s,7f*s,27.5f*s)
                        cubicTo(5f*s,26.2f*s,5.4f*s,24f*s,7.4f*s,23.1f*s)
                        cubicTo(8.8f*s,22.5f*s,10f*s,21.2f*s,9.8f*s,19.8f*s)
                        lineTo(8.8f*s,13.8f*s)
                        cubicTo(8f*s,8.5f*s,12.5f*s,4.5f*s,18f*s,4.5f*s)
                        close()
                    }
                    drawPath(path = q, color = fill)
                    drawPath(path = q, color = stroke, style = Stroke(2.4f*s))
                    drawCircle(color = stroke, radius = 1.6f*s, center = androidx.compose.ui.geometry.Offset(14f*s,15f*s))
                    drawCircle(color = stroke, radius = 1.6f*s, center = androidx.compose.ui.geometry.Offset(22f*s,15f*s))
                }
            }
        }
        Text(
            label,
            fontSize = 12.sp,
            color = if (which == selected) Color.Black else Color(0xFF6B7280)
        )
    }
}
