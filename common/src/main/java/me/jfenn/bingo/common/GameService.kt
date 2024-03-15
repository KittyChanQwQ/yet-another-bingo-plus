package me.jfenn.bingo.common

import me.jfenn.bingo.common.card.BingoTeam
import me.jfenn.bingo.common.card.ScoreService
import me.jfenn.bingo.common.card.TeamService
import me.jfenn.bingo.common.config.BingoOptions
import me.jfenn.bingo.common.config.ConfigService
import me.jfenn.bingo.common.event.ScopedEvents
import me.jfenn.bingo.common.map.CardViewService
import me.jfenn.bingo.common.spawn.SpawnService
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.state.GameState
import me.jfenn.bingo.common.text.GameOverPacket
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.common.utils.formatString
import me.jfenn.bingo.integrations.PermissionsIntegration
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
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
    private val cardViewService: CardViewService,
    private val configService: ConfigService,
    private val permissions: PermissionsIntegration,
    private val textProvider: TextProvider,
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
            yield(Text.literal("  BINGO Start!").formatted(Formatting.GREEN))

            Text.empty()
                .append("  Game mode: ")
                .append(options.formatGameMode())
                .formatted(Formatting.GRAY)
                .also { yield(it) }

            Text.empty()
                .append("  Features: ")
                .append(options.formatFeatures())
                .formatted(Formatting.GRAY)
                .also { yield(it) }

            Text.empty()
                .append("  Win condition: ")
                .append(options.goal.formatName())
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

        val winTitle = reason ?: textProvider.string("bingo.game.end.game_over")
        val winMessage = (
            Text.empty()
                .takeIf { options.isRankedMode }
                ?: winningTeam?.let { winner ->
                    textProvider.string("bingo.game.end.completed_card", winner.text())
                }
                ?: ScoreService.getLeadingByLines(state)?.let { leader ->
                    textProvider.string("bingo.game.end.won_with_more_lines", leader.text())
                }
                ?: ScoreService.getLeadingByItems(state)?.let { leader ->
                    textProvider.string("bingo.game.end.won_with_more_items", leader.text())
                }
                ?: textProvider.string("bingo.game.end.draw")
        ).formatted(Formatting.YELLOW)

        sequence {
            yield(Text.literal("┄".repeat(50)).formatted(Formatting.YELLOW))
            yield(Text.empty().append("  ").append(winMessage))

            Text.literal("  ")
                .append(
                    textProvider.string("bingo.game.end.duration", (state.ingameDuration() ?: Duration.ZERO).formatString())
                )
                .also { yield(it) }

            Text.literal("  ")
                .append(
                    textProvider.string("bingo.game.end.seed", Texts.bracketedCopyable(server.overworld.seed.toString()))
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
            if (cardViewService.supportsCardHud(player) && ServerPlayNetworking.canSend(player, PACKET_GAME_OVER_S2C)) {
                ServerPlayNetworking.send(player, PACKET_GAME_OVER_S2C, GameOverPacket(
                    title = winTitle,
                    subtitle = winMessage,
                    winner = winningTeam?.key,
                    duration = state.ingameDuration() ?: Duration.ZERO,
                    canSendRestart = server.isDedicated && permissions.hasPermission(player, Permission.COMMAND_RESTART),
                ).toPacketBuf())
            } else {
                // send the winning team title message
                player.networkHandler.sendPacket(TitleS2CPacket(winTitle))
                player.networkHandler.sendPacket(SubtitleS2CPacket(winMessage))

                Sounds.playGameOver(player)
            }
        }
    }

}