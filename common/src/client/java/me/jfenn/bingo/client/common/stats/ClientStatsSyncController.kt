package me.jfenn.bingo.client.common.stats

import me.jfenn.bingo.client.api.ISessionAccessor
import me.jfenn.bingo.client.common.packet.ClientPacketEvents
import me.jfenn.bingo.common.scope.BingoComponent
import me.jfenn.bingo.common.stats.StatsService
import me.jfenn.bingo.common.stats.data.PlayerGameSummary
import me.jfenn.bingo.common.stats.packets.StatsGamePacket
import me.jfenn.bingo.common.stats.packets.StatsIndexPacket
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents
import net.fabricmc.fabric.api.client.networking.v1.C2SPlayChannelEvents
import org.slf4j.Logger
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.random.Random

class ClientStatsSyncController(
    private val log: Logger,
    private val packets: ClientPacketEvents,
    private val stats: StatsService,
    sessionAccessor: ISessionAccessor,
) : BingoComponent() {

    private val playerUuid = sessionAccessor.getPlayerUuid()
        ?: UUID.fromString("00000000-0000-0000-0000-000000000000")

    private val executor = Executors.newSingleThreadExecutor()

    private fun broadcastKnownIds() = executor.submit {
        for (ids in stats.fetchGamesByPlayer(playerUuid)) {
            val packet = StatsIndexPacket(
                action = StatsIndexPacket.Action.BROADCAST,
                games = ids.toSet(),
            )

            log.info("[ClientStatsSyncController] Broadcasting {} known gameIds to the server", ids.size)
            packets.statsIndexV1C2S.send(packet)

            // sleep to avoid overloading the connection
            Thread.sleep(Random.nextLong(20L, 100L))
        }
    }

    private fun checkForMissingIds(gameIds: Set<UUID>) = executor.submit {
        val missingIds = stats.checkIfIdsExist(playerUuid, gameIds)

        // if there are ids missing, send the client a packet to request them
        if (missingIds.isNotEmpty()) {
            log.info("[ClientStatsSyncController] Requesting missing gameIds from the server")
            packets.statsIndexV1C2S.send(StatsIndexPacket(StatsIndexPacket.Action.REQUEST, missingIds))
        }
    }

    private fun sendRequestedIds(gameIds: Set<UUID>) = executor.submit {
        for (gameId in gameIds) {
            val gameSummary = stats.findById(gameId = gameId, playerId = playerUuid)
            if (gameSummary == null) {
                log.error("[ClientStatsSyncController] Requested gameId={}, playerId={} does not exist in the database", gameId, playerUuid)
                continue
            }

            log.info("[ClientStatsSyncController] Sending requested gameId={}", gameId)
            val packet = StatsGamePacket(gameSummary)
            packets.statsGameV1C2S.send(packet)

            // sleep to avoid overloading the connection
            Thread.sleep(Random.nextLong(20L, 100L))
        }
    }

    private fun insertReceivedGame(game: PlayerGameSummary) {
        stats.insertGame(
            game = game.game,
            teams = listOf(game.team),
            players = listOf(game.player),
        )
    }

    init {
        C2SPlayChannelEvents.REGISTER.register { _, _, _, _ ->
            if (packets.statsIndexV1C2S.isSupported()) {
                broadcastKnownIds()
            }
        }

        packets.statsIndexV1S2C.onPacket {
            when (it.packet.action) {
                StatsIndexPacket.Action.BROADCAST -> checkForMissingIds(it.packet.games)
                StatsIndexPacket.Action.REQUEST -> sendRequestedIds(it.packet.games)
            }
        }

        packets.statsGameV1S2C.onPacket {
            insertReceivedGame(it.packet.game)
        }

        ClientLifecycleEvents.CLIENT_STOPPING.register {
            executor.awaitTermination(5L, TimeUnit.SECONDS)
        }
    }
}