package me.jfenn.bingo.common.bossbar

import me.jfenn.bingo.common.options.BingoOptions
import me.jfenn.bingo.common.event.ScopedEvents
import me.jfenn.bingo.common.scope.BingoComponent
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.generated.StringKey
import me.jfenn.bingo.platform.IPlayerManager
import net.minecraft.util.Formatting

/**
 * Displays bossbar score/timer information for all players.
 */
class BossBarController(
    events: ScopedEvents,
    private val options: BingoOptions,
    private val state: BingoState,
    private val playerManager: IPlayerManager,
    private val textProvider: TextProvider,
    private val bossBarService: BossBarService,
) : BingoComponent() {

    init {
        events.onPlayerJoin { player -> bossBarService.updateBossBar(player) }
        events.onChangeTeam { player -> bossBarService.updateBossBar(player) }
        events.onChangePlayerSettings { player -> bossBarService.updateBossBar(player) }

        events.onUpdateTick {
            for (player in playerManager.getPlayers())
                bossBarService.updateBossBar(player)

            for (team in state.getRegisteredTeams() + null) {
                val bossBar = bossBarService.getBossbar(team)

                if (options.showRemainingTime) {
                    val timeLimit = options.timeLimit
                    val duration = state.ingameDuration()
                    if (timeLimit != null && duration != null) {
                        bossBar.value = ((1f - (duration.inWholeSeconds / timeLimit.inWholeSeconds.toFloat())) * bossBar.maxValue).toInt()
                    }
                }

                val goalText = state.formatCardGoals(team, textProvider).formatted(Formatting.BOLD)
                bossBar.name = when {
                    options.showRemainingTime -> {
                        textProvider.string(
                            StringKey.BossbarTimeRemainingOrGoal, state.formatTimeRemaining(textProvider).formatted(
                                Formatting.BOLD
                            ), goalText)
                    }
                    else -> goalText
                }
            }
        }
    }

}