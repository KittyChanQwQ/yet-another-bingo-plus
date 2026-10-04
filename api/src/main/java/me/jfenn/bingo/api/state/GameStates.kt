package me.jfenn.bingo.api.state

import kotlinx.serialization.Serializable
import me.jfenn.bingo.api.data.BingoGameStatus

@Serializable
class GameState(
    val identifier: String,
    val status: BingoGameStatus,
) {
    override fun equals(other: Any?) = other is GameState && other.identifier == identifier
    override fun hashCode() = identifier.hashCode()
    override fun toString(): String = identifier

    companion object {
        /** This state shouldn't show up in actual use; it's immediately changed from the default in ScopeManager */
        @JvmField
        val UNINITIALIZED = GameState("uninitialized", BingoGameStatus.PREGAME)
        @JvmField
        val PREGAME = GameState("pregame", BingoGameStatus.PREGAME)
        @JvmField
        val LOBBY_EDITOR =  GameState("lobby_editor", BingoGameStatus.PREGAME)
        @JvmField
        val STARTING = GameState("starting", BingoGameStatus.PREGAME)
        @JvmField
        val PRELOADING = GameState("preloading", BingoGameStatus.PREGAME)
        @JvmField
        val LOADING = GameState("loading", BingoGameStatus.STARTING)
        @JvmField
        val COUNTDOWN = GameState("countdown", BingoGameStatus.STARTING)
        @JvmField
        val PLAYING = GameState("playing", BingoGameStatus.PLAYING)
        @JvmField
        val POSTGAME = GameState("postgame", BingoGameStatus.POSTGAME)
        /** Used when "Next Round" is clicked on the end menu */
        @JvmField
        val STARTING_NEXT_ROUND = GameState("starting_next_round", BingoGameStatus.POSTGAME)
    }
}

class GameAction(
    internal val default: GameState,
    vararg states: Pair<GameState, GameState>
) {
    internal val states = mutableMapOf(*states)

    fun getNext(current: GameState) = states[current] ?: default

    fun insertAfter(previous: GameState, next: GameState) {
        val target = getNext(previous)
        states[previous] = next
        states[next] = target
    }

    companion object {
        @JvmField
        val START = GameAction(
            GameState.STARTING,
            GameState.STARTING to GameState.PRELOADING,
            GameState.PRELOADING to GameState.LOADING,
            GameState.LOADING to GameState.COUNTDOWN,
            GameState.COUNTDOWN to GameState.PLAYING,
        )

        @JvmField
        val END = GameAction(GameState.POSTGAME)
    }
}
