package me.jfenn.bingo.common.stats

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import me.jfenn.bingo.platform.*
import me.jfenn.bingo.platform.item.IItemStackFactory
import me.jfenn.bingo.common.event.ScopedEvents
import me.jfenn.bingo.common.lobbyWorld
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.state.GameState
import me.jfenn.bingo.common.stats.data.PlayerStatsSummary
import me.jfenn.bingo.common.team.BingoTeam
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.common.utils.formatString
import me.jfenn.bingo.common.utils.plus
import me.jfenn.bingo.generated.StringKey
import net.minecraft.block.Blocks
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import org.joml.Matrix4f
import org.joml.Vector3d
import org.slf4j.Logger
import java.util.*
import java.util.concurrent.CompletableFuture
import kotlin.time.Duration.Companion.seconds

class StatsLobbyController(
    private val log: Logger,
    private val server: MinecraftServer,
    private val state: BingoState,
    private val stats: StatsService,
    private val textProvider: TextProvider,
    private val playerManager: IPlayerManager,
    private val itemStackFactory: IItemStackFactory,
    private val entityManager: IEntityManager,
    events: ScopedEvents
) {
    companion object {
        private val DURATION_DELAY = 5.seconds
        private val DURATION_RESET = 10.seconds
    }

    private var showingPlayer: UUID? = null
    private var showingStartedAt: Instant = Instant.DISTANT_PAST

    private var pendingStats: CompletableFuture<PlayerStatsSummary>? = null

    private var statsPos: Vector3d? = null

    private var nameEntityUuid: UUID? = null
    private val nameEntity: ITextDisplayEntity? get() {
        val lobbyWorld = server.lobbyWorld ?: return null

        nameEntityUuid
            ?.let { entityManager.getEntity(lobbyWorld, it) as? ITextDisplayEntity }
            ?.let { return it }

        val pos = statsPos ?: return null

        val entity = entityManager.createEntity(EntityType.TEXT_DISPLAY, lobbyWorld)
            .apply {
                this.pos = pos + Vector3d(0.0, 2.2, 0.0)
                value = Text.empty()
                billboard = ITextDisplayEntity.Billboard.FIXED
                alignment = ITextDisplayEntity.TextAlignment.RIGHT
                background = 0x80_000000.toInt()
            }
            .also {
                entityManager.spawnEntity(lobbyWorld, it)
            }

        nameEntityUuid = entity.uuid
        return entity
    }

    private var leftEntityUuid: UUID? = null
    private val leftEntity: ITextDisplayEntity? get() {
        val lobbyWorld = server.lobbyWorld ?: return null

        leftEntityUuid
            ?.let { entityManager.getEntity(lobbyWorld, it) as? ITextDisplayEntity }
            ?.let { return it }

        val pos = statsPos ?: return null

        val entity = entityManager.createEntity(EntityType.TEXT_DISPLAY, lobbyWorld)
            .apply {
                this.pos = pos + Vector3d(-1.8, 0.5, 0.0)
                value = Text.empty()
                billboard = ITextDisplayEntity.Billboard.FIXED
                alignment = ITextDisplayEntity.TextAlignment.RIGHT
                background = 0xb0_000000.toInt()
                transformation = Matrix4f().scale(.5f)
            }
            .also {
                entityManager.spawnEntity(lobbyWorld, it)
            }

        leftEntityUuid = entity.uuid
        return entity
    }

    private var rightEntityUuid: UUID? = null
    private val rightEntity: ITextDisplayEntity? get() {
        val lobbyWorld = server.lobbyWorld ?: return null

        rightEntityUuid
            ?.let { entityManager.getEntity(lobbyWorld, it) as? ITextDisplayEntity }
            ?.let { return it }

        val pos = statsPos ?: return null

        val entity = entityManager.createEntity(EntityType.TEXT_DISPLAY, lobbyWorld)
            .apply {
                this.pos = pos + Vector3d(1.8, 0.5, 0.0)
                value = Text.empty()
                billboard = ITextDisplayEntity.Billboard.FIXED
                alignment = ITextDisplayEntity.TextAlignment.LEFT
                background = 0xb0_000000.toInt()
                transformation = Matrix4f().scale(.5f)
            }
            .also {
                entityManager.spawnEntity(lobbyWorld, it)
            }

        rightEntityUuid = entity.uuid
        return entity
    }

    private fun getArmorStand() = server.lobbyWorld
        ?.let { entityManager.iterateEntities(it) }
        ?.mapNotNull { it as? IArmorStandEntity }
        ?.firstOrNull()

    private fun spawnSummary(
        player: ServerPlayerEntity,
        summary: PlayerStatsSummary,
    ) {
        val armorStand = getArmorStand()
        if (armorStand == null) {
            log.warn("[StatsLobbyController] Could not display player stats; no armor stand found")
            return
        }

        statsPos = armorStand.pos.let { Vector3d(it.x, it.y, it.z) }

        val playerHead = itemStackFactory.createPlayerHead()
        playerHead.setSkullOwner(player)
        armorStand.equipStack(IArmorStandEntity.EquipmentSlot.HEAD, playerHead)

        nameEntity?.value = Text.literal(playerManager.forPlayer(player).playerName)

        val favoriteTeam = BingoTeam.TEAM_BLOCKS.values
            .find { it.key == summary.favoriteTeam }
            ?.let { team ->
                Text.empty()
                    .append(
                        team.getName(
                            textProvider = textProvider,
                            symbol = true,
                            bracketed = false,
                            teamNameKey = null,
                        )
                    )
                    .append(" (${(summary.favoriteTeamPercentage * 100f).toInt()}%)")
            }

        val divider = Text.literal("\uD83E\uDF2D".repeat(28))
        val allTimeStats = buildList<Text> {
            add(textProvider.string(StringKey.StatsAllTime).append(" ★").formatted(Formatting.BOLD, Formatting.YELLOW))
            add(divider)
            add(Text.empty())

            Text.empty()
                .append(textProvider.string(StringKey.StatsGames).formatted(Formatting.BOLD, Formatting.GRAY))
                .append(": ${summary.totalGames.formatLargeNumber()}")
                .also { add(it) }

            Text.empty()
                .append(textProvider.string(StringKey.StatsWinLoss).formatted(Formatting.BOLD, Formatting.GRAY))
                .append(": ${summary.totalWins.formatLargeNumber()}/${summary.totalLosses.formatLargeNumber()}")
                .also { add(it) }

            Text.empty()
                .append(textProvider.string(StringKey.StatsItems).formatted(Formatting.BOLD, Formatting.GRAY))
                .append(": ${summary.totalItems.formatLargeNumber()}")
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
                .append(": ${summary.monthlyGames.formatLargeNumber()}")
                .also { add(it) }

            Text.empty()
                .append(textProvider.string(StringKey.StatsWinLoss).formatted(Formatting.BOLD, Formatting.GRAY))
                .append(": ${summary.monthlyWins.formatLargeNumber()}/${summary.monthlyLosses.formatLargeNumber()}")
                .also { add(it) }

            Text.empty()
                .append(textProvider.string(StringKey.StatsItems).formatted(Formatting.BOLD, Formatting.GRAY))
                .append(": ${summary.monthlyItems.formatLargeNumber()}")
                .also { add(it) }

            Text.empty()
                .append(textProvider.string(StringKey.StatsPlaytime).formatted(Formatting.BOLD, Formatting.GRAY))
                .append(": ${summary.monthlyPlaytime.formatString()}")
                .also { add(it) }

            add(Text.empty())

            Text.empty()
                .append(textProvider.string(StringKey.StatsWinStreak).formatted(Formatting.BOLD, Formatting.GRAY))
                .append(": ")
                .append(
                    when {
                        summary.winStreakBest != null -> {
                            textProvider.string(StringKey.StatsWinStreakValue, summary.winStreak, summary.winStreakBest)
                        }
                        else -> Text.literal(summary.winStreak.toString())
                    }
                )
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
            .let { leftEntity?.value = it }

        Text.literal("\n")
            .also { text ->
                for (t in monthlyStats)
                    text.append(margin).append(t).append("$margin\n")
            }
            .let { rightEntity?.value = it }
    }

    private fun resetSummary() {
        leftEntity?.value = Text.empty()
        rightEntity?.value = Text.empty()
        nameEntity?.value = Text.empty()
        getArmorStand()?.equipStack(IArmorStandEntity.EquipmentSlot.HEAD, itemStackFactory.emptyStack)
    }

    private fun tickPlayer(player: ServerPlayerEntity) {
        val now = Clock.System.now()
        // if the stats are already being shown for this player, return
        if (showingPlayer == player.uuid) {
            showingStartedAt = now
            return
        }

        // if the last change was less than DURATION_DELAY ago... then don't let another player change it yet
        if (now - showingStartedAt < DURATION_DELAY) {
            return
        }

        log.info("[StatsLobbyController] Fetching summary for player ${player.uuid}...")
        showingPlayer = player.uuid
        showingStartedAt = now
        pendingStats = stats.getPlayerSummaryAsync(player.uuid)
    }

    private fun tickPlayers() {
        val lobbyWorld = server.lobbyWorld ?: return

        val player = server.playerManager.playerList
            .filter { it.world == lobbyWorld }
            .find { player ->
                lobbyWorld.getBlockState(player.blockPos).block == Blocks.HEAVY_WEIGHTED_PRESSURE_PLATE
            }

        if (player != null) {
            tickPlayer(player)
            return
        }

        // if no player is activating the stats and DURATION_RESET has passed...
        if (Clock.System.now() - showingStartedAt > DURATION_RESET && showingPlayer != null) {
            showingPlayer = null
            resetSummary()
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