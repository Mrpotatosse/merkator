package io.github.mrpotatosse.merkator.states.game

import io.github.mrpotatosse.merkator.projections.MapDrawInformation

sealed interface DofusGameState {
    data object Loading : DofusGameState
    data class Ready(val info: MapDrawInformation) : DofusGameState
    data class Error(val message: String) : DofusGameState
}