package io.github.mrpotatosse.merkator

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.tooling.preview.Preview
import io.github.mrpotatosse.merkator.utils.LocalLoadingBackground
import io.github.mrpotatosse.merkator.views.game.DofusGame
import merkator.app.shared.generated.resources.Res
import merkator.app.shared.generated.resources.loading_background
import org.jetbrains.compose.resources.painterResource

@OptIn(ExperimentalComposeUiApi::class)
@Composable
@Preview
fun App() {
    val background = painterResource(Res.drawable.loading_background)
    
    CompositionLocalProvider(LocalLoadingBackground provides background) {
        DofusGame()
    }
}