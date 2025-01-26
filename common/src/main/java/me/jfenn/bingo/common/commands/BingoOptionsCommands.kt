package me.jfenn.bingo.common.commands

import me.jfenn.bingo.common.LOBBY_WORLD_ID
import me.jfenn.bingo.common.Permission
import me.jfenn.bingo.common.event.ScopedEvents
import me.jfenn.bingo.common.options.*
import me.jfenn.bingo.common.scope.BingoComponent
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.state.GameState
import me.jfenn.bingo.platform.IServerWorldFactory
import me.jfenn.bingo.platform.commands.CommandBuilder
import me.jfenn.bingo.platform.commands.ICommandManager
import me.jfenn.bingo.platform.commands.IExecutionContext
import me.jfenn.bingo.platform.commands.IExecutionSource
import kotlin.reflect.KMutableProperty1

class BingoOptionsCommands(
    commandManager: ICommandManager,
) : BingoComponent() {

    private fun IExecutionSource.hasConfigureGame() = hasState(GameState.PREGAME, GameState.PLAYING) && hasPermission(Permission.CONFIGURE_GAME)

    private fun IExecutionContext.setGoal(goal: BingoGoal) {
        scope.get<OptionsService>().setGoal(
            ctx = optionsContext,
            goal = goal,
        )
        scope.get<ScopedEvents>().onChangeOptions(Unit)
    }

    private fun IExecutionContext.toggleCardMode(
        prop: KMutableProperty1<BingoCardOptions, Boolean>,
        value: Boolean? = null,
    ) {
        scope.get<OptionsService>().toggleCardMode(
            ctx = optionsContext,
            prop = prop,
            value = value,
        )
        scope.get<ScopedEvents>().onChangeOptions(Unit)
    }

    private fun IExecutionContext.setWinCondition(winCondition: BingoWinCondition) {
        scope.get<OptionsService>().setWinCondition(optionsContext, winCondition)
        scope.get<ScopedEvents>().onChangeOptions(Unit)
    }

    private fun IExecutionContext.setStalemateBehavior(stalemateBehavior: StalemateBehavior) {
        scope.get<OptionsService>().setStalemateBehavior(optionsContext, stalemateBehavior)
        scope.get<ScopedEvents>().onChangeOptions(Unit)
    }

    private fun IExecutionContext.setEndWhen(endWhen: EndWhen) {
        scope.get<OptionsService>().setEndWhen(optionsContext, endWhen)
        scope.get<ScopedEvents>().onChangeOptions(Unit)
    }

    private fun IExecutionContext.setTimeLimit(minutes: Int?) {
        scope.get<OptionsService>().setTimeLimit(optionsContext, minutes)
        scope.get<ScopedEvents>().onChangeOptions(Unit)
    }

    private fun CommandBuilder.executesToggle(callback: IExecutionContext.(Boolean?) -> Unit) {
        executes { callback(null) }
        boolean("enabled") { arg ->
            executes { callback(getArgument(arg)) }
        }
    }

    init {
        commandManager.register("bingo") {
            literal("goal") {
                requires { hasConfigureGame() }
                integer("count", min = 1) { countArg ->
                    literal("items") {
                        executes {
                            val count = getArgument(countArg)
                            setGoal(BingoGoal.Items(count))
                        }
                    }
                    literal("lines") {
                        executes {
                            val count = getArgument(countArg)
                            setGoal(BingoGoal.Lines(count))
                        }
                    }
                }
            }

            literal("mode") {
                requires { hasConfigureGame() }
                literal("lockout") {
                    executesToggle { toggleCardMode(BingoCardOptions::isLockoutMode, it) }
                }
                literal("inventory") {
                    executesToggle { toggleCardMode(BingoCardOptions::isInventoryMode, it) }
                }
                literal("hidden_items") {
                    executesToggle { toggleCardMode(BingoCardOptions::isHiddenItemsMode, it) }
                }
                literal("consume_items") {
                    requires {
                        hasState(GameState.PREGAME, GameState.PLAYING)
                                && hasPermission(Permission.CONFIGURE_GAME)
                                && scope.get<BingoState>().isLobbyMode
                    }
                    executesToggle { toggleCardMode(BingoCardOptions::isConsumeItemsMode, it) }
                }
            }

            literal("options") {
                requires { hasConfigureGame() }

                literal("play_to") {
                    literal("cards") {
                        integer("count", min = 1) { countArg ->
                            executes {
                                val count = getArgument(countArg).coerceAtLeast(1)
                                setWinCondition(BingoWinCondition.Cards(count))
                            }
                        }
                    }

                    literal("infinite_cards") {
                        executes {
                            setWinCondition(BingoWinCondition.Infinite)
                        }
                    }

                    literal("replace_goals") {
                        executes {
                            setWinCondition(BingoWinCondition.ReplaceGoals)
                        }
                    }
                }

                literal("stalemate") {
                    literal("end_game") {
                        executes { setStalemateBehavior(StalemateBehavior.END_GAME) }
                    }
                    literal("reroll_card") {
                        executes { setStalemateBehavior(StalemateBehavior.REROLL_CARD) }
                    }
                    literal("do_nothing") {
                        executes { setStalemateBehavior(StalemateBehavior.NOTHING) }
                    }
                }

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

                literal("pvp") {
                    executesToggle { isPvpEnabled ->
                        scope.get<OptionsService>().togglePvp(optionsContext, isPvpEnabled)
                        scope.get<ScopedEvents>().onChangeOptions.invoke(Unit)
                    }
                }

                literal("elytra") {
                    executesToggle { isElytra ->
                        scope.get<OptionsService>().toggleElytra(optionsContext, isElytra)
                        scope.get<ScopedEvents>().onChangeOptions(Unit)
                    }
                }

                literal("night_vision") {
                    executesToggle { isNightVision ->
                        scope.get<OptionsService>().toggleNightVision(optionsContext, isNightVision)
                        scope.get<ScopedEvents>().onChangeOptions(Unit)
                    }
                }

                literal("preview_card") {
                    requires {
                        hasConfigureGame() && hasState(GameState.PREGAME)
                    }
                    executesToggle { isPreviewCard ->
                        scope.get<OptionsService>().togglePreviewCard(optionsContext, isPreviewCard)
                        scope.get<ScopedEvents>().onChangeOptions.invoke(Unit)
                    }
                }

                literal("spawn_distance") {
                    requires { hasLobby() && hasConfigureGame() }
                    integer("chunks", min = 0) { chunksArg ->
                        executes {
                            val chunks = getArgument(chunksArg)
                            scope.get<OptionsService>().setSpawnDistance(optionsContext, chunks)
                            scope.get<ScopedEvents>().onChangeOptions(Unit)
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

                            scope.get<OptionsService>().setSpawnDimension(optionsContext, dimension)
                            scope.get<ScopedEvents>().onChangeOptions(Unit)
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