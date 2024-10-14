package me.jfenn.bingo.api

import me.jfenn.bingo.api.block.IBlockState
import me.jfenn.bingo.common.config.BlockPosition
import net.minecraft.server.MinecraftServer
import net.minecraft.server.world.ServerWorld
import net.minecraft.world.chunk.Chunk
import java.io.Closeable
import java.nio.file.Path
import java.util.concurrent.CompletableFuture

interface IServerWorldFactory {
    fun forWorld(world: ServerWorld): IServerWorld
    fun listWorlds(server: MinecraftServer): List<IServerWorld>
    fun recreateWorlds(server: MinecraftServer, seed: Long, callback: () -> Unit)
}

interface IServerWorld {

    val identifier: String

    val directory: Path

    val world: ServerWorld

    fun getBlockState(pos: BlockPosition): IBlockState

    fun addTicket(chunk: Pair<Int, Int>): IChunkTicketHandle

    fun getChunkAsync(chunk: Pair<Int, Int>): CompletableFuture<Chunk?>

    fun close()

    interface IChunkTicketHandle : Closeable
}