package me.jfenn.bingo.impl.block

import me.jfenn.bingo.platform.block.ILodestoneBlockState
import net.minecraft.world.level.block.state.BlockState

class LodestoneBlockStateImpl(
    blockState: BlockState,
) : BlockStateImpl(blockState), ILodestoneBlockState