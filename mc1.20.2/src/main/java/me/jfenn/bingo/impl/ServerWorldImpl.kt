package me.jfenn.bingo.impl

import me.jfenn.bingo.platform.IServerWorld
import me.jfenn.bingo.platform.IServerWorldFactory
import me.jfenn.bingo.platform.block.IBlockState
import me.jfenn.bingo.common.MOD_ID_BINGO
import me.jfenn.bingo.platform.block.BlockPosition
import me.jfenn.bingo.impl.block.BlockStateImpl
import me.jfenn.bingo.mixin.LevelPropertiesAccessor
import me.jfenn.bingo.mixin.ServerChunkManagerAccessor
import me.jfenn.bingo.mixin.ThreadedAnvilChunkStorageAccessor
import net.minecraft.server.MinecraftServer
import net.minecraft.server.world.ChunkTicketType
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.Unit
import net.minecraft.util.math.ChunkPos
import net.minecraft.world.chunk.Chunk
import net.minecraft.world.chunk.ChunkStatus
import net.minecraft.world.level.LevelProperties
import java.nio.file.Path
import java.util.*
import java.util.concurrent.CompletableFuture
import kotlin.jvm.optionals.getOrNull

class ServerWorldImpl(
    override val world: ServerWorld
): IServerWorld {
    companion object : IServerWorldFactory {
        override fun forWorld(world: ServerWorld): IServerWorld = ServerWorldImpl(world)
        override fun listWorlds(server: MinecraftServer): List<IServerWorld> {
            return server.worlds.map { forWorld(it) }
        }

        override fun recreateWorlds(
            server: MinecraftServer,
            seed: Long,
            callback: () -> kotlin.Unit
        ) {
            // First, ensure the world/data is saved
            server.saveAll(false, true, true)

            // Remove scoreboard objectives to avoid a crash when data is reloaded in loadWorld()
            server.scoreboard.objectives.forEach { server.scoreboard.removeObjective(it) }

            server.saving = true
            try {
                server.cancelTasks()
                for (world in listWorlds(server)) {
                    world.close()
                    arrayOf("region", "poi", "entities")
                        .map { world.directory.resolve(it).toFile() }
                        .filter { it.exists() }
                        .onEach { it.deleteRecursively() }
                }
                callback()
                val levelPropertiesAccessor = (server.saveProperties as LevelProperties) as LevelPropertiesAccessor
                levelPropertiesAccessor.generatorOptions = levelPropertiesAccessor.generatorOptions.withSeed(OptionalLong.of(seed))
                server.loadWorld()
            } finally {
                server.saving = false
            }
        }

        private val TICKET_HELD = ChunkTicketType.create("$MOD_ID_BINGO-held") { _: Any?, _: Any? -> 0 }
        private val TICKET_ASYNC = ChunkTicketType.create("$MOD_ID_BINGO-async") { _: Any?, _: Any? -> 0 }
    }

    override val identifier: String
        get() = world.registryKey.value.toString()

    override val directory: Path
        get() = world.server.session.getWorldDirectory(world.registryKey)

    override fun getBlockState(pos: BlockPosition): IBlockState {
        return world.getBlockState(pos.toBlockPos())
            .let { BlockStateImpl.fromBlockState(it) }
    }

    private val taskExecutor = Executors.createServerTaskExecutor(world.server)

    override fun addTicket(chunk: Pair<Int, Int>): IServerWorld.IChunkTicketHandle {
        world.chunkManager.addTicket(TICKET_HELD, ChunkPos(chunk.first, chunk.second), 0, Unit.INSTANCE)
        return ChunkTicketHandle(chunk)
    }

    inner class ChunkTicketHandle(
        private val chunk: Pair<Int, Int>
    ) : IServerWorld.IChunkTicketHandle {
        override fun close() {
            world.chunkManager.removeTicket(TICKET_HELD, ChunkPos(chunk.first, chunk.second), 0, Unit.INSTANCE)
        }
    }

    override fun getChunkAsync(chunk: Pair<Int, Int>): CompletableFuture<Chunk?> {
        if (!world.server.isOnThread) {
            return CompletableFuture.supplyAsync({ getChunkAsync(chunk) }, world.server)
                .thenCompose({ it })
        }

        // Add a ticket for the chunk
        val chunkManager = world.chunkManager
        chunkManager.addTicket(TICKET_ASYNC, ChunkPos(chunk.first, chunk.second), 0, Unit.INSTANCE)

        (chunkManager as ServerChunkManagerAccessor).invokeUpdateChunks()

        val threadedAnvilChunkStorage = world.chunkManager.threadedAnvilChunkStorage
        val chunkHolder = (threadedAnvilChunkStorage as ThreadedAnvilChunkStorageAccessor)
            .invokeGetChunkHolder(ChunkPos(chunk.first, chunk.second).toLong())

        val chunkFuture = chunkHolder?.getChunkAt(ChunkStatus.FULL, threadedAnvilChunkStorage)
            ?.thenApply { it.left().getOrNull() }
            ?: CompletableFuture.completedFuture<Chunk?>(null)

        // Remove the chunk ticket once loaded
        chunkFuture.whenCompleteAsync({ _, _ ->
            chunkManager.removeTicket(TICKET_ASYNC, ChunkPos(chunk.first, chunk.second), 0, Unit.INSTANCE)
        }, taskExecutor)

        return chunkFuture
    }

    override fun close() {
        world.close()
    }
}
