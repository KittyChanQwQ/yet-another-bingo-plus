package me.jfenn.bingo.common.game

import kotlinx.datetime.Clock
import me.jfenn.bingo.common.config.BingoOptions
import me.jfenn.bingo.common.config.ConfigService
import me.jfenn.bingo.common.event.ScopedEvents
import me.jfenn.bingo.common.spawn.ChestService
import me.jfenn.bingo.common.spawn.SpawnService
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.state.GameState
import me.jfenn.bingo.common.team.BingoTeam
import me.jfenn.bingo.common.team.TeamService
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.common.utils.joinText
import me.jfenn.bingo.generated.StringKey
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket
import net.minecraft.network.packet.s2c.play.TitleS2CPacket
import net.minecraft.server.MinecraftServer
import net.minecraft.text.Text
import net.minecraft.util.Formatting

class GameService(
    private val state: BingoState,
    private val options: BingoOptions,
    private val server: MinecraftServer,
    private val events: ScopedEvents,
    private val spawnService: SpawnService,
    private val chestService: ChestService,
    private val teamService: TeamService,
    private val configService: ConfigService,
    private val textProvider: TextProvider,
) {

    fun start(
        sendFeedback: (StringKey) -> Unit = {},
        allowSpectators: Boolean = false,
    ): Boolean {
        val playersWithoutATeam = server.playerManager.playerList
            .filter { teamService.getPlayerTeam(it) == null }

        if (playersWithoutATeam.isNotEmpty() && !allowSpectators) {
            // If there's more than one player, teams need to be set up manually!
            if (server.playerManager.playerList.size > 1) {
                sendFeedback(StringKey.CommandStartMissingTeams)
                return false
            }

            // Otherwise, we can just auto-assign a team
            for (player in playersWithoutATeam) {
                teamService.joinTeam(player, BingoTeam.TEAM_BLOCKS.values.random())
            }
        }

        // if the game has already been started, ignore
        if (state.state != GameState.PREGAME) {
            sendFeedback(StringKey.CommandStartAlreadyStarted)
            return false
        }

        if (!options.isValid()) {
            sendFeedback(StringKey.CommandStartInvalidDistribution)
            return false
        }

        // save the current config, if overwrite option is set
        configService.writeOptions(options)

        for (player in server.playerManager.playerList) {
            val team = teamService.getPlayerTeam(player)
            player.networkHandler.sendPacket(
                TitleS2CPacket(
                    textProvider.string(StringKey.GameAnnounceBingo).formatted(team?.formatting ?: Formatting.WHITE)
                )
            )
            player.networkHandler.sendPacket(SubtitleS2CPacket(textProvider.string(StringKey.GameAnnounceStarting)))
        }

        if (state.isLobbyMode) {
            // spread player teams to spawnpoint
            spawnService.createSpawnpoints()
            chestService.createChestSpawnpoints()
        }

        // change to the loading state (until all players have loaded terrain)
        state.changeState(events, GameState.LOADING)

        broadcastStart()

        return true
    }

    fun end(reason: GameEndReason?) {
        state.endReason = reason
        state.endedAt = Clock.System.now()
        state.changeState(events, GameState.POSTGAME)

        // if lobbyMode=false, immediately change state back to PREGAME once the POSTGAME events are sent
        if (!state.isLobbyMode) {
            state.reset()
            state.changeState(events, GameState.PREGAME)
        }
    }

    private fun broadcastStart() {
        sequence {
            yield(Text.literal("┄".repeat(50)).formatted(Formatting.YELLOW))

            Text.literal("  ")
                .append(textProvider.string(StringKey.GameStart))
                .formatted(Formatting.GREEN)
                .also { yield(it) }

            Text.literal("  ")
                .append(textProvider.string(StringKey.ScoreboardMode, options.formatGameMode().map { textProvider.string(it) }.joinText()))
                .formatted(Formatting.GRAY)
                .also { yield(it) }

            Text.literal("  ")
                .append(textProvider.string(StringKey.ScoreboardFeatures, options.formatFeatures().map { textProvider.string(it) }.joinText()))
                .formatted(Formatting.GRAY)
                .also { yield(it) }

            Text.empty()
                .append("  ")
                .append(textProvider.string(StringKey.ScoreboardGoal, options.cardGoal.format(textProvider)))
                .formatted(Formatting.GRAY)
                .also { yield(it) }

            yield(Text.literal("┄".repeat(50)).formatted(Formatting.YELLOW))
        }.forEach {
            server.playerManager.broadcast(it, false)
        }
    }

}