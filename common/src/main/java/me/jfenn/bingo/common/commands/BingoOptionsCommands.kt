package me.jfenn.bingo.common.commands

import me.jfenn.bingo.common.LOBBY_WORLD_ID
import me.jfenn.bingo.common.Permission
import me.jfenn.bingo.common.config.BingoGoal
import me.jfenn.bingo.common.config.BingoOptions
import me.jfenn.bingo.common.config.BingoWinCondition
import me.jfenn.bingo.common.config.EndWhen
import me.jfenn.bingo.common.event.ScopedEvents
import me.jfenn.bingo.common.map.CardViewService
import me.jfenn.bingo.common.scope.BingoComponent
import me.jfenn.bingo.common.spawn.ElytraService
import me.jfenn.bingo.common.spawn.PlayerController
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.state.GameState
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.generated.StringKey
import me.jfenn.bingo.platform.IPlayerManager
import me.jfenn.bingo.platform.IServerWorldFactory
import me.jfenn.bingo.platform.commands.ICommandManager
import me.jfenn.bingo.platform.commands.IExecutionContext
import me.jfenn.bingo.platform.commands.IExecutionSource
import net.minecraft.text.Text
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

class BingoOptionsCommands(
    commandManager: ICommandManager,
    private val textProvider: TextProvider,
) : BingoComponent() {

    private fun IExecutionSource.hasConfigureGame() = hasState(GameState.PREGAME, GameState.PLAYING) && hasPermission(Permission.CONFIGURE_GAME)

    private fun IExecutionContext.changeOption(callback: (BingoOptions) -> Unit): BingoOptions {
        val options = scope.get<BingoOptions>()
        callback(options)

        val events = scope.get<ScopedEvents>()
        events.onChangeOptions(Unit)

        return options
    }

    private fun IExecutionContext.setGoal(goal: BingoGoal) {
        val cardViewService = scope.get<CardViewService>()
        val card = cardViewService.getPlayerCard(player!!)
        card.options.goal = goal

        val events = scope.get<ScopedEvents>()
        events.onChangeOptions(Unit)

        // Send a message that the goal was changed
        sendInfoFeedback(
            textProvider.string(StringKey.ScoreboardGoal, goal.format(textProvider))
        )
    }

    private fun IExecutionContext.setGoalCards(cards: Int) {
        val options = scope.get<BingoOptions>()
        options.winCondition = BingoWinCondition.Cards(cards)

        val events = scope.get<ScopedEvents>()
        events.onChangeOptions(Unit)

        // Send a message that the goal was changed
        sendInfoFeedback(
            textProvider.string(StringKey.ScoreboardGoal, textProvider.cardCount(cards))
        )
    }

    private fun IExecutionContext.setEndWhen(endWhen: EndWhen) {
        changeOption { it.endGameWhen = endWhen }
        sendInfoFeedback(
            textProvider.string(StringKey.OptionsEndWhen, endWhen.string(textProvider))
        )
    }

    private fun IExecutionContext.toggleElytra() {
        val playerManager = scope.get<IPlayerManager>()
        val elytraService = scope.get<ElytraService>()

        val options = changeOption { it.isElytra = !it.isElytra }

        // if Elytra mode is turned off during a game, remove all players' elytra
        if (!options.isElytra) {
            for (player in playerManager.getPlayers()) {
                elytraService.takeElytra(player)
            }
        }

        sendInfoFeedback(
            Text.empty()
                .append(textProvider.string(StringKey.OptionsElytra))
                .append(": ${options.isElytra}")
        )
    }

    private fun IExecutionContext.toggleNightVision() {
        val state = scope.get<BingoState>()
        val playerController = scope.get<PlayerController>()
        val playerManager = scope.get<IPlayerManager>()

        val options = changeOption { it.isNightVision = !it.isNightVision }

        // if night vision is changed, update all players
        if (state.state == GameState.PLAYING) {
            playerManager.getPlayers().forEach { player ->
                playerController.updateGameMode(player, forceReset = false)
            }
        }

        sendInfoFeedback(
            Text.empty()
                .append(textProvider.string(StringKey.OptionsNightVis))
                .append(": ${options.isNightVision}")
        )
    }

    private fun IExecutionContext.setTimeLimit(minutes: Int?) {
        val state = scope.get<BingoState>()
        changeOption {
            it.timeLimit = minutes?.minutes?.plus(state.ingameDuration() ?: Duration.ZERO)
        }

        sendInfoFeedback(
            textProvider.string(StringKey.ScoreboardTimeLeft, state.formatTimeRemaining(textProvider))
        )
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
                    literal("cards") {
                        executes {
                            val count = getArgument(countArg).coerceAtLeast(1)
                            setGoalCards(count)
                        }
                    }
                }
            }

            literal("options") {
                requires { hasConfigureGame() }
                literal("end_when") {
                    literal("never") {
                        executes { setEndWhen(EndWhen.Never) }
                    }
                    literal("first_win") {
                        executes { setEndWhen(EndWhen.FirstWin) }
                    }
                    literal("teams_win") {
                        integer("teams", min = 2) { teamsArg ->
                            executes { setEndWhen(EndWhen.TeamsWin(getArgument(teamsArg))) }
                        }
                    }
                    literal("all_win") {
                        executes { setEndWhen(EndWhen.AllWin) }
                    }
                }

                literal("elytra") {
                    executes { toggleElytra() }
                }

                literal("night_vision") {
                    executes { toggleNightVision() }
                }

                literal("preview_card") {
                    requires {
                        hasConfigureGame() && hasState(GameState.PREGAME)
                    }
                    executes {
                        val options = scope.get<BingoOptions>()
                        options.showPreviewCard = !options.showPreviewCard

                        val events = scope.get<ScopedEvents>()
                        events.onChangeOptions.invoke(Unit)

                        sendInfoFeedback(
                            textProvider.string(StringKey.OptionsPreviewCard)
                                .append(": ${options.showPreviewCard}")
                        )
                    }
                }

                literal("spawn_distance") {
                    requires { hasLobby() && hasConfigureGame() }
                    integer("chunks", min = 0) { chunksArg ->
                        executes {
                            val chunks = getArgument(chunksArg)
                            changeOption { it.spawnDistance = chunks }
                            textProvider.string(StringKey.OptionsSpawnDistance)
                                .append(": $chunks")
                                .let { sendInfoFeedback(it) }
                        }
                    }
                }

                literal("spawn_dimension") {
                    requires { hasLobby() && hasConfigureGame() }
                    string(
                        name = "dimension",
                        suggestions = {
                            scope.get<IServerWorldFactory>()
                                .listWorlds(server)
                                .map { it.identifier }
                                .minus(LOBBY_WORLD_ID.toString())
                        },
                        greedy = true,
                    ) { dimensionArg ->
                        executes {
                            val dimension = getArgument(dimensionArg)

                            scope.get<IServerWorldFactory>()
                                .listWorlds(server)
                                .find { it.identifier == dimension }
                                ?: error("Dimension '$dimension' does not exist!")

                            changeOption { it.spawnDimension = dimension }
                            textProvider.string(StringKey.OptionsSpawnDimension)
                                .append(": $dimension")
                                .let { sendInfoFeedback(it) }
                        }
                    }
                }
            }

            literal("timelimit") {
                requires { hasConfigureGame() }
                integer("minutes", min = 1) { minutesArg ->
                    executes { setTimeLimit(getArgument(minutesArg)) }
                }
                literal("off") {
                    executes { setTimeLimit(null) }
                }
            }
        }
    }

    companion object {
        const val END_WHEN = "/bingo options end_when"
    }
}