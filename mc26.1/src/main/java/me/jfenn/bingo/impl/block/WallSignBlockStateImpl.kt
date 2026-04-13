package me.jfenn.bingo.impl.block

import me.jfenn.bingo.platform.block.IWallSignBlockState
import net.minecraft.world.level.block.WallSignBlock
import net.minecraft.world.level.block.state.BlockState
import org.joml.Vector3i

class WallSignBlockStateImpl(
    blockState: BlockState,
) : BlockStateImpl(blockState), IWallSignBlockState {
    override val facing: Vector3i
        get() = blockState.getValue(WallSignBlock.FACING)
            .unitVec3i
            .let { Vector3i(it.x, it.y, it.z) }
}