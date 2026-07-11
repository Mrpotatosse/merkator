package io.github.mrpotatosse.merkator.components.game

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawTransform
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import io.github.mrpotatosse.merkator.models.game.BasicDrawWithBitmap
import io.github.mrpotatosse.merkator.projections.FixtureElementDraw
import io.github.mrpotatosse.merkator.projections.GraphicalElementDraw
import io.github.mrpotatosse.merkator.utils.tintFilter

@Composable
fun ElementsCanvas(
    drawsProvider: () -> List<BasicDrawWithBitmap>,
    modifier: Modifier = Modifier,
    transformBlock: DrawTransform.() -> Unit,
) {
    Spacer(
        modifier
            .fillMaxSize()
            .graphicsLayer()      // own render layer, isolates invalidation
            .drawBehind {
                withTransform(transformBlock) {
                    drawsProvider().forEach { draw ->
                        when (draw.elementDraw) {
                            is FixtureElementDraw -> {
                                val w = draw.bitmap.width
                                val h = draw.bitmap.height
                                val halfW = w * 0.5f
                                val halfH = h * 0.5f
                                val colorFilter = tintFilter(
                                    draw.elementDraw.color.getR(),
                                    draw.elementDraw.color.getG(),
                                    draw.elementDraw.color.getB(),
                                    draw.elementDraw.color.getA()
                                )

                                val posX = draw.elementDraw.x + halfW
                                val posY = draw.elementDraw.y + halfH

                                withTransform({
                                    translate(
                                        posX,
                                        posY
                                    )
                                    rotate(
                                        degrees = draw.elementDraw.rotation / 100f,
                                        pivot = Offset.Zero
                                    )
                                    scale(
                                        draw.elementDraw.scale.x / 1000f,
                                        draw.elementDraw.scale.y / 1000f,
                                        pivot = Offset.Zero
                                    )
                                    translate(
                                        -halfW,
                                        -halfH
                                    )
                                }) {
                                    drawImage(
                                        image = draw.bitmap,
                                        dstOffset = IntOffset.Zero,
                                        dstSize = IntSize(w, h),
                                        colorFilter = colorFilter
                                    )
                                }
                            }

                            is GraphicalElementDraw -> {
                                val size = IntSize(
                                    draw.elementDraw.width,
                                    draw.elementDraw.height
                                )
                                val offset = IntOffset(
                                    draw.elementDraw.x.toInt(),
                                    draw.elementDraw.y.toInt()
                                )
                                val colorFilter = tintFilter(
                                    draw.elementDraw.r,
                                    draw.elementDraw.g,
                                    draw.elementDraw.b
                                )

                                if (draw.elementDraw.flipped) {
                                    withTransform({
                                        scale(
                                            -1f,
                                            1f,
                                            pivot = Offset(draw.elementDraw.x + draw.elementDraw.width / 2f, 0f)
                                        )
                                    }) {
                                        drawImage(
                                            image = draw.bitmap,
                                            dstSize = size,
                                            dstOffset = offset,
                                            colorFilter = colorFilter
                                        )
                                    }
                                } else {
                                    drawImage(
                                        image = draw.bitmap,
                                        dstSize = size,
                                        dstOffset = offset,
                                        colorFilter = colorFilter
                                    )
                                }
                            }
                        }
                    }
                }
            }
    )
}