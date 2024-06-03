package me.jfenn.bingo.common.stats

import me.jfenn.bingo.api.IPlayerHandle
import me.jfenn.bingo.api.IPlayerManager
import me.jfenn.bingo.api.commands.ICommandManager
import me.jfenn.bingo.api.commands.IExecutionContext
import me.jfenn.bingo.common.Permission
import me.jfenn.bingo.common.commands.hasPermission
import me.jfenn.bingo.common.team.BingoTeam
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.common.utils.formatString
import me.jfenn.bingo.generated.StringKey
import net.minecraft.text.Text
import net.minecraft.util.Formatting

class StatsCommand(
    commandManager: ICommandManager,
    private val textProvider: TextProvider,
) {

    private fun IExecutionContext.getStatsSummary(
        player: IPlayerHandle
    ) {
        val statsService = scope.get<StatsService>()
        val summary = statsService.getPlayerSummary(player.uuid)

        sendMessage(textProvider.string(StringKey.CommandStatsPlayer, player.name))

        // All Time
        sendMessage(Text.literal("★ ").append(textProvider.string(StringKey.StatsAllTime)).formatted(Formatting.BOLD, Formatting.YELLOW))
        sendMessage(
            Text.literal("  ")
                .append(textProvider.string(StringKey.StatsGames).formatted(Formatting.BOLD, Formatting.GRAY))
                .append(": ${summary.totalGames}, ")
                .append(textProvider.string(StringKey.StatsWinLoss).formatted(Formatting.BOLD, Formatting.GRAY))
                .append(": ${summary.totalWins}/${summary.totalLosses}, ")
                .append(textProvider.string(StringKey.StatsItems).formatted(Formatting.BOLD, Formatting.GRAY))
                .append(": ${summary.totalItems}")
        )
        sendMessage(
            Text.literal("  ")
                .append(textProvider.string(StringKey.StatsPlaytime).formatted(Formatting.BOLD, Formatting.GRAY))
                .append(": ${summary.totalPlaytime.formatString()}")
        )

        // Monthly
        sendMessage(Text.literal("\uD83D\uDCC6 ").append(textProvider.string(StringKey.StatsMonthly)).formatted(Formatting.BOLD, Formatting.AQUA))
        sendMessage(
            Text.literal("  ")
                .append(textProvider.string(StringKey.StatsGames).formatted(Formatting.BOLD, Formatting.GRAY))
                .append(": ${summary.monthlyGames}, ")
                .append(textProvider.string(StringKey.StatsWinLoss).formatted(Formatting.BOLD, Formatting.GRAY))
                .append(": ${summary.monthlyWins}/${summary.monthlyLosses}, ")
                .append(textProvider.string(StringKey.StatsItems).formatted(Formatting.BOLD, Formatting.GRAY))
                .append(": ${summary.monthlyItems}")
        )
        sendMessage(
            Text.literal("  ")
                .append(textProvider.string(StringKey.StatsPlaytime).formatted(Formatting.BOLD, Formatting.GRAY))
                .append(": ${summary.monthlyPlaytime.formatString()}")
        )
    }

    init {
        commandManager.register("bingo") {
            literal("stats") {
                player("player") { playerArg ->
                    requires {
                        hasPermission(Permission.STATS_VIEW_PLAYER)
                    }
                    executes {
                        val player = getArgument(playerArg)
                        scope.get<IPlayerManager>().forPlayer(player)
                            .let { getStatsSummary(it) }
                    }
                }

                requires {
                    hasPermission(Permission.STATS_VIEW_SELF)
                }
                executes {
                    val player = player ?: throw IllegalArgumentException("Must be called on player")
                    scope.get<IPlayerManager>().forPlayer(player)
                        .let { getStatsSummary(it) }
                }
            }
        }
    }

}