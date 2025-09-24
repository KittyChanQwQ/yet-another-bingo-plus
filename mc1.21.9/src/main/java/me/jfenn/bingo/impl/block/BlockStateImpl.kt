package me.jfenn.bingo.impl.block

import me.jfenn.bingo.platform.block.IBlockState
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.block.WallSignBlock
import net.minecraft.registry.Registries

open class BlockStateImpl(
    private val blockState: BlockState,
) : IBlockState {
    override val block: Block get() = blockState.block
    override val identifier: String get() = Registries.BLOCK.getId(block).toString()

    companion object {
        fun fromBlockState(blockState: BlockState): IBlockState {
            return when (blockState.block) {
                is WallSignBlock -> WallSignBlockStateImpl(blockState)
                else -> BlockStateImpl(blockState)
            }
        }
    }
}
