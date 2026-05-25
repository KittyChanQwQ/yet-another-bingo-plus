package me.jfenn.bingo.common.game

import me.jfenn.bingo.common.Permission
import me.jfenn.bingo.common.commands.formatWarning
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.integrations.permissions.IPermissionsApi
import me.jfenn.bingo.platform.IPlayerManager
import me.jfenn.bingo.platform.event.IEventBus

internal class GameWarningController(
    private val text: TextProvider,
    private val permissions: IPermissionsApi,
    private val playerManager: IPlayerManager,
    eventBus: IEventBus,
) {
    init {
        eventBus.register(GameWarningEvent) {
            it.warnings.forEachIndexed { index, message ->
                playerManager.getPlayers()
                    .filter { permissions.hasPermission(it, Permission.CONFIGURE_GAME) }
                    .forEach {
                        it.sendMessage(text.formatWarning(message, index == 0))
                    }
            }
        }
    }
}
