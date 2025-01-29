package me.jfenn.bingo.common.game

import me.jfenn.bingo.common.Permission
import me.jfenn.bingo.common.Sounds
import me.jfenn.bingo.common.event.ScopedEvents
import me.jfenn.bingo.common.event.packet.ServerPacketEvents
import me.jfenn.bingo.common.map.CardViewService
import me.jfenn.bingo.common.options.BingoOptions
import me.jfenn.bingo.common.options.BingoWinCondition
import me.jfenn.bingo.common.options.EndWhen
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.state.GameState
import me.jfenn.bingo.common.team.TeamService
import me.jfenn.bingo.common.text.MessageService
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.common.utils.formatString
import me.jfenn.bingo.generated.StringKey
import me.jfenn.bingo.integrations.permissions.IPermissionsApi
import me.jfenn.bingo.platform.IPlayerManager
import net.minecraft.server.MinecraftServer
import net.minecraft.text.Text
import net.minecraft.text.Texts
import net.minecraft.util.Formatting
import org.slf4j.Logger
import kotlin.time.Duration

class GameOverController(
    private val server: MinecraftServer,
    private val state: BingoState,
    private val options: BingoOptions,
    private val textProvider: TextProvider,
    private val gameOverService: GameOverService,
    private val cardViewService: CardViewService,
    private val playerManager: IPlayerManager,
    private val permissions: IPermissionsApi,
    private val packetManager: ServerPacketEvents,
    private val messageService: MessageService,
    private val teamService: TeamService,
    private val log: Logger,
    events: ScopedEvents,
) {

    private fun broadcastGameOver(info: GameOverService.GameOverInfo) {
        val scores = buildList {
            if (
                (options.endGameWhen !is EndWhen.FirstWin || options.winCondition is BingoWinCondition.Infinite)
                && info.scoreRankings.size > 1
            ) {
                add(Text.empty())
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
                        .also { add(it) }
                }
            }
        }

        val placeholders = mapOf(
            "%game_end_reason%" to listOf(
                (gameOverService.getMessage(info) ?: gameOverService.getTitle(info))
                    .let { Text.literal("  ").append(it) }
            ),
            "%time%" to textProvider.string(
                StringKey.GameEndDuration, Text.literal(
                    (state.ingameDuration() ?: Duration.ZERO).formatString()
                ).formatted(Formatting.GREEN))
                .let { listOf(Text.literal("  ").append(it)) },
            "%seed%" to textProvider.string(
                StringKey.GameEndSeed,
                Texts.bracketedCopyable(server.overworld.seed.toString())
            ).let { listOf(Text.literal("  ").append(it)) },
            "%scores%" to scores,
        )

        val players = playerManager.getPlayers()
            .filter { state.isLobbyMode || teamService.isPlaying(it) }

        messageService.getLines(MessageService.MessageType.GAME_END, placeholders)
            .forEach { text ->
                log.info(text.string)
                players.forEach { it.sendMessage(text) }
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

        val defaultTab = when {
            // If there are multiple teams & either more than one team completed the game, or the game was a draw
            scoreRankings.size > 1 && scoreRankings.count { it.duration != null } != 1 -> GameOverPacket.EndScreenTab.SCORES
            options.winCondition == BingoWinCondition.ReplaceGoals -> GameOverPacket.EndScreenTab.SCORES
            else -> GameOverPacket.EndScreenTab.CARDS
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
                    Permission.COMMAND_RESET
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
                defaultTab = defaultTab,
            )

            when {
                packetManager.gameOverV7.send(player.player, packet) -> {}
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
                    player.sendTitle(title, message)
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

        events.onPlayerChannelRegister {
            state.gameOverInfo?.let {
                sendGameOverPacket(it, isUpdate = true)
            }
        }
    }
}