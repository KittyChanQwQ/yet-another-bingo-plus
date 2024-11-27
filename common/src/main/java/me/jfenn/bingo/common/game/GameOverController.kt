package me.jfenn.bingo.common.game

import me.jfenn.bingo.common.Permission
import me.jfenn.bingo.common.Sounds
import me.jfenn.bingo.common.config.BingoOptions
import me.jfenn.bingo.common.config.BingoWinCondition
import me.jfenn.bingo.common.config.EndWhen
import me.jfenn.bingo.common.event.ScopedEvents
import me.jfenn.bingo.common.event.packet.ServerPacketEvents
import me.jfenn.bingo.common.map.CardViewService
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.state.GameState
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.common.utils.formatString
import me.jfenn.bingo.generated.StringKey
import me.jfenn.bingo.integrations.PermissionsIntegration
import me.jfenn.bingo.platform.IPlayerManager
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
    private val textProvider: TextProvider,
    private val gameOverService: GameOverService,
    private val cardViewService: CardViewService,
    private val playerManager: IPlayerManager,
    private val permissions: PermissionsIntegration,
    private val packetManager: ServerPacketEvents,
    events: ScopedEvents,
) {

    private fun broadcastGameOver(info: GameOverService.GameOverInfo) {
        sequence {
            yield(Text.literal("┄".repeat(50)).formatted(Formatting.YELLOW))
            Text.literal("  ")
                .append(gameOverService.getMessage(info) ?: gameOverService.getTitle(info))
                .also { yield(it) }

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

            if (
                (options.endGameWhen !is EndWhen.FirstWin || options.winCondition is BingoWinCondition.Infinite)
                && info.scoreRankings.size > 1
            ) {
                yield(Text.empty())
                // List all teams that have completed the card
                for (score in info.scoreRankings) {
                    val duration = score.duration?.formatString() ?: "DNF"
                    val teamName = state.teams[score.key]?.getName(textProvider, playerName = true)
                        ?: Text.literal("[unknown]")

                    Text.empty()
                        .append(Text.literal("${score.index + 1}. ").formatted(Formatting.GRAY))
                        .append(teamName)
                        .append(" - ")
                        .append(score.score.formatText(textProvider))
                        .append(" - $duration")
                        .also { yield(it) }
                }
            }

            yield(Text.literal("┄".repeat(50)).formatted(Formatting.YELLOW))
        }.forEach {
            server.playerManager.broadcast(it, false)
        }
    }

    private fun sendGameOverPacket(
        info: GameOverService.GameOverInfo,
        isUpdate: Boolean,
    ) {
        if (!isUpdate) {
            // On the first call, ensure that all players are sent updated card data
            for (team in state.getRegisteredTeams()) {
                cardViewService.updateCard(team, forceNotFlashing = true)
            }
        }

        val winningTeam = info.winningTeamKey?.let { state.teams[it] }
        val title = gameOverService.getTitle(info)
        val message = gameOverService.getMessage(info)

        val scoreRankings = info.scoreRankings.map {
            GameOverPacket.ScoreRanking(
                index = it.index,
                key = it.key,
                name = state.teams[it.key]?.getName(textProvider, playerName = true) ?: Text.literal("[unknown]"),
                score = it.score,
                duration = it.duration,
            )
        }

        for (player in playerManager.getPlayers()) {
            val playerInfo = info.playerInfo[player.uuid]
            val isWinner = winningTeam?.includesPlayer(player.player) ?: false

            val packet = GameOverPacket(
                title = title,
                subtitle = message ?: Text.empty(),
                winner = info.winningTeamKey,
                duration = state.ingameDuration() ?: Duration.ZERO,
                canSendRestart = server.isDedicated && state.isLobbyMode && permissions.hasPermission(
                    player,
                    Permission.COMMAND_RESTART
                ),
                isWinner = isWinner,
                isUpdate = isUpdate,

                winStreak = playerInfo?.winStreak,
                bestWinStreak = playerInfo?.bestWinStreak,
                isBestWinStreak = playerInfo?.isBestWinStreak == true,

                capturedItems = playerInfo?.capturedItems,
                bestCapturedItems = playerInfo?.bestCapturedItems,
                isBestCapturedItems = playerInfo?.isBestCapturedItems == true,

                endedAt = info.endedAt,

                isBestTime = info.isBestTime,
                prevBestTime = info.prevBestTime,

                seed = server.overworld.seed,
                scores = scoreRankings,
            )

            when {
                packetManager.gameOverV6.send(player.player, packet) -> {}
                packetManager.gameOverV5.send(player.player, packet) -> {}
                packetManager.gameOverV4.send(player.player, packet) -> {}
                packetManager.gameOverV3.send(player.player, packet) -> {}
                isUpdate -> {
                    // if this is an update packet, this should only be sent to v3 clients
                    // (for v2/below, this would re-open the end screen on each update)
                }
                packetManager.gameOverV2.send(player.player, packet) -> {}
                packetManager.gameOverV1.send(player.player, packet) -> {
                    Sounds.playGameOver(player)
                }
                else -> {
                    // if the player does not have the mod, send the vanilla title message
                    player.player.networkHandler.sendPacket(TitleS2CPacket(title))
                    player.player.networkHandler.sendPacket(SubtitleS2CPacket(message))
                    Sounds.playGameOver(player)
                }
            }
        }
    }

    init {
        events.onEnter(GameState.POSTGAME) {
            val info = state.gameOverInfo ?: return@onEnter
            broadcastGameOver(info)
            sendGameOverPacket(info, isUpdate = false)
        }

        events.onUpdateTick {
            // If the game is currently over, keep sending the result to players
            // (this prevents state loss if relogging / switching dimensions)
            state.gameOverInfo?.let {
                sendGameOverPacket(it, isUpdate = true)
            }
        }
    }
}