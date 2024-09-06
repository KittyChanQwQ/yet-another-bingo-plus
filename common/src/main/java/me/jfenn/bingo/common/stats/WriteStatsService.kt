package me.jfenn.bingo.common.stats

import kotlinx.datetime.Clock
import kotlinx.serialization.encodeToString
import me.jfenn.bingo.common.config.BingoConfig
import me.jfenn.bingo.common.event.packet.ServerPacketEvents
import me.jfenn.bingo.common.scope.BingoComponent
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.stats.data.GameInfo
import me.jfenn.bingo.common.stats.data.GamePlayerInfo
import me.jfenn.bingo.common.stats.data.GameTeamInfo
import me.jfenn.bingo.common.stats.data.PlayerGameSummary
import me.jfenn.bingo.common.stats.packets.StatsGamePacket
import me.jfenn.bingo.common.utils.json
import me.jfenn.bingo.common.utils.measureTime
import net.minecraft.server.MinecraftServer
import org.slf4j.Logger
import kotlin.time.Duration

class WriteStatsService(
    private val log: Logger,
    private val config: BingoConfig,
    private val state: BingoState,
    private val stats: StatsService,
    private val packets: ServerPacketEvents,
    private val server: MinecraftServer,
) : BingoComponent() {
    private fun broadcastResult(
        gameInfo: GameInfo,
        teamInfo: GameTeamInfo,
        playerInfo: GamePlayerInfo,
    ) {
        // TODO: if player.uuid is also the server host (in singleplayer/lan), there's no need to send this packet
        val player = server.playerManager.getPlayer(playerInfo.minecraftId) ?: return

        packets.statsGameV1S2C.send(player, StatsGamePacket(
            PlayerGameSummary(
                game = gameInfo,
                team = teamInfo,
                player = playerInfo,
            )
        ))
    }

    private fun writeGameResult(): GameInfo {
        val insertTeams = mutableListOf<GameTeamInfo>()
        val insertPlayers = mutableListOf<GamePlayerInfo>()

        val teams = state.getRegisteredTeams()

        val options = json.encodeToString(state.options)
        val startedAt = state.startedAt ?: run {
            log.error("state.startedAt is null!")
            Clock.System.now()
        }
        val endedAt = state.endedAt ?: run {
            log.error("state.endedAt is null!")
            Clock.System.now()
        }
        val insertGame = GameInfo(
            id = state.gameId,
            bingoOptions = options,
            bingoOptionsHash = state.options.getShaHash(),
            startedAt = startedAt,
            endedAt = endedAt,
            duration = state.ingameDuration() ?: Duration.ZERO,
            playerCount = teams.sumOf { it.players.size },
            // the game is a draw if nobody won and there is more than one team
            isDraw = teams.none { it.isWinner() } && teams.size > 1,
            isForfeit = state.isForfeit,
            hostId = config.statsHostId,
        )

        for (team in teams) {
            val insertTeam = GameTeamInfo(
                id = team.key,
                gameId = insertGame.id,
                name = team.name.key,
                isWinner = team.isWinner()
            ).also { insertTeams.add(it) }

            // include any completed and in-progress cards
            val teamCards = state.cards.filter { card -> team.completedCards.none { it.card.id == card.id } }
                .plus(team.completedCards.map { it.card })

            for (player in team.players) {
                val itemCount = teamCards.sumOf { card ->
                    card.countItems { it.players.containsKey(player.uuid) }
                }
                val insertPlayer = GamePlayerInfo(
                    teamId = insertTeam.id,
                    gameId = insertGame.id,
                    minecraftId = player.uuid,
                    minecraftName = player.name,
                    capturedItems = itemCount,
                )

                broadcastResult(insertGame, insertTeam, insertPlayer)
                insertPlayers.add(insertPlayer)
            }
        }

        stats.insertGame(
            game = insertGame,
            teams = insertTeams,
            players = insertPlayers,
        )
        stats.updateBestStats(
            game = insertGame,
            players = insertPlayers,
            isSingleplayer = server.isSingleplayer && state.isSingleplayer()
        )

        return insertGame
    }

    fun writeGame(): GameInfo {
        return log.measureTime("Writing game result to stats.db...") {
            writeGameResult()
        }
    }
}