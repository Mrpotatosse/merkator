package io.github.mrpotatosse.merkator.models.game

import androidx.compose.ui.graphics.ImageBitmap
import io.github.mrpotatosse.merkator.projections.BasicDraw

data class BasicDrawWithBitmap(
    val elementDraw: BasicDraw,
    val bitmap: ImageBitmap
)