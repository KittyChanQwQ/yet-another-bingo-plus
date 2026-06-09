package me.jfenn.bingo.impl.block

import me.jfenn.bingo.platform.block.ITorchBlockState
import net.minecraft.world.level.block.state.BlockState

class TorchBlockStateImpl(
    blockState: BlockState,
) : BlockStateImpl(blockState), ITorchBlockState
