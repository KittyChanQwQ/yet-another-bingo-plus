package me.jfenn.bingo.common.commands

import me.jfenn.bingo.api.state.GameState
import me.jfenn.bingo.api.options.BingoGoal
import me.jfenn.bingo.api.options.BingoWinCondition
import me.jfenn.bingo.api.options.EndWhen
import me.jfenn.bingo.api.options.StalemateBehavior
import me.jfenn.bingo.common.LOBBY_WORLD_ID
import me.jfenn.bingo.common.Permission
import me.jfenn.bingo.common.options.BingoCardOptions
import me.jfenn.bingo.common.options.LocatorBarMode
import me.jfenn.bingo.common.options.withOptionsService
import me.jfenn.bingo.common.scope.BingoComponent
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
        withOptionsService {
            it.setGoal(goal = goal)
        }
    }

    private fun IExecutionContext.toggleCardMode(
        prop: KMutableProperty1<BingoCardOptions, Boolean>,
        value: Boolean? = null,
    ) {
        withOptionsService {
            it.toggleCardMode(
                prop = prop,
                value = value,
            )
        }
    }

    private fun IExecutionContext.setWinCondition(winCondition: BingoWinCondition) {
        withOptionsService {
            it.winCondition = winCondition
        }
    }

    private fun IExecutionContext.setStalemateBehavior(stalemateBehavior: StalemateBehavior) {
        withOptionsService {
            it.stalemateBehavior = stalemateBehavior
        }
    }

    private fun IExecutionContext.setLocatorBarMode(locatorBarMode: LocatorBarMode) {
        withOptionsService {
            it.locatorBarMode = locatorBarMode
        }
    }

    private fun IExecutionContext.setEndWhen(endWhen: EndWhen) {
        withOptionsService {
            it.endGameWhen = endWhen
        }
    }

    private fun IExecutionContext.setTimeLimit(minutes: Int?) {
        withOptionsService {
            it.setTimeLimit(minutes)
        }
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

                literal("full_card") {
                    executes {
                        setGoal(BingoGoal.Items(BingoGoal.MAX_ITEMS))
                    }
                    literal("items") {
                        executes {
                            setGoal(BingoGoal.Items(BingoGoal.MAX_ITEMS))
                        }
                    }
                    literal("lines") {
                        executes {
                            setGoal(BingoGoal.Lines(BingoGoal.MAX_LINES))
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
                                && hasLobby()
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

                literal("locator_bar") {
                    literal("all") {
                        executes { setLocatorBarMode(LocatorBarMode.ALL) }
                    }
                    literal("team_and_winner") {
                        executes { setLocatorBarMode(LocatorBarMode.TEAM_AND_WINNER) }
                    }
                    literal("team_only") {
                        executes { setLocatorBarMode(LocatorBarMode.TEAM_ONLY) }
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
                    requires { hasLobby() && hasConfigureGame() }
                    executesToggle { isPvpEnabled ->
                        withOptionsService {
                            it.togglePvp(isPvpEnabled)
                        }
                    }
                }

                literal("elytra") {
                    executesToggle { isElytra ->
                        withOptionsService {
                            it.toggleElytra(isElytra)
                        }
                    }
                }

                literal("night_vision") {
                    requires { hasLobby() && hasConfigureGame() }
                    executesToggle { isNightVision ->
                        withOptionsService {
                            it.toggleNightVision(isNightVision)
                        }
                    }
                }

                literal("preview_card") {
                    requires {
                        hasConfigureGame() && hasState(GameState.PREGAME)
                    }
                    executesToggle { isPreviewCard ->
                        withOptionsService {
                            it.togglePreviewCard(isPreviewCard)
                        }
                    }
                }

                literal("spawn_distance") {
                    requires { hasLobby() && hasConfigureGame() }
                    integer("chunks", min = 0) { chunksArg ->
                        executes {
                            val chunks = getArgument(chunksArg)
                            withOptionsService {
                                it.spawnDistance = chunks
                            }
                        }
                    }
                }

                literal("spawn_dimension") {
                    requires { hasLobby() && hasConfigureGame() }
                    string(
                        name = "dimension",
                        suggestions = {
                            scope.get<IServerWorldFactory>()
                                .listWorlds()
                                .map { it.identifier }
                                .minus(LOBBY_WORLD_ID.toString())
                        },
                        greedy = true,
                    ) { dimensionArg ->
                        executes {
                            val dimension = getArgument(dimensionArg)

                            scope.get<IServerWorldFactory>()
                                .listWorlds()
                                .find { it.identifier == dimension }
                                ?: error("Dimension '$dimension' does not exist!")

                            withOptionsService {
                                it.spawnDimension = dimension
                            }
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
        const val STALEMATE_DO_NOTHING = "/bingo options stalemate do_nothing"
    }
}