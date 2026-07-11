package io.github.mrpotatosse.merkator.views.game

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposeImageBitmap
import io.github.mrpotatosse.merkator.api.HiboukinApi
import io.github.mrpotatosse.merkator.components.game.DofusGameFiller
import io.github.mrpotatosse.merkator.components.game.DofusGameMap
import io.github.mrpotatosse.merkator.models.game.BasicDrawWithBitmap
import io.github.mrpotatosse.merkator.projections.FixtureElementDraw
import io.github.mrpotatosse.merkator.projections.GraphicalElementDraw
import io.github.mrpotatosse.merkator.states.game.DofusGameState
import io.github.mrpotatosse.merkator.utils.game.extractDistinctElementsIds
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageInfo
import kotlin.coroutines.cancellation.CancellationException

@Composable
fun DofusGame() {
    val api = remember { HiboukinApi(baseUrl = "/api") }

    var currentMapId by remember { mutableStateOf(191105026u) }
    var gameState by remember { mutableStateOf<DofusGameState>(DofusGameState.Loading) }
    var drawList by remember { mutableStateOf<List<List<BasicDrawWithBitmap>>>(emptyList()) }

    LaunchedEffect(currentMapId) {
        drawList = emptyList()
        gameState = DofusGameState.Loading
        gameState = runCatching { api.draw(currentMapId) }.fold(
            onSuccess = { information ->
                drawList = withContext(Dispatchers.Default) {
                    val ids = information.extractDistinctElementsIds()
                    val semaphore = Semaphore(16)

                    val images = coroutineScope {
                        ids.chunked(16).map { chunk ->
                            async {
                                semaphore.withPermit {
                                    try {
                                        api.gfxs(chunk).gfxs
                                    } catch (e: CancellationException) {
                                        throw e
                                    } catch (_: Exception) {
                                        emptyMap()
                                    }
                                }
                            }
                        }.awaitAll().fold(mutableMapOf<Int, ByteArray>()) { acc, m -> acc.apply { putAll(m) } }
                    }

                    val bitmaps: Map<Int, ImageBitmap> = coroutineScope {
                        images.map { (id, bytes) ->
                            async(Dispatchers.Default) {
                                val img = Image.makeFromEncoded(bytes)
                                val bitmap = Bitmap().apply {
                                    allocPixels(ImageInfo.makeN32Premul(img.width, img.height))
                                }
                                img.readPixels(bitmap, 0, 0)
                                img.close()
                                bitmap.setImmutable()
                                val composeBitmap = bitmap.asComposeImageBitmap()

                                id to composeBitmap
                            }
                        }.awaitAll().toMap()
                    }

                    information.elements.map { elementDraws ->
                        elementDraws.mapNotNull { element ->
                            val id = when (element) {
                                is FixtureElementDraw -> element.fixtureId
                                is GraphicalElementDraw -> element.gfxId
                            }
                            bitmaps[id]?.let { BasicDrawWithBitmap(element, it) }
                        }
                    }
                }
                DofusGameState.Ready(information)
            },
            onFailure = { DofusGameState.Error(it.message ?: "unknown error") }
        )
    }

    when (val state = gameState) {
        is DofusGameState.Error -> DofusGameFiller()
        is DofusGameState.Loading -> DofusGameFiller()
        is DofusGameState.Ready -> DofusGameMap(
            drawList = drawList,
            state = state
        )
    }
}