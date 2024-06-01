package me.jfenn.bingo.common.stats

import me.jfenn.bingo.common.event.ScopedEvents
import me.jfenn.bingo.common.event.packet.ServerPacketEvents
import me.jfenn.bingo.common.scope.BingoComponent
import me.jfenn.bingo.common.stats.data.PlayerGameSummary
import me.jfenn.bingo.common.stats.packets.StatsGamePacket
import me.jfenn.bingo.common.stats.packets.StatsIndexPacket
import net.minecraft.server.network.ServerPlayerEntity
import org.slf4j.Logger
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.random.Random

class StatsSyncController(
    private val log: Logger,
    private val packets: ServerPacketEvents,
    private val stats: StatsService,
    events: ScopedEvents,
) : BingoComponent() {

    private val executor = Executors.newSingleThreadExecutor()

    private fun broadcastKnownIds(player: ServerPlayerEntity) = executor.submit {
        for (ids in stats.fetchGamesByPlayer(player.uuid)) {
            val packet = StatsIndexPacket(
                action = StatsIndexPacket.Action.BROADCAST,
                games = ids.toSet(),
            )

            log.info("[StatsSyncController] Broadcasting {} known gameIds to the client", ids.size)
            packets.statsIndexV1S2C.send(player, packet)

            // sleep to avoid overloading the connection
            try {
                Thread.sleep(Random.nextLong(20L, 100L))
            } catch (e: InterruptedException) {
                log.warn("[StatsSyncController] broadcastKnownIds interrupted!")
                break
            }
        }
    }

    private fun checkForMissingIds(player: ServerPlayerEntity, gameIds: Set<UUID>) = executor.submit {
        val missingIds = stats.checkIfIdsExist(player.uuid, gameIds)

        // if there are ids missing, send the client a packet to request them
        if (missingIds.isNotEmpty()) {
            log.info("[StatsSyncController] Requesting missing gameIds from the client")
            packets.statsIndexV1S2C.send(player, StatsIndexPacket(StatsIndexPacket.Action.REQUEST, missingIds))
        }
    }

    private fun sendRequestedIds(player: ServerPlayerEntity, gameIds: Set<UUID>) = executor.submit {
        for (gameId in gameIds) {
            val gameSummary = stats.findById(gameId = gameId, playerId = player.uuid)
            if (gameSummary == null) {
                log.error("[StatsSyncController] Requested gameId={}, playerId={} does not exist in the database", gameId, player.uuid)
                continue
            }

            log.info("[StatsSyncController] Sending requested gameId={}", gameId)
            val packet = StatsGamePacket(gameSummary)
            packets.statsGameV1S2C.send(player, packet)

            // sleep to avoid overloading the connection
            try {
                Thread.sleep(Random.nextLong(20L, 100L))
            } catch (e: InterruptedException) {
                log.warn("[StatsSyncController] sendRequestedIds interrupted!")
                break
            }
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
        events.onPlayerChannelRegister { player ->
            if (!packets.statsIndexV1S2C.isSupported(player)) return@onPlayerChannelRegister
            broadcastKnownIds(player)
        }

        events.onPacket(packets.statsIndexV1C2S) {
            when (it.packet.action) {
                StatsIndexPacket.Action.BROADCAST -> checkForMissingIds(it.player, it.packet.games)
                StatsIndexPacket.Action.REQUEST -> sendRequestedIds(it.player, it.packet.games)
            }
        }

        events.onPacket(packets.statsGameV1C2S) {
            insertReceivedGame(it.packet.game)
        }

        events.onClose {
            log.info("[StatsSyncController] Closing executor thread")
            executor.shutdown()
            executor.awaitTermination(5, TimeUnit.SECONDS)
        }
    }
}