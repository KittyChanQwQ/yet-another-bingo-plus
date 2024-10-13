package me.jfenn.bingo.api

import me.jfenn.bingo.api.block.IBlockState
import me.jfenn.bingo.common.config.BlockPosition
import net.minecraft.server.world.ServerWorld
import net.minecraft.world.chunk.Chunk
import java.io.Closeable
import java.util.concurrent.CompletableFuture

interface IServerWorldFactory {
    fun forWorld(world: ServerWorld): IServerWorld
}

interface IServerWorld {

    fun getBlockState(pos: BlockPosition): IBlockState

    fun addTicket(chunk: Pair<Int, Int>): IChunkTicketHandle

    fun getChunkAsync(chunk: Pair<Int, Int>): CompletableFuture<Chunk?>

    interface IChunkTicketHandle : Closeable
}