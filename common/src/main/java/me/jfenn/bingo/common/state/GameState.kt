package me.jfenn.bingo.common.state

import me.jfenn.bingo.api.data.BingoGameStatus
import me.jfenn.bingo.api.state.GameState

val GameState.isInLobby get() = status == BingoGameStatus.PREGAME

val GameState.isPlayingOrCountdown get() = when (status) {
    BingoGameStatus.STARTING -> true
    BingoGameStatus.PLAYING -> true
    else -> false
}

val GameState.canStartNewRound get() = when (this) {
    GameState.PREGAME, GameState.STARTING_NEXT_ROUND -> true
    else -> false
}
