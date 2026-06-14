package me.jfenn.bingo.impl.block

import me.jfenn.bingo.platform.block.IPlayerHeadBlockState
import net.minecraft.world.level.block.PlayerHeadBlock
import net.minecraft.world.level.block.state.BlockState

class PlayerHeadBlockStateImpl(
    blockState: BlockState,
) : BlockStateImpl(blockState), IPlayerHeadBlockState {
    override val rotation: Int
        get() = blockState.getValue(PlayerHeadBlock.ROTATION)
}