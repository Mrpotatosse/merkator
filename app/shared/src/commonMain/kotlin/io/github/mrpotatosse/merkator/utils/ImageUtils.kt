package io.github.mrpotatosse.merkator.utils

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.painter.Painter

val LocalLoadingBackground = staticCompositionLocalOf<Painter> {
    error("LocalLoadingBackground not provided")
}