package me.jfenn.bingo.common.lobby

import me.jfenn.bingo.platform.commands.ICommandManager
import me.jfenn.bingo.common.config.BingoConfig
import net.fabricmc.api.EnvType
import net.fabricmc.loader.api.FabricLoader

internal class BingoLobbyCommand(
    commandManager: ICommandManager,
    config: BingoConfig,
) {
    init {
        if (FabricLoader.getInstance().environmentType == EnvType.SERVER && !config.server.isLobbyMode) {
            commandManager.register("bingo") {
                literal("lobby") {
                    requires { scope.get<LobbyModeService>().canUseLobbyCommand(player!!) }
                    executes {
                        scope.get<LobbyModeService>()
                            .sendWarnings(player!!)
                            .forEach { sendMessage(it) }
                    }

                    literal("confirm") {
                        executes {
                            scope.get<LobbyModeService>()
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