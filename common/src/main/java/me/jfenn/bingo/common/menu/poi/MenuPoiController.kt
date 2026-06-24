package me.jfenn.bingo.common.menu.poi

import me.jfenn.bingo.common.LOBBY_WORLD_IDENTIFIER
import me.jfenn.bingo.common.data.ScopedData
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.state.GameState
import me.jfenn.bingo.platform.IExecutors
import me.jfenn.bingo.platform.IServerWorld
import me.jfenn.bingo.platform.block.BlockPosition
import me.jfenn.bingo.platform.block.ILodestoneBlockState
import me.jfenn.bingo.platform.block.IPlayerHeadBlockState
import me.jfenn.bingo.platform.block.IWallSignBlockState
import me.jfenn.bingo.platform.event.IEventBus
import me.jfenn.bingo.platform.event.model.ActionResult
import me.jfenn.bingo.platform.event.model.BreakBlockEvent
import me.jfenn.bingo.platform.event.model.UseItemOnBlockEvent
import org.slf4j.Logger
import kotlin.math.atan2

internal class MenuPoiController(
    eventBus: IEventBus,
    private val serverTaskExecutor: IExecutors.IServerTaskExecutor,
    private val menuPoiState: MenuPoiState,
    private val data: ScopedData,
    private val state: BingoState,
    private val logger: Logger,
) {
    private fun invalidateAround(world: IServerWorld, pos: BlockPosition) {
        for (x in -1..1) {
            for (y in -2..2) {
                for (z in -1..1) {
                    invalidate(world, pos.move(x, y, z))
                }
            }
        }
    }

    private fun invalidate(world: IServerWorld, pos: BlockPosition) {
        menuPoiState.poi.removeIf { it.pos == pos }

        // Test for spawnpoint
        val blockState = world.getBlockState(pos)
        if (blockState is IPlayerHeadBlockState) {
            val yaw = (blockState.rotation - 8).toFloat() * 360f / 16f
            addPoi(MenuPoi.Spawn(pos, yaw))
        }

        // Test for menu blocks
        val blocks = listOf(
            pos.move(y = 1).toBlockPos(),
            pos.toBlockPos(),
            pos.move(y = -1).toBlockPos()
        ).associateWith {
            world.getBlockState(BlockPosition.fromBlockPos(it))
                    as? IWallSignBlockState
        }

        val isMenuPos = blocks.values.all {
            it?.identifier == "minecraft:oak_wall_sign"
        }
        val centerState = blocks.values.first()

        if (isMenuPos && centerState != null) {
            val directionVec = centerState.facing
            val yaw = atan2(directionVec.x.toDouble(), directionVec.z.toDouble())

            addPoi(MenuPoi.Menu(pos, yaw))
        }

        // Test for team preset
        val isLodestone = world.getBlockState(pos.move(0, -1, 0)) is ILodestoneBlockState
        if (isLodestone) {
            val blockState = world.getBlockState(pos)
            val teamKey = data.teamPresets.entries
                .find { it.value.blockId == blockState.identifier }
                ?.key

            if (teamKey != null) {
                addPoi(MenuPoi.Team(pos, teamKey))
            }
        }
    }

    private fun addPoi(poi: MenuPoi) {
        logger.info("[MenuPoiController] Created poi: $poi")
        menuPoiState.poi.add(poi)
    }

    init {
        eventBus.register(UseItemOnBlockEvent.Companion) {
            if (state.state == GameState.LOBBY_EDITOR && it.world.identifier == LOBBY_WORLD_IDENTIFIER) {
                serverTaskExecutor.execute {
                    invalidateAround(it.world, it.blockPos)
                }
            }
            ActionResult.Pass(Unit)
        }

        eventBus.register(BreakBlockEvent.After) {
            if (state.state == GameState.LOBBY_EDITOR && it.world.identifier == LOBBY_WORLD_IDENTIFIER) {
                invalidateAround(it.world, it.blockPos)
            }
        }
    }
}