package me.jfenn.bingo.impl.world

import me.jfenn.bingo.impl.block.BlockStateImpl
import me.jfenn.bingo.platform.block.BlockPosition
import me.jfenn.bingo.platform.block.ChunkPosition
import me.jfenn.bingo.platform.block.IBlockState
import me.jfenn.bingo.platform.world.IChunk
import me.jfenn.bingo.platform.world.IChunkHeightmap
import net.minecraft.world.level.chunk.ChunkAccess
import net.minecraft.world.level.levelgen.Heightmap

class ChunkImpl(
    private val chunk: ChunkAccess,
) : IChunk {
    override val pos: ChunkPosition
        get() = ChunkPosition(chunk.pos.x, chunk.pos.z)

    override fun getBlockPos(
        offsetX: Int,
        y: Int,
        offsetZ: Int
    ): BlockPosition {
        return BlockPosition.fromBlockPos(
            chunk.pos.getBlockAt(offsetX, y, offsetZ)
        )
    }

    override fun getBlockState(pos: BlockPosition): IBlockState {
        return BlockStateImpl(chunk.getBlockState(pos.toBlockPos()))
    }

    override fun getHeightmap(type: IChunkHeightmap.Type): IChunkHeightmap {
        return ChunkHeightmapImpl(
            chunk.getOrCreateHeightmapUnprimed(
                when (type) {
                    IChunkHeightmap.Type.MOTION_BLOCKING_NO_LEAVES -> Heightmap.Types.MOTION_BLOCKING_NO_LEAVES
                    IChunkHeightmap.Type.MOTION_BLOCKING -> Heightmap.Types.MOTION_BLOCKING
                    IChunkHeightmap.Type.WORLD_SURFACE -> Heightmap.Types.WORLD_SURFACE
                }
            )
        )
    }
}
