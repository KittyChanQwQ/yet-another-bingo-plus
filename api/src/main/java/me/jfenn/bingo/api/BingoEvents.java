package me.jfenn.bingo.api;

import me.jfenn.bingo.api.event.EventListener;
import me.jfenn.bingo.api.event.GameEndedEvent;
import me.jfenn.bingo.api.event.GameStartedEvent;
import me.jfenn.bingo.api.event.TeamChangedEvent;
import me.jfenn.bingo.api.internal.ExtensionManager;
import me.jfenn.bingo.api.state.GameState;
import me.jfenn.bingo.api.state.GameStateChangedEvent;

public class BingoEvents {
    /**
     * Invoked in SERVER_STARTED, after the bingo API has been initialized.
     */
    public static final EventListener<IBingoApi> INIT = new EventListener<>("INIT");
    /**
     * Invoked in SERVER_STOPPED, when the bingo API is removed.
     */
    public static final EventListener<Void> CLOSE = new EventListener<>("CLOSE");
    /**
     * Invoked when a round begins the loading/countdown.
     */
    public static final EventListener<Void> GAME_STARTING = new EventListener<>("GAME_STARTING");
    /**
     * Invoked when a round is started.
     */
    public static final EventListener<GameStartedEvent> GAME_STARTED = new EventListener<>("GAME_STARTED");
    /**
     * Invoked when a round ends.
     */
    public static final EventListener<GameEndedEvent> GAME_ENDED = new EventListener<>("GAME_ENDED");
    /**
     * Invoked in lobby mode when the world is reset and a new seed is applied.
     */
    public static final EventListener<Void> GAME_RESET = new EventListener<>("GAME_RESET");
    /**
     * Invoked whenever a player changes team.
     */
    public static final EventListener<TeamChangedEvent> TEAM_CHANGED = new EventListener<>("TEAM_CHANGED");
    /**
     * Invoked on any change to the game state.
     */
    public static final EventListener<GameStateChangedEvent> STATE_CHANGED = new EventListener<>("STATE_CHANGED");

    /**
     * Listen for when the game enters a particular game state.
     */
    public static EventListener<GameStateChangedEvent> onStateChanged(GameState state) {
        var consumers = ExtensionManager.INSTANCE.getGameStateListeners();
        var listener = consumers.get(state);
        if (listener == null) {
            listener = new EventListener<>("STATE_CHANGED:" + state.getIdentifier());
            consumers.put(state, listener);
        }
        return listener;
    }
}
