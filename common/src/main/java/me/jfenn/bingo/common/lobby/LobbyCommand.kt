package me.jfenn.bingo.common.lobby

import me.jfenn.bingo.common.config.BingoConfig
import me.jfenn.bingo.platform.commands.ICommandManager
import me.jfenn.bingo.platform.commands.IExecutionSource
import net.fabricmc.api.EnvType
import net.fabricmc.loader.api.FabricLoader

internal class BingoLobbyCommand(
    commandManager: ICommandManager,
    config: BingoConfig,
) {
    private val IExecutionSource.lobbyModeService get() = scope.get<LobbyModeService>()

    init {
        if (FabricLoader.getInstance().environmentType == EnvType.SERVER && !config.server.isLobbyMode) {
            commandManager.register("bingo") {
                literal("lobby") {
                    requires {
                        lobbyModeService.canUseLobbyCommand(player!!)
                    }
                    executes {
                        lobbyModeService
                            .sendWarnings(player!!)
                            .forEach { sendMessage(it) }
                    }

                    literal("confirm") {
                        executes {
                            lobbyModeService
                                .acceptWarnings(player!!)
                                .forEach { sendMessage(it) }
                        }
                    }
                }
            }
        }
    }

    companion object {
        const val LOBBY_COMMAND = "/bingo lobby"
        const val CONFIRM_COMMAND = "/bingo lobby confirm"
    }
}