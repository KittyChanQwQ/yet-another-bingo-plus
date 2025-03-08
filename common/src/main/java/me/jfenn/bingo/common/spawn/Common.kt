package me.jfenn.bingo.common.spawn

import net.minecraft.block.BlockState
import net.minecraft.block.FluidBlock
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.math.BlockPos

fun isEmptyBlock(world: ServerWorld, blockPos: BlockPos, blockState: BlockState): Boolean {
    return blockState.isAir || blockState.getCollisionShape(world, blockPos).isEmpty
}

fun isFluidBlock(blockState: BlockState): Boolean {
    return blockState.block is FluidBlock
}
