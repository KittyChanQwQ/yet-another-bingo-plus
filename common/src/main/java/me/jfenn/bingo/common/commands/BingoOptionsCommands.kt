package me.jfenn.bingo.common.commands

import me.jfenn.bingo.common.LOBBY_WORLD_ID
import me.jfenn.bingo.common.Permission
import me.jfenn.bingo.common.event.ScopedEvents
import me.jfenn.bingo.common.options.*
import me.jfenn.bingo.common.scope.BingoComponent
import me.jfenn.bingo.common.state.GameState
import me.jfenn.bingo.platform.IServerWorldFactory
import me.jfenn.bingo.platform.commands.ICommandManager
import me.jfenn.bingo.platform.commands.IExecutionContext
import me.jfenn.bingo.platform.commands.IExecutionSource

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

    private fun IExecutionContext.setGoalCards(cards: Int) {
        scope.get<OptionsService>().setWinCondition(optionsContext, BingoWinCondition.Cards(cards))
        scope.get<ScopedEvents>().onChangeOptions(Unit)
    }

    private fun IExecutionContext.setEndWhen(endWhen: EndWhen) {
        scope.get<OptionsService>().setEndWhen(optionsContext, endWhen)
        scope.get<ScopedEvents>().onChangeOptions(Unit)
    }

    private fun IExecutionContext.toggleElytra() {
        scope.get<OptionsService>().toggleElytra(optionsContext)
        scope.get<ScopedEvents>().onChangeOptions(Unit)
    }

    private fun IExecutionContext.toggleNightVision() {
        scope.get<OptionsService>().toggleNightVision(optionsContext)
        scope.get<ScopedEvents>().onChangeOptions(Unit)
    }

    private fun IExecutionContext.setTimeLimit(minutes: Int?) {
        scope.get<OptionsService>().setTimeLimit(optionsContext, minutes)
        scope.get<ScopedEvents>().onChangeOptions(Unit)
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

                literal("pvp") {
                    executes {
                        scope.get<OptionsService>().togglePvp(optionsContext)
                        scope.get<ScopedEvents>().onChangeOptions.invoke(Unit)
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
                        scope.get<OptionsService>().togglePreviewCard(optionsContext)
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