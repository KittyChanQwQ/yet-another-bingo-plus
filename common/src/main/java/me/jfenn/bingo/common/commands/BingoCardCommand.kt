package me.jfenn.bingo.common.commands

import me.jfenn.bingo.api.commands.ICommandManager
import me.jfenn.bingo.api.commands.IExecutionContext
import me.jfenn.bingo.common.Permission
import me.jfenn.bingo.common.card.CardService
import me.jfenn.bingo.common.card.objective.BingoObjective
import me.jfenn.bingo.common.card.objective.BingoObjectiveManager
import me.jfenn.bingo.common.scope.BingoComponent
import me.jfenn.bingo.common.state.GameState
import me.jfenn.bingo.common.utils.cacheFor
import kotlin.time.Duration.Companion.seconds

class BingoCardCommand(
    commandManager: ICommandManager,
) : BingoComponent() {

    companion object {
        private val CARD_POSITIONS = buildMap {
            val text = "bingo".toCharArray()
            for (x in 0..4) for (y in 0..4) {
                this["${text[x]}${y+1}"] = Pair(x, y)
            }
        }
    }

    private fun IExecutionContext.suggestObjectives(): Iterable<String> {
        val objectiveManager = scope.get<BingoObjectiveManager>()
        return objectiveManager.list()
    }

    private val objectivesCache = cacheFor(5.seconds) { ctx: IExecutionContext ->
        ctx.suggestObjectives()
    }

    private fun IExecutionContext.setObjective(tile: String, objectiveId: String) {
        val (x, y) = CARD_POSITIONS[tile]
            ?: throw IllegalArgumentException("'$tile' is not a valid bingo tile position")

        val cardService = scope.get<CardService>()
        cardService.replaceEntry(x, y, objectiveId)
    }

    private fun IExecutionContext.setFreeSpace(tile: String) {
        val (x, y) = CARD_POSITIONS[tile]
            ?: throw IllegalArgumentException("'$tile' is not a valid bingo tile position")

        val cardService = scope.get<CardService>()
        cardService.replaceEntry(x, y, BingoObjective.FreeSpace())
    }

    init {
        commandManager.register("bingo") {
            literal("card") {
                requires {
                    hasState(GameState.PREGAME, GameState.PLAYING) && hasPermission(Permission.CONFIGURE_GAME)
                }
                string("tile", { CARD_POSITIONS.keys }) { tileArg ->
                    literal("set") {
                        string("objective", { objectivesCache.get(this) }, greedy = true) { objectiveArg ->
                            executes {
                                val tile = getArgument(tileArg).lowercase()
                                val objectiveId = getArgument(objectiveArg)
                                setObjective(tile, objectiveId)
                            }
                        }
                        literal("free_space") {
                            executes {
                                setFreeSpace(getArgument(tileArg).lowercase())
                            }
                        }
                    }
                }
            }
        }
    }

}