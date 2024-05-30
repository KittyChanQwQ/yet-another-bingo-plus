package me.jfenn.bingo.common.game

import kotlinx.datetime.Clock
import me.jfenn.bingo.api.IPlayerManager
import me.jfenn.bingo.common.Permission
import me.jfenn.bingo.common.Sounds
import me.jfenn.bingo.common.card.TeamService
import me.jfenn.bingo.common.config.BingoOptions
import me.jfenn.bingo.common.event.ScopedEvents
import me.jfenn.bingo.common.event.packet.ServerPacketEvents
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.state.GameState
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

class GameOverController(
    private val server: MinecraftServer,
    private val state: BingoState,
    private val options: BingoOptions,
    private val teamService: TeamService,
    private val textProvider: TextProvider,
    private val gameOverService: GameOverService,
    private val playerManager: IPlayerManager,
    private val permissions: PermissionsIntegration,
    private val packetManager: ServerPacketEvents,
    events: ScopedEvents,
) {

    private fun broadcastGameOver() {
        val info = gameOverService.getGameInfo()

        sequence {
            yield(Text.literal("┄".repeat(50)).formatted(Formatting.YELLOW))
            yield(Text.empty().append("  ").append(info.message))

            Text.literal("  ")
                .append(
                    textProvider.string(
                        StringKey.GameEndDuration, Text.literal(
                            (state.ingameDuration() ?: Duration.ZERO).formatString()
                        ).formatted(Formatting.GREEN))
                )
                .also { yield(it) }

            Text.literal("  ")
                .append(
                    textProvider.string(
                        StringKey.GameEndSeed,
                        Texts.bracketedCopyable(server.overworld.seed.toString())
                    )
                )
                .also { yield(it) }

            if (options.isRankedMode) {
                yield(Text.empty())
                // List all teams that have completed the card
                state.getRegisteredTeams()
                    .filter { it.isWinner() }
                    .mapNotNull { team ->
                        team.winner?.let { team to it }
                    }
                    .sortedBy { (_, win) -> win.instant }
                    .forEachIndexed { index, (team, win) ->
                        val duration = win.instant - (state.startedAt ?: Clock.System.now())
                        Text.empty()
                            .append(Text.literal("${index + 1}. ").formatted(Formatting.GRAY))
                            .append(teamService.getTeamOrPlayerName(team))
                            .append(" - ${duration.formatString()}")
                            .also { yield(it) }
                    }

                // List any teams that started the card but did not finish
                state.getRegisteredTeams()
                    .filter { !it.isWinner() }
                    .forEach { team ->
                        Text.empty()
                            .append(Text.literal("DNF - ").formatted(Formatting.GRAY))
                            .append(teamService.getTeamOrPlayerName(team))
                            .also { yield(it) }
                    }
            }

            yield(Text.literal("┄".repeat(50)).formatted(Formatting.YELLOW))
        }.forEach {
            server.playerManager.broadcast(it, false)
        }
    }

    private fun sendGameOverPacket(
        isUpdate: Boolean,
    ) {
        val info = gameOverService.getGameInfo()

        for (player in playerManager.getPlayers()) {
            val packet = GameOverPacket(
                title = info.title,
                subtitle = info.message,
                winner = info.winningTeam?.key,
                duration = state.ingameDuration() ?: Duration.ZERO,
                canSendRestart = server.isDedicated && permissions.hasPermission(
                    player.player,
                    Permission.COMMAND_RESTART
                ),
                isWinner = info.winningTeam?.includesPlayer(player.player) ?: false,
                isUpdate = isUpdate,
            )

            if (isUpdate) {
                // if this is an update packet, this should only be sent to v3 clients
                // (for v2/below, this would re-open the end screen on each update)
                packetManager.gameOverV3.send(player.player, packet)
                continue
            }

            when {
                packetManager.gameOverV3.send(player.player, packet) -> {}
                packetManager.gameOverV2.send(player.player, packet) -> {}
                packetManager.gameOverV1.send(player.player, packet) -> {
                    Sounds.playGameOver(player)
                }
                else -> {
                    // send the winning team title message
                    player.player.networkHandler.sendPacket(TitleS2CPacket(info.title))
                    player.player.networkHandler.sendPacket(SubtitleS2CPacket(info.message))
                    Sounds.playGameOver(player)
                }
            }
        }
    }

    init {
        events.onEnter(GameState.POSTGAME) {
            broadcastGameOver()
            sendGameOverPacket(isUpdate = false)
        }

        events.onUpdateTick {
            if (state.state == GameState.POSTGAME) {
                sendGameOverPacket(isUpdate = true)
            }
        }
    }
}