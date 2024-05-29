package me.jfenn.bingo.common.commands

import me.jfenn.bingo.api.IPlayerHandle
import me.jfenn.bingo.api.commands.ICommandManager
import me.jfenn.bingo.api.commands.IExecutionContext
import me.jfenn.bingo.common.stats.StatsService
import me.jfenn.bingo.common.utils.formatString
import net.minecraft.text.Text

class BingoStatsCommand(
    commandManager: ICommandManager,
) {

    private fun IExecutionContext.getStatsSummary(
        player: IPlayerHandle
    ) {
        val statsService = scope.get<StatsService>()
        val summary = statsService.getPlayerSummary(player.uuid)
        sendMessage(Text.literal("Bingo stats for ${player.name}:"))
        sendMessage(Text.literal("  This month:"))
        sendMessage(Text.literal("  ${summary.monthlyWins}/${summary.monthlyLosses} win/loss - ${summary.monthlyGames} games - ${summary.monthlyItems} items - ${summary.monthlyPlaytime.formatString()} playtime"))
        sendMessage(Text.literal("  All time:"))
        sendMessage(Text.literal("  ${summary.totalWins}/${summary.totalLosses} win/loss - ${summary.totalGames} games - ${summary.totalItems} items - ${summary.totalPlaytime.formatString()} playtime"))
    }

    init {
        commandManager.register("bingo") {
            literal("stats") {
                player("player") { playerArg ->
                    executes {
                        val player = getArgument(playerArg)
                        getStatsSummary(player)
                    }
                }

                executes {
                    val player = player ?: throw IllegalArgumentException("Must be called on player")
                    getStatsSummary(player)
                }
            }
        }
    }

}