package me.jfenn.bingo.impl.block

import me.jfenn.bingo.platform.block.IChestBlockState
import me.jfenn.bingo.platform.block.ILodestoneBlockState
import net.minecraft.world.level.block.state.BlockState

class ChestBlockStateImpl(
    blockState: BlockState,
) : BlockStateImpl(blockState), IChestBlockState