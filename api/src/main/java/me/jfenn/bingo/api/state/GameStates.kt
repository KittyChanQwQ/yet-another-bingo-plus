package me.jfenn.bingo.api.state

import kotlinx.serialization.Serializable
import me.jfenn.bingo.api.data.BingoGameStatus

@Serializable
class GameState(
    /**
     * A unique identifier for each game state.
     *
     * This is normalized into alphanumeric characters and used to invoke function tags, scoreboard templates, etc.
     *
     * e.g. "mymod:custom_state" will become "#yet-another-minecraft-bingo:on_mymod_custom_state"
     */
    val identifier: String,
    /**
     * A simplified game status that indicates the round lifecycle (pregame, playing, postgame).
     *
     * This is used for most command permission checks, player gamemodes, voice groups, etc.
     */
    val status: BingoGameStatus,
) {
    override fun equals(other: Any?) = other is GameState && other.identifier == identifier
    override fun hashCode() = identifier.hashCode()
    override fun toString(): String = identifier

    companion object {
        /** This state shouldn't show up in actual use; it's immediately changed from the default in ScopeManager */
        @JvmField
        val UNINITIALIZED = GameState("uninitialized", BingoGameStatus.PREGAME)
        /** Used in the lobby (or otherwise when game is not started). Players can pick teams and edit the game options. */
        @JvmField
        val PREGAME = GameState("pregame", BingoGameStatus.PREGAME)
        /** Used in the "/bingo lobby edit" mode when players are editing the lobby in creative mode. */
        @JvmField
        val LOBBY_EDITOR =  GameState("lobby_editor", BingoGameStatus.PREGAME)
        /** Used once the start button is pressed and the game options are established, while the game locates a spawnpoint for each team. */
        @JvmField
        val STARTING = GameState("starting", BingoGameStatus.PREGAME)
        /** Used when the team spawnpoints have been chosen, and the view distance around each spawnpoint is being preloaded. */
        @JvmField
        val PRELOADING = GameState("preloading", BingoGameStatus.PREGAME)
        /** Used when players are being teleported into the world before starting the countdown. */
        @JvmField
        val LOADING = GameState("loading", BingoGameStatus.STARTING)
        /** Used when players have loaded into the world, and the countdown has started. */
        @JvmField
        val COUNTDOWN = GameState("countdown", BingoGameStatus.STARTING)
        /** Used while the game is in progress. */
        @JvmField
        val PLAYING = GameState("playing", BingoGameStatus.PLAYING)
        /** Used when the game ends, and players are viewing the game over screen or spectating the world. */
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

    /**
     * Returns the state that should occur after [current].
     */
    fun getNext(current: GameState) = states[current] ?: default

    /**
     * Insert a new state into the sequence.
     */
    fun insertAfter(previous: GameState, next: GameState) {
        val target = getNext(previous)
        states[previous] = next
        states[next] = target
    }

    companion object {
        /**
         * The sequence of states used when starting a game.
         * Typically starts from [GameState.PREGAME], and ends with [GameState.PLAYING].
         */
        @JvmField
        val START = GameAction(
            GameState.STARTING,
            GameState.STARTING to GameState.PRELOADING,
            GameState.PRELOADING to GameState.LOADING,
            GameState.LOADING to GameState.COUNTDOWN,
            GameState.COUNTDOWN to GameState.PLAYING,
        )

        /**
         * The sequence of states used when ending a game.
         * Typically starts from [GameState.PLAYING], and ends with [GameState.POSTGAME].
         */
        @JvmField
        val END = GameAction(GameState.POSTGAME)
    }
}
