package me.jfenn.bingo.platform.block

import me.jfenn.bingo.platform.IRegistryEntry
import me.jfenn.bingo.platform.IServerWorld
import me.jfenn.bingo.platform.inventory.IInventory
import org.joml.Vector3i

interface IBlockState {
    sealed interface Type

    val identifier: String
    val block: IRegistryEntry.Block
    fun isEmpty(world: IServerWorld, pos: BlockPosition): Boolean
    val isFluid: Boolean
    fun getInventory(world: IServerWorld, pos: BlockPosition): IInventory?
    fun canSurvive(world: IServerWorld, pos: BlockPosition): Boolean
}

interface IWallSignBlockState: IBlockState {
    val facing: Vector3i
}

interface IPlayerHeadBlockState: IBlockState {
    val rotation: Int
}

interface ILodestoneBlockState: IBlockState

interface IChestBlockState: IBlockState {
    object Type: IBlockState.Type
}

interface ITorchBlockState: IBlockState {
    object Type: IBlockState.Type
}
