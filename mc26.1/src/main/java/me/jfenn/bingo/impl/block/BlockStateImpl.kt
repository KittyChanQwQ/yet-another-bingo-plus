package me.jfenn.bingo.impl.block

import me.jfenn.bingo.impl.BlockRegistryEntry
import me.jfenn.bingo.impl.inventory.InventoryImpl
import me.jfenn.bingo.platform.IRegistryEntry
import me.jfenn.bingo.platform.IServerWorld
import me.jfenn.bingo.platform.block.BlockPosition
import me.jfenn.bingo.platform.block.IBlockState
import me.jfenn.bingo.platform.inventory.IInventory
import me.jfenn.bingo.platform.item.IItemStackFactory
import me.jfenn.bingo.platform.scope.BingoKoin
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.Container
import net.minecraft.world.level.block.*
import net.minecraft.world.level.block.state.BlockState

open class BlockStateImpl(
    val blockState: BlockState,
) : IBlockState {
    override val block: IRegistryEntry.Block get() = BlockRegistryEntry(
        BuiltInRegistries.BLOCK.wrapAsHolder(blockState.block)
    )

    override val identifier: String get() = BuiltInRegistries.BLOCK.getKey(blockState.block).toString()

    override fun isEmpty(world: IServerWorld, pos: BlockPosition): Boolean {
        return blockState.isAir || blockState.getCollisionShape(world.world, pos.toBlockPos()).isEmpty
    }

    override val isFluid: Boolean
        get() = blockState.block is LiquidBlock

    override fun getInventory(world: IServerWorld, pos: BlockPosition): IInventory? {
        val itemStackFactory = BingoKoin.getScope(world.world.server)?.get<IItemStackFactory>()
            ?: return null
        val inventory: Container? = when (val block = blockState.block) {
            is ChestBlock -> { // includes TrappedChestBlock and double-chests
                ChestBlock.getContainer(block, blockState, world.world, pos.toBlockPos(), true)
            }
            else -> world.world.getBlockEntity(pos.toBlockPos()) as? Container
        }

        return if (inventory != null) {
            InventoryImpl(itemStackFactory, inventory)
        } else null
    }

    override fun canSurvive(world: IServerWorld, pos: BlockPosition): Boolean {
        return blockState.canSurvive(world.world, pos.toBlockPos())
    }

    companion object {
        fun fromBlockState(blockState: BlockState): IBlockState {
            return when (blockState.block) {
                Blocks.LODESTONE -> LodestoneBlockStateImpl(blockState)
                Blocks.TORCH -> TorchBlockStateImpl(blockState)
                Blocks.CHEST -> ChestBlockStateImpl(blockState)
                is PlayerHeadBlock -> PlayerHeadBlockStateImpl(blockState)
                is WallSignBlock -> WallSignBlockStateImpl(blockState)
                else -> BlockStateImpl(blockState)
            }
        }
    }
}
