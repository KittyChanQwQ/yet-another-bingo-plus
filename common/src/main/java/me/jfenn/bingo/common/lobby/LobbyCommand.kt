package me.jfenn.bingo.common.lobby

import me.jfenn.bingo.common.Permission
import me.jfenn.bingo.common.commands.hasLobby
import me.jfenn.bingo.common.commands.hasPermission
import me.jfenn.bingo.common.commands.hasState
import me.jfenn.bingo.common.commands.runByName
import me.jfenn.bingo.common.config.BingoConfig
import me.jfenn.bingo.common.reset.ResetService
import me.jfenn.bingo.common.state.GameState
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.generated.StringKey
import me.jfenn.bingo.platform.IExecutors
import me.jfenn.bingo.platform.IModEnvironment
import me.jfenn.bingo.platform.IPlayerHandle
import me.jfenn.bingo.platform.commands.ICommandManager
import me.jfenn.bingo.platform.commands.IExecutionContext
import me.jfenn.bingo.platform.commands.IExecutionSource
import me.jfenn.bingo.platform.dialog.IDialogAction
import me.jfenn.bingo.platform.dialog.IDialogManager
import me.jfenn.bingo.platform.text.Formatting
import java.util.*

internal class BingoLobbyCommand(
    commandManager: ICommandManager,
    environment: IModEnvironment,
    config: BingoConfig,
    private val text: TextProvider,
) {
    private val IExecutionSource.lobbyModeService get() = scope.get<LobbyModeService>()

    private val emptyUuid = UUID.fromString("00000000-0000-0000-0000-000000000000")

    private val IExecutionSource.executorUuid get() = when {
        isConsole -> emptyUuid
        else -> playerOrThrow.uuid
    }

    private fun IExecutionContext.createLobbyDialog(player: IPlayerHandle): Boolean {
        val dialogManager = scope.get<IDialogManager>()
        val builder = dialogManager.confirmationBuilder()
            ?: return false

        builder.title = text.string(StringKey.FullName)
        lobbyModeService.getWarnings().forEach { builder.addText(it) }

        builder.setYes(
            text.translatable("gui.yes", "Yes"),
            IDialogAction.RunCommand(CONFIRM_COMMAND)
        )
        builder.setNo(
            text.translatable("gui.no", "No"),
            IDialogAction.None
        )

        dialogManager.showDialog(player, builder.build())
        return true
    }

    private fun IExecutionSource.canUseReset(): Boolean {
        return hasPermission(Permission.COMMAND_RESET) &&
                hasState(GameState.PLAYING, GameState.POSTGAME) &&
                hasLobby()
    }

    private fun IExecutionSource.canUseEdit(): Boolean {
        return hasPermission(Permission.CONFIGURE_LOBBY) &&
                hasLobby()
    }

    private fun IExecutionContext.reset() {
        val resetService = scope.get<ResetService>()
        val serverTaskExecutor = scope.get<IExecutors.IServerTaskExecutor>()
        sendFeedback(text.string(StringKey.CommandResetSuccess))
        serverTaskExecutor.execute {
            resetService.resetGame()
        }
    }

    private fun IExecutionContext.edit() {
        val resetService = scope.get<ResetService>()
        val serverTaskExecutor = scope.get<IExecutors.IServerTaskExecutor>()
        sendFeedback(text.string(StringKey.LobbyEditMessage1Entering))
        serverTaskExecutor.execute {
            resetService.resetGame(GameState.LOBBY_EDITOR)
            sendFeedback(text.string(StringKey.LobbyEditMessage2KeepChanges, SAVE_COMMAND).formatted(Formatting.YELLOW))
            sendFeedback(text.string(StringKey.LobbyEditMessage3Return, CANCEL_COMMAND).formatted(Formatting.YELLOW))
        }
    }

    private fun IExecutionContext.save() {
        val lobbyWorldService = scope.get<LobbyWorldService>()
        val resetService = scope.get<ResetService>()
        val serverTaskExecutor = scope.get<IExecutors.IServerTaskExecutor>()
        sendFeedback(text.string(StringKey.LobbyEditSaving))
        serverTaskExecutor.execute {
            lobbyWorldService.writeLobbyFiles()
            resetService.resetGame(GameState.PREGAME)
        }
    }

    init {
        commandManager.register("bingo") {
            literal("reset") {
                requires { canUseReset() }
                executes { reset() }
            }

            literal("lobby") {
                requires {
                    hasPermission(Permission.CONFIGURE_LOBBY)
                }
                executes {
                    if (environment.envType == IModEnvironment.EnvType.SERVER && !config.server.isLobbyMode) {
                        val isDialog = player?.let { createLobbyDialog(it) }
                        if (isDialog != true) {
                            lobbyModeService.getCommandWarnings()
                                .forEach { sendMessage(it) }
                        }
                        lobbyModeService.onPlayerWarned(executorUuid)
                    } else if (canUseReset()) {
                        reset()
                    }
                }

                literal("confirm") {
                    requires {
                        if (environment.envType == IModEnvironment.EnvType.SERVER && !config.server.isLobbyMode) {
                            isConsole || lobbyModeService.canUseLobbyCommand(playerOrThrow)
                        } else false
                    }
                    executes {
                        lobbyModeService
                            .acceptWarnings(executorUuid, runByName)
                            .forEach { sendMessage(it) }
                    }
                }

                literal("edit") {
                    requires { canUseEdit() && hasState(GameState.PREGAME) }
                    executes { edit() }
                }

                literal("save") {
                    requires { canUseEdit() && hasState(GameState.LOBBY_EDITOR) }
                    executes { save() }
                }

                literal("cancel") {
                    requires { canUseEdit() && hasState(GameState.LOBBY_EDITOR) }
                    executes { reset() }
                }
            }
        }
    }

    companion object {
        const val LOBBY_COMMAND = "/bingo lobby"
        const val CONFIRM_COMMAND = "/bingo lobby confirm"
        const val SAVE_COMMAND = "/bingo lobby save"
        const val CANCEL_COMMAND = "/bingo lobby cancel"
    }
}