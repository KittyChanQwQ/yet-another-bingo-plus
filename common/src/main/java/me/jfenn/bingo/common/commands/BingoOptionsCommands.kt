package me.jfenn.bingo.common.commands

import me.jfenn.bingo.api.IPlayerManager
import me.jfenn.bingo.api.commands.ICommandManager
import me.jfenn.bingo.api.commands.IExecutionContext
import me.jfenn.bingo.api.commands.IExecutionSource
import me.jfenn.bingo.common.Permission
import me.jfenn.bingo.common.config.BingoGoal
import me.jfenn.bingo.common.config.BingoOptions
import me.jfenn.bingo.common.event.ScopedEvents
import me.jfenn.bingo.common.scope.BingoComponent
import me.jfenn.bingo.common.spawn.ElytraService
import me.jfenn.bingo.common.spawn.PlayerController
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.state.GameState
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.generated.StringKey
import net.minecraft.text.Text
import net.minecraft.util.Formatting

class BingoOptionsCommands(
    commandManager: ICommandManager,
    private val textProvider: TextProvider,
) : BingoComponent() {

    private fun IExecutionSource.hasConfigureGame() = hasState(GameState.PREGAME, GameState.PLAYING) && hasPermission(Permission.CONFIGURE_GAME)

    private fun IExecutionContext.setGoal(goal: BingoGoal) {
        val state = scope.get<BingoState>()
        state.options.cardGoal = goal

        val events = scope.get<ScopedEvents>()
        events.onChangeOptions(Unit)

        // Send a message that the goal was changed
        Text.literal("ℹ  ")
            .append(textProvider.string(StringKey.ScoreboardGoal, goal.format(textProvider)))
            .formatted(Formatting.AQUA)
            .also { sendFeedback(it) }
    }

    private fun IExecutionContext.toggleElytra() {
        val options = scope.get<BingoOptions>()
        val events = scope.get<ScopedEvents>()
        val playerManager = scope.get<IPlayerManager>()
        val elytraService = scope.get<ElytraService>()

        options.isElytra = !options.isElytra
        events.onChangeOptions(Unit)

        // if Elytra mode is turned off during a game, remove all players' elytra
        if (!options.isElytra) {
            for (player in playerManager.getPlayers()) {
                elytraService.takeElytra(player)
            }
        }

        Text.literal("ℹ  ")
            .append(textProvider.string(StringKey.OptionsElytra))
            .append(": ${options.isElytra}")
            .formatted(Formatting.AQUA)
            .also { sendFeedback(it) }
    }

    private fun IExecutionContext.toggleNightVision() {
        val state = scope.get<BingoState>()
        val options = scope.get<BingoOptions>()
        val events = scope.get<ScopedEvents>()
        val playerController = scope.get<PlayerController>()

        options.isNightVision = !options.isNightVision
        events.onChangeOptions(Unit)

        // if night vision is changed, update all players
        if (state.state == GameState.PLAYING) {
            server.playerManager.playerList.forEach { player ->
                playerController.updateGameMode(player, forceReset = false)
            }
        }

        Text.literal("ℹ  ")
            .append(textProvider.string(StringKey.OptionsNightVis))
            .append(": ${options.isNightVision}")
            .formatted(Formatting.AQUA)
            .also { sendFeedback(it) }
    }

    init {
        commandManager.register("bingo") {
            literal("goal") {
                requires { hasConfigureGame() }
                integer("count", min = 1, max = 25) { countArg ->
                    literal("items") {
                        executes {
                            val count = getArgument(countArg).coerceIn(1, BingoGoal.MAX_ITEMS)
                            setGoal(BingoGoal.Items(count))
                        }
                    }
                    literal("lines") {
                        executes {
                            val count = getArgument(countArg).coerceIn(1, BingoGoal.MAX_LINES)
                            setGoal(BingoGoal.Lines(count))
                        }
                    }
                }
            }

            literal("elytra") {
                requires { hasConfigureGame() }
                executes {
                    toggleElytra()
                }
            }

            literal("night_vision") {
                requires { hasConfigureGame() }
                executes {
                    toggleNightVision()
                }
            }
        }
    }
}