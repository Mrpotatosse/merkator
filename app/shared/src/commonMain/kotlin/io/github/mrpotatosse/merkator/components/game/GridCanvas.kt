package io.github.mrpotatosse.merkator.components.game

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawTransform
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer

@Composable
fun GridCanvas(
    gridLinesProvider: () -> List<List<Pair<Float, Float>>>,
    gridLinesColor: Color = Color.White,
    transformBlock: DrawTransform.() -> Unit,
    modifier: Modifier = Modifier
) {
    Spacer(
        modifier
            .fillMaxSize()
            .graphicsLayer()
            .drawBehind {
                val gridLines = gridLinesProvider()
                if (gridLines.isEmpty()) return@drawBehind
                withTransform(transformBlock) {
                    gridLines.forEach { (start, end) ->
                        drawLine(
                            color = gridLinesColor,
                            start = Offset(start.first, start.second),
                            end = Offset(end.first, end.second),
                            strokeWidth = 1f
                        )
                    }
                }
            }
    )
}