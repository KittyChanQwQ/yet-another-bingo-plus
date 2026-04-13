package me.jfenn.bingo.platform.world

import me.jfenn.bingo.platform.block.BlockPosition
import me.jfenn.bingo.platform.block.ChunkPosition
import me.jfenn.bingo.platform.block.IBlockState

interface IChunk {
    val pos: ChunkPosition
    fun getBlockPos(offsetX: Int, y: Int, offsetZ: Int): BlockPosition
    fun getBlockState(pos: BlockPosition): IBlockState
    fun getHeightmap(type: IChunkHeightmap.Type): IChunkHeightmap
}

interface IChunkHeightmap {
    fun get(x: Int, z: Int): Int

    enum class Type {
        MOTION_BLOCKING_NO_LEAVES,
        MOTION_BLOCKING,
        WORLD_SURFACE
    }
}
