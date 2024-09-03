package me.jfenn.bingo.common.controller

import me.jfenn.bingo.api.IPlayerHandle
import me.jfenn.bingo.api.IPlayerManager
import me.jfenn.bingo.common.MOD_ID_BINGO
import me.jfenn.bingo.common.config.BingoOptions
import me.jfenn.bingo.common.config.PlayerSettingsService
import me.jfenn.bingo.common.event.ScopedEvents
import me.jfenn.bingo.common.scope.BingoComponent
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.team.BingoTeam
import me.jfenn.bingo.common.team.BingoTeamKey
import me.jfenn.bingo.common.team.TeamService
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.generated.StringKey
import net.minecraft.entity.boss.BossBar
import net.minecraft.entity.boss.CommandBossBar
import net.minecraft.server.MinecraftServer
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.util.Identifier

/**
 * Displays bossbar score/timer information for all players.
 */
class BossBarController(
    events: ScopedEvents,
    private val server: MinecraftServer,
    private val options: BingoOptions,
    private val state: BingoState,
    private val playerManager: IPlayerManager,
    private val playerSettingsService: PlayerSettingsService,
    private val teamService: TeamService,
    private val textProvider: TextProvider,
) : BingoComponent() {

    private fun createBossbar(team: BingoTeam?): CommandBossBar {
        val bossbarId = Identifier.of(MOD_ID_BINGO, team?.id ?: "preview")

        // if the boss bar already exists, recreate it
        server.bossBarManager.get(bossbarId)
            ?.also { server.bossBarManager.remove(it) }

        return server.bossBarManager.add(bossbarId, Text.literal("Time Remaining"))
            .apply {
                color = BossBar.Color.WHITE
                style = BossBar.Style.PROGRESS
                value = 0
                maxValue = 10_000
            }
    }

    private val bossBars = mutableMapOf<BingoTeamKey?, CommandBossBar>()

    private fun getBossbar(team: BingoTeam?) = bossBars.getOrPut(team?.key) { createBossbar(team) }

    private fun updateBossBar(player: IPlayerHandle) {
        val settings = playerSettingsService.getPlayer(player)
        val team = teamService.getPlayerTeam(player)

        for ((teamKey, bossBar) in bossBars) {
            val shouldIncludePlayer = state.isLobbyMode || team != null
            if (settings.bossbar && teamKey == team?.key && shouldIncludePlayer)
                bossBar.addPlayer(player.player)
            else bossBar.removePlayer(player.player)
        }
    }

    init {
        events.onPlayerJoin { player -> updateBossBar(player) }
        events.onChangePlayerSettings { player -> updateBossBar(player) }

        events.onUpdateTick {
            for (player in playerManager.getPlayers())
                updateBossBar(player)

            for (team in state.getRegisteredTeams() + null) {
                val bossBar = getBossbar(team)

                if (options.showRemainingTime) {
                    val timeLimit = options.timeLimit
                    val duration = state.ingameDuration()
                    if (timeLimit != null && duration != null) {
                        bossBar.value = ((1f - (duration.inWholeSeconds / timeLimit.inWholeSeconds.toFloat())) * bossBar.maxValue).toInt()
                    }
                }

                val card = team?.let { state.getCard(it) } ?: state.getActiveCard()
                val goalText = state.formatCardGoals(card, textProvider).formatted(Formatting.BOLD)
                bossBar.name = when {
                    options.showRemainingTime -> {
                        textProvider.string(StringKey.BossbarTimeRemainingOrGoal, state.formatTimeRemaining(textProvider).formatted(Formatting.BOLD), goalText)
                    }
                    else -> goalText
                }
            }
        }
    }

}