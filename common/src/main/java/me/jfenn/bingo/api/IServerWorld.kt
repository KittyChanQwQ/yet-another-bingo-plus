package me.jfenn.bingo.api

import net.minecraft.server.world.ServerWorld
import net.minecraft.world.chunk.Chunk
import java.io.Closeable
import java.util.concurrent.CompletableFuture

interface IServerWorldFactory {
    fun forWorld(world: ServerWorld): IServerWorld
}

interface IServerWorld {
    fun addTicket(chunk: Pair<Int, Int>): IChunkTicketHandle

    fun getChunkAsync(chunk: Pair<Int, Int>): CompletableFuture<Chunk?>

    interface IChunkTicketHandle : Closeable
}