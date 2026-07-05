package io.github.mrpotatosse.merkator.utils

import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix

fun tintFilter(redMul: Float, greenMul: Float, blueMul: Float): ColorFilter =
    ColorFilter.colorMatrix(
        ColorMatrix(
            floatArrayOf(
                redMul, 0f, 0f, 0f, 0f,
                0f, greenMul, 0f, 0f, 0f,
                0f, 0f, blueMul, 0f, 0f,
                0f, 0f, 0f, 1f, 0f,   // alpha untouched
            )
        )
    )

fun tintFilter(redMul: Float, greenMul: Float, blueMul: Float, alfaMul: Float): ColorFilter =
    ColorFilter.colorMatrix(
        ColorMatrix(
            floatArrayOf(
                redMul, 0f, 0f, 0f, 0f,
                0f, greenMul, 0f, 0f, 0f,
                0f, 0f, blueMul, 0f, 0f,
                0f, 0f, 0f, alfaMul, 0f,
            )
        )
    )