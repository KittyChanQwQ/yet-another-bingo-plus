package me.jfenn.bingo.common

import me.jfenn.bingo.common.card.BingoTeam
import me.jfenn.bingo.common.card.TeamService
import me.jfenn.bingo.common.config.BingoOptions
import me.jfenn.bingo.common.config.ConfigService
import me.jfenn.bingo.common.event.ScopedEvents
import me.jfenn.bingo.common.event.packet.ServerPacketManager
import me.jfenn.bingo.common.scoring.ScoreService
import me.jfenn.bingo.common.spawn.SpawnService
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.state.GameState
import me.jfenn.bingo.common.text.GameOverPacket
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.common.utils.formatString
import me.jfenn.bingo.generated.StringKey
import me.jfenn.bingo.integrations.PermissionsIntegration
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket
import net.minecraft.network.packet.s2c.play.TitleS2CPacket
import net.minecraft.server.MinecraftServer
import net.minecraft.text.Text
import net.minecraft.text.Texts
import net.minecraft.util.Formatting
import kotlin.time.Duration

class GameService(
    private val state: BingoState,
    private val options: BingoOptions,
    private val server: MinecraftServer,
    private val events: ScopedEvents,
    private val spawnService: SpawnService,
    private val teamService: TeamService,
    private val configService: ConfigService,
    private val permissions: PermissionsIntegration,
    private val textProvider: TextProvider,
    private val packetManager: ServerPacketManager,
) {

    fun start(
        sendFeedback: (String) -> Unit = {},
        allowSpectators: Boolean = false,
    ): Boolean {
        val playersWithoutATeam = server.playerManager.playerList
            .filter { teamService.getPlayerTeam(it) == null }

        if (playersWithoutATeam.isNotEmpty() && !allowSpectators) {
            // If there's more than one player, teams need to be set up manually!
            if (server.playerManager.playerList.size > 1) {
                sendFeedback("Some players have not picked a team! Run '/bingo shuffleteams' or sneak+click to ignore.")
                return false
            }

            // Otherwise, we can just auto-assign a team
            for (player in playersWithoutATeam) {
                teamService.joinTeam(player, BingoTeam.TEAM_BLOCKS.values.random())
            }
        }

        // if the game has already been started, ignore
        if (state.startedAt != null) {
            sendFeedback("The game has already been started! (if this is a bug, try restarting the server...)")
            return false
        }

        if (!options.isValid()) {
            sendFeedback("Item distribution must add up to 25!")
            return false
        }

        // save the current config, if overwrite option is set
        configService.writeOptions(options)

        for (player in server.playerManager.playerList) {
            val team = teamService.getPlayerTeam(player)
            player.networkHandler.sendPacket(TitleS2CPacket(Text.literal("BINGO").formatted(team?.formatting ?: Formatting.WHITE)))
            player.networkHandler.sendPacket(SubtitleS2CPacket(Text.literal("Game is starting!")))
        }

        // spread player teams to spawnpoint
        spawnService.createSpawnpoints()

        broadcastStart()

        // change to the playing state
        state.changeState(events, GameState.PLAYING)

        for (player in server.playerManager.playerList) {
            val team = teamService.getPlayerTeam(player)
            player.networkHandler.sendPacket(TitleS2CPacket(Text.literal("BINGO").formatted(team?.formatting ?: Formatting.WHITE)))
            player.networkHandler.sendPacket(SubtitleS2CPacket(Text.literal("Game has started!")))
            Sounds.playGameStarted(player)
        }

        return true
    }

    fun end(reason: Text?) {
        broadcastEnd(reason)
        state.changeState(events, GameState.POSTGAME)
    }

    private fun broadcastStart() {
        sequence {
            yield(Text.literal("┄".repeat(50)).formatted(Formatting.YELLOW))

            Text.literal("  ")
                .append(textProvider.string(StringKey.GameStart))
                .formatted(Formatting.GREEN)
                .also { yield(it) }

            Text.literal("  ")
                .append(textProvider.string(StringKey.ScoreboardMode, options.formatGameMode()))
                .formatted(Formatting.GRAY)
                .also { yield(it) }

            Text.literal("  ")
                .append(textProvider.string(StringKey.ScoreboardFeatures, options.formatFeatures()))
                .formatted(Formatting.GRAY)
                .also { yield(it) }

            Text.empty()
                .append("  ")
                .append(textProvider.string(StringKey.ScoreboardGoal, textProvider.string(options.goal.string)))
                .formatted(Formatting.GRAY)
                .also { yield(it) }

            yield(Text.literal("┄".repeat(50)).formatted(Formatting.YELLOW))
        }.forEach {
            server.playerManager.broadcast(it, false)
        }
    }

    private fun broadcastEnd(reason: Text?) {
        val winningTeams = state.getRegisteredTeams()
            .filter { it.isWinner() }

        // The winning team, if there is only one winner
        val winningTeam = winningTeams
            .takeIf { it.size == 1 }
            ?.firstOrNull()

        val winTitle = reason ?: textProvider.string(StringKey.GameEndGameOver)
        val winMessage = (
            Text.empty()
                .takeIf { options.isRankedMode }
                ?: winningTeam?.let { winner ->
                    textProvider.string(StringKey.GameEndCompletedCard, winner.teamOrPlayerName())
                }
                ?: ScoreService.getLeadingByLines(state)?.let { leader ->
                    textProvider.string(StringKey.GameEndWonWithMoreLines, leader.teamOrPlayerName())
                }
                ?: ScoreService.getLeadingByItems(state)?.let { leader ->
                    textProvider.string(StringKey.GameEndWonWithMoreItems, leader.teamOrPlayerName())
                }
                ?: textProvider.string(StringKey.GameEndDraw)
        ).formatted(Formatting.YELLOW)

        sequence {
            yield(Text.literal("┄".repeat(50)).formatted(Formatting.YELLOW))
            yield(Text.empty().append("  ").append(winMessage))

            Text.literal("  ")
                .append(
                    textProvider.string(StringKey.GameEndDuration, Text.literal((state.ingameDuration() ?: Duration.ZERO).formatString()).formatted(Formatting.GREEN))
                )
                .also { yield(it) }

            Text.literal("  ")
                .append(
                    textProvider.string(StringKey.GameEndSeed, Texts.bracketedCopyable(server.overworld.seed.toString()))
                )
                .also { yield(it) }

            if (options.isRankedMode) {
                yield(Text.empty())
                for (text in state.formatWinners())
                    yield(Text.empty().append("  ").append(text))
            }

            yield(Text.literal("┄".repeat(50)).formatted(Formatting.YELLOW))
        }.forEach {
            server.playerManager.broadcast(it, false)
        }

        for (player in server.playerManager.playerList) {
            val packet = GameOverPacket(
                title = winTitle,
                subtitle = winMessage,
                winner = winningTeam?.key,
                duration = state.ingameDuration() ?: Duration.ZERO,
                canSendRestart = server.isDedicated && permissions.hasPermission(player, Permission.COMMAND_RESTART),
                isWinner = winningTeam?.includesPlayer(player) ?: false,
            )

            when {
                packetManager.sendPacketS2C(player, GameOverPacket.V2, packet) -> {}
                packetManager.sendPacketS2C(player, GameOverPacket.V1, packet) -> {
                    Sounds.playGameOver(player)
                }
                else -> {
                    // send the winning team title message
                    player.networkHandler.sendPacket(TitleS2CPacket(winTitle))
                    player.networkHandler.sendPacket(SubtitleS2CPacket(winMessage))
                    Sounds.playGameOver(player)
                }
            }
        }
    }

}