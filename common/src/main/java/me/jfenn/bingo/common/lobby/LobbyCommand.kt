package me.jfenn.bingo.common.lobby

import me.jfenn.bingo.common.commands.runByName
import me.jfenn.bingo.common.config.BingoConfig
import me.jfenn.bingo.platform.commands.ICommandManager
import me.jfenn.bingo.platform.commands.IExecutionSource
import net.fabricmc.api.EnvType
import net.fabricmc.loader.api.FabricLoader
import java.util.*

internal class BingoLobbyCommand(
    commandManager: ICommandManager,
    config: BingoConfig,
) {
    private val IExecutionSource.lobbyModeService get() = scope.get<LobbyModeService>()

    private val emptyUuid = UUID.fromString("00000000-0000-0000-0000-000000000000")

    private val IExecutionSource.executorUuid get() = when {
        isConsole -> emptyUuid
        else -> playerOrThrow.uuid
    }

    init {
        if (FabricLoader.getInstance().environmentType == EnvType.SERVER && !config.server.isLobbyMode) {
            commandManager.register("bingo") {
                literal("lobby") {
                    requires {
                        isConsole || lobbyModeService.canUseLobbyCommand(playerOrThrow)
                    }
                    executes {
                        lobbyModeService
                            .sendWarnings(executorUuid)
                            .forEach { sendMessage(it) }
                    }

                    literal("confirm") {
                        executes {
                            lobbyModeService
                                .acceptWarnings(executorUuid, runByName)
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