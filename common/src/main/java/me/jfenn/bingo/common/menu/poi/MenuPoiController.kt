package me.jfenn.bingo.common.menu.poi

import me.jfenn.bingo.api.state.GameState
import me.jfenn.bingo.common.LOBBY_WORLD_IDENTIFIER
import me.jfenn.bingo.common.data.ScopedData
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.generated.StringKey
import me.jfenn.bingo.platform.IExecutors
import me.jfenn.bingo.platform.IPlayerHandle
import me.jfenn.bingo.platform.IServerWorld
import me.jfenn.bingo.platform.block.BlockPosition
import me.jfenn.bingo.platform.block.ILodestoneBlockState
import me.jfenn.bingo.platform.block.IPlayerHeadBlockState
import me.jfenn.bingo.platform.block.IWallSignBlockState
import me.jfenn.bingo.platform.event.IEventBus
import me.jfenn.bingo.platform.event.model.ActionResult
import me.jfenn.bingo.platform.event.model.BreakBlockEvent
import me.jfenn.bingo.platform.event.model.UseItemOnEvent
import me.jfenn.bingo.platform.text.Formatting
import org.slf4j.Logger
import kotlin.math.atan2

internal class MenuPoiController(
    eventBus: IEventBus,
    private val serverTaskExecutor: IExecutors.IServerTaskExecutor,
    private val menuPoiState: MenuPoiState,
    private val data: ScopedData,
    private val state: BingoState,
    private val logger: Logger,
    private val text: TextProvider,
) {
    private data class Context(
        val world: IServerWorld,
        val pos: BlockPosition,
        val player: IPlayerHandle,
    )

    private fun invalidateAround(ctx: Context) {
        for (x in -1..1) {
            for (y in -2..2) {
                for (z in -1..1) {
                    invalidate(ctx.copy(pos = ctx.pos.move(x, y, z)))
                }
            }
        }
    }

    private fun invalidate(ctx: Context) {
        val pos = ctx.pos
        menuPoiState.poi.removeIf { it.pos == pos }

        // Test for spawnpoint
        val blockState = ctx.world.getBlockState(pos)
        if (blockState is IPlayerHeadBlockState) {
            val yaw = (blockState.rotation - 8).toFloat() * 360f / 16f
            ctx.addPoi(MenuPoi.Spawn(pos, yaw))
        }

        // Test for menu blocks
        val blocks = listOf(
            pos.move(y = 1).toBlockPos(),
            pos.toBlockPos(),
            pos.move(y = -1).toBlockPos()
        ).associateWith {
            ctx.world.getBlockState(BlockPosition.fromBlockPos(it))
                    as? IWallSignBlockState
        }

        val isMenuPos = blocks.values.all {
            it?.identifier == "minecraft:oak_wall_sign"
        }
        val centerState = blocks.values.first()

        if (isMenuPos && centerState != null) {
            val directionVec = centerState.facing
            val yaw = atan2(directionVec.x.toDouble(), directionVec.z.toDouble())

            ctx.addPoi(MenuPoi.Menu(pos, yaw))
        }

        // Test for team preset
        val isLodestone = ctx.world.getBlockState(pos.move(0, -1, 0)) is ILodestoneBlockState
        if (isLodestone) {
            val blockState = ctx.world.getBlockState(pos)
            val teamKey = data.teamPresets.entries
                .find { it.value.blockId == blockState.identifier }
                ?.key

            if (teamKey != null) {
                ctx.addPoi(MenuPoi.Team(pos, teamKey))
            }
        }
    }

    private fun Context.addPoi(poi: MenuPoi) {
        logger.info("[MenuPoiController] Created poi: $poi")
        menuPoiState.poi.add(poi)

        when (poi) {
            is MenuPoi.Spawn -> text.string(StringKey.LobbyEditCreatedSpawn, poi.pos.toString())
            is MenuPoi.Team -> text.string(StringKey.LobbyEditCreatedTeam, poi.key.id, poi.pos.toString())
            is MenuPoi.Menu -> text.string(StringKey.LobbyEditCreatedMenu, poi.pos.toString())
        }.let { player.sendMessage(it.formatted(Formatting.GRAY)) }
    }

    init {
        eventBus.register(UseItemOnEvent) {
            if (state.state == GameState.LOBBY_EDITOR && it.world.identifier == LOBBY_WORLD_IDENTIFIER) {
                serverTaskExecutor.execute {
                    invalidateAround(Context(it.world, it.blockPos, it.player))
                }
            }
            ActionResult.Pass(Unit)
        }

        eventBus.register(BreakBlockEvent.After) {
            if (state.state == GameState.LOBBY_EDITOR && it.world.identifier == LOBBY_WORLD_IDENTIFIER) {
                invalidateAround(Context(it.world, it.blockPos, it.player))
            }
        }
    }
}