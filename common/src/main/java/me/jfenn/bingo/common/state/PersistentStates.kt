package me.jfenn.bingo.common.state

import me.jfenn.bingo.common.MOD_ID_BINGO
import me.jfenn.bingo.common.config.ConfigService

internal class PersistentStates(
    persistentStateManager: PersistentStateManager,
    private val configService: ConfigService,
) {
    val bingo = persistentStateManager.register<BingoState>(MOD_ID_BINGO) {
        BingoState(options = configService.options)
    }
}