package me.jfenn.bingo.common.scope

import org.slf4j.Logger

abstract class BingoComponent {
    init {
        BingoKoin.koinApp.koin
            .get<Logger>()
            .debug("Initialize [${this::class.simpleName}]")
    }
}