package me.jfenn.bingo.api;

import me.jfenn.bingo.api.event.EventListener;
import me.jfenn.bingo.api.event.GameEndedEvent;
import me.jfenn.bingo.api.event.GameStartedEvent;

public class BingoEvents {
    public static final EventListener<GameStartedEvent> GAME_STARTED = new EventListener<>();
    public static final EventListener<GameEndedEvent> GAME_ENDED = new EventListener<>();
}
