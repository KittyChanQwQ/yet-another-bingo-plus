package me.jfenn.bingo.common.stats

import me.jfenn.bingo.api.IPlayerManager
import me.jfenn.bingo.api.item.IItemStackFactory
import me.jfenn.bingo.common.event.ScopedEvents
import me.jfenn.bingo.common.lobbyWorld
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.state.GameState
import me.jfenn.bingo.common.stats.data.PlayerStatsSummary
import me.jfenn.bingo.common.team.BingoTeam
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.common.utils.formatString
import me.jfenn.bingo.common.utils.toNbt
import me.jfenn.bingo.generated.StringKey
import net.minecraft.block.Blocks
import net.minecraft.entity.EntityType
import net.minecraft.entity.EquipmentSlot
import net.minecraft.entity.decoration.ArmorStandEntity
import net.minecraft.entity.decoration.DisplayEntity
import net.minecraft.nbt.NbtCompound
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.util.math.Vec3d
import org.slf4j.Logger
import java.util.*
import java.util.concurrent.CompletableFuture

class StatsLobbyController(
    private val log: Logger,
    private val server: MinecraftServer,
    private val state: BingoState,
    private val stats: StatsService,
    private val textProvider: TextProvider,
    private val playerManager: IPlayerManager,
    private val itemStackFactory: IItemStackFactory,
    events: ScopedEvents
) {
    private var lobbyWorld = server.lobbyWorld!!
    private var showingPlayer: UUID? = null

    private var pendingStats: CompletableFuture<PlayerStatsSummary>? = null

    private var statsPos: Vec3d? = null

    private val emptyText = textProvider.serialized(Text.empty())

    private var nameEntityUuid: UUID? = null
    private val nameEntity: DisplayEntity.TextDisplayEntity? get() {
        if (nameEntityUuid != null)
            return lobbyWorld.getEntity(nameEntityUuid) as? DisplayEntity.TextDisplayEntity

        val pos = statsPos ?: return null

        val entity = EntityType.TEXT_DISPLAY.create(lobbyWorld)!!
        entity.readNbt(NbtCompound().apply {
            put("Pos", pos.add(0.0, 2.2, 0.0).toNbt())
            putString("text", emptyText)
            putString("billboard", "fixed")
            putString("alignment", "right")
            putInt("background", 0x80_000000.toInt())
        })
        lobbyWorld.spawnEntity(entity)
        nameEntityUuid = entity.uuid
        return entity
    }

    private var leftEntityUuid: UUID? = null
    private val leftEntity: DisplayEntity.TextDisplayEntity? get() {
        if (leftEntityUuid != null)
            return lobbyWorld.getEntity(leftEntityUuid) as? DisplayEntity.TextDisplayEntity

        val pos = statsPos ?: return null

        val entity = EntityType.TEXT_DISPLAY.create(lobbyWorld)!!
        entity.readNbt(NbtCompound().apply {
            put("Pos", pos.add(-1.8, 0.5, 0.0).toNbt())
            put("transformation", listOf(
                .5f, 0f, 0f, 0f,
                0f, .5f, 0f, 0f,
                0f, 0f, .5f, 0f,
                0f, 0f, 0f, 1f,
            ).toNbt())
            putString("text", emptyText)
            putString("billboard", "fixed")
            putString("alignment", "right")
            putInt("background", 0xb0_000000.toInt())
        })
        lobbyWorld.spawnEntity(entity)
        leftEntityUuid = entity.uuid
        return entity
    }

    private var rightEntityUuid: UUID? = null
    private val rightEntity: DisplayEntity.TextDisplayEntity? get() {
        if (rightEntityUuid != null)
            return lobbyWorld.getEntity(rightEntityUuid) as? DisplayEntity.TextDisplayEntity

        val pos = statsPos ?: return null

        val entity = EntityType.TEXT_DISPLAY.create(lobbyWorld)!!
        entity.readNbt(NbtCompound().apply {
            put("Pos", pos.add(1.8, 0.5, 0.0).toNbt())
            put("transformation", listOf(
                .5f, 0f, 0f, 0f,
                0f, .5f, 0f, 0f,
                0f, 0f, .5f, 0f,
                0f, 0f, 0f, 1f,
            ).toNbt())
            putString("text", emptyText)
            putString("billboard", "fixed")
            putString("alignment", "left")
            putInt("background", 0xb0_000000.toInt())
        })
        lobbyWorld.spawnEntity(entity)
        rightEntityUuid = entity.uuid
        return entity
    }

    private fun DisplayEntity.TextDisplayEntity.updateText(text: Text) {
        readNbt(writeNbt(NbtCompound()).apply {
            putString("text", textProvider.serialized(text))
        })
    }

    private fun spawnSummary(
        player: ServerPlayerEntity,
        summary: PlayerStatsSummary,
    ) {
        val armorStand = lobbyWorld.iterateEntities()
            ?.filterIsInstance<ArmorStandEntity>()
            ?.minByOrNull { it.distanceTo(player) }

        if (armorStand == null) {
            log.warn("[StatsLobbyController] Could not display player stats; no armor stand found")
            return
        }

        statsPos = armorStand.pos

        val playerHead = itemStackFactory.createPlayerHead()
        playerHead.setSkullOwner(player)
        armorStand.equipStack(EquipmentSlot.HEAD, playerHead.stack)

        nameEntity?.updateText(Text.literal(playerManager.forPlayer(player).name))

        val favoriteTeam = BingoTeam.TEAM_BLOCKS.values
            .find { it.key == summary.favoriteTeam }
            ?.let { team ->
                Text.empty()
                    .append(
                        Text.literal(team.symbol + " ")
                            .append(textProvider.string(team.name))
                            .formatted(team.formatting)
                    )
                    .append(" (${(summary.favoriteTeamPercentage * 100f).toInt()}%)")
            }

        val divider = Text.literal("\uD83E\uDF2D".repeat(26))
        val allTimeStats = buildList<Text> {
            add(textProvider.string(StringKey.StatsAllTime).append(" ★").formatted(Formatting.BOLD, Formatting.YELLOW))
            add(divider)
            add(Text.empty())

            Text.empty()
                .append(textProvider.string(StringKey.StatsGames).formatted(Formatting.BOLD, Formatting.GRAY))
                .append(": ${summary.totalGames}")
                .also { add(it) }

            Text.empty()
                .append(textProvider.string(StringKey.StatsWinLoss).formatted(Formatting.BOLD, Formatting.GRAY))
                .append(": ${summary.totalWins}/${summary.totalLosses}")
                .also { add(it) }

            Text.empty()
                .append(textProvider.string(StringKey.StatsItems).formatted(Formatting.BOLD, Formatting.GRAY))
                .append(": ${summary.totalItems}")
                .also { add(it) }

            Text.empty()
                .append(textProvider.string(StringKey.StatsPlaytime).formatted(Formatting.BOLD, Formatting.GRAY))
                .append(": ${summary.totalPlaytime.formatString()}")
                .also { add(it) }

            add(Text.empty())

            Text.empty()
                .append(textProvider.string(StringKey.StatsFavoriteTeam).formatted(Formatting.BOLD, Formatting.GRAY))
                .append(":")
                .also { add(it) }

            add(favoriteTeam ?: Text.literal("-"))
        }.toMutableList()

        val monthlyStats = buildList<Text> {
            add(Text.literal("\uD83D\uDCC6 ").append(textProvider.string(StringKey.StatsMonthly)).formatted(Formatting.BOLD, Formatting.AQUA))
            add(divider)
            add(Text.empty())

            Text.empty()
                .append(textProvider.string(StringKey.StatsGames).formatted(Formatting.BOLD, Formatting.GRAY))
                .append(": ${summary.monthlyGames}")
                .also { add(it) }

            Text.empty()
                .append(textProvider.string(StringKey.StatsWinLoss).formatted(Formatting.BOLD, Formatting.GRAY))
                .append(": ${summary.monthlyWins}/${summary.monthlyLosses}")
                .also { add(it) }

            Text.empty()
                .append(textProvider.string(StringKey.StatsItems).formatted(Formatting.BOLD, Formatting.GRAY))
                .append(": ${summary.monthlyItems}")
                .also { add(it) }

            Text.empty()
                .append(textProvider.string(StringKey.StatsPlaytime).formatted(Formatting.BOLD, Formatting.GRAY))
                .append(": ${summary.monthlyPlaytime.formatString()}")
                .also { add(it) }

            add(Text.empty())

            Text.empty()
                .append(textProvider.string(StringKey.StatsWinStreak).formatted(Formatting.BOLD, Formatting.GRAY))
                .append(": ${summary.winStreak}")
                .also { add(it) }
        }.toMutableList()

        // ensure that both lists have an equal height
        while (allTimeStats.size < monthlyStats.size)
            allTimeStats.add(Text.empty())

        while (monthlyStats.size < allTimeStats.size)
            monthlyStats.add(Text.empty())

        val margin = " ".repeat(4)
        Text.literal("\n")
            .also { text ->
                for (t in allTimeStats)
                    text.append(margin).append(t).append("$margin\n")
            }
            .let { leftEntity?.updateText(it) }

        Text.literal("\n")
            .also { text ->
                for (t in monthlyStats)
                    text.append(margin).append(t).append("$margin\n")
            }
            .let { rightEntity?.updateText(it) }
    }

    private fun tickPlayer(player: ServerPlayerEntity): Boolean {
        if (player.world != lobbyWorld)
            return false

        val isOnPressurePlate = lobbyWorld.getBlockState(player.blockPos).block == Blocks.HEAVY_WEIGHTED_PRESSURE_PLATE
        if (!isOnPressurePlate)
            return false

        if (showingPlayer == player.uuid)
            return true

        log.info("[StatsLobbyController] Fetching summary for player ${player.uuid}...")
        showingPlayer = player.uuid
        pendingStats = stats.getPlayerSummaryAsync(player.uuid)
        return true
    }

    private fun tickPlayers() {
        for (player in server.playerManager.playerList) {
            if (tickPlayer(player)) return
        }
    }

    private fun tickStats() {
        val pending = pendingStats ?: return
        if (!pending.isDone) return
        pendingStats = null

        val result = pending.getNow(null) ?: return
        val player = showingPlayer
            ?.let { server.playerManager.getPlayer(it) }
            ?: return

        log.info("[StatsLobbyController] Finished fetching player summary!")
        spawnSummary(player, result)
    }

    init {
        events.onGameTick {
            if (state.state != GameState.PREGAME) return@onGameTick

            tickPlayers()
            tickStats()
        }
    }
}