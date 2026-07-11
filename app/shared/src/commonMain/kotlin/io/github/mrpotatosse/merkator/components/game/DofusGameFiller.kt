package io.github.mrpotatosse.merkator.components.game

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import io.github.mrpotatosse.merkator.utils.LocalLoadingBackground

@Composable
fun DofusGameFiller() {
    val background = LocalLoadingBackground.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = background,
            contentDescription = "Background image",
            contentScale = ContentScale.Fit
        )
    }
}