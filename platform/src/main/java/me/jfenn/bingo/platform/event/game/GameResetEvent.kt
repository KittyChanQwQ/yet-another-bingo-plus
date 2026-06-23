package me.jfenn.bingo.platform.event.game

import me.jfenn.bingo.platform.event.IEvent

object GameResetEvent : IEvent<Unit> {
    object BeforeClose : IEvent<Unit>
    object Copy : IEvent<Unit>
}