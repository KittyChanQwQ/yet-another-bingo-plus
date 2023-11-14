package me.jfenn.bingo.spawn

import net.minecraft.block.BlockState
import net.minecraft.block.FluidBlock
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

fun isEmptyBlock(world: World, blockPos: BlockPos, blockState: BlockState): Boolean {
    return blockState.isAir || blockState.getCollisionShape(world, blockPos).isEmpty
}

fun isFluidBlock(blockState: BlockState): Boolean {
    return blockState.block is FluidBlock
}
