package me.jfenn.bingo.platform

import me.jfenn.bingo.platform.block.BlockPosition
import me.jfenn.bingo.platform.block.ChunkPosition
import me.jfenn.bingo.platform.block.IBlockState
import me.jfenn.bingo.platform.world.IBlockEntity
import me.jfenn.bingo.platform.world.IChunk
import net.minecraft.server.level.ServerLevel
import java.io.Closeable
import java.nio.file.Path
import java.util.concurrent.CompletableFuture

interface IServerWorldFactory {
    val overworld: IServerWorld
    val baseDir: Path
    val serverDataDir: Path
    fun getLevelSaveDir(worldId: Identifier): Path?
    fun getLevelDataDir(worldId: Identifier): Path?
    var seed: Long
    fun forWorld(world: ServerLevel): IServerWorld
    fun listWorlds(): List<IServerWorld>
    fun copyFromTemplate(templatePath: Path)
    fun recreateWorlds(callback: () -> Unit)
}

interface IServerWorld {

    val identifier: String

    val directory: Path

    val world: ServerLevel
    val seed: Long

    val worldBorder: IWorldBorder
    val coordinateScale: Double
    val logicalHeight: Int
    val bottomY: Int
    val seaLevel: Int
    val spawnPos: BlockPosition
    val hasCeiling: Boolean
    val isOverworld: Boolean

    var timeOfDay: Long

    fun createBlockState(identifier: String): IBlockState
    fun createBlockState(type: IBlockState.Type): IBlockState
    fun getBlockState(pos: BlockPosition): IBlockState
    fun setBlockState(pos: BlockPosition, blockState: IBlockState)
    fun getBlockEntity(pos: BlockPosition): IBlockEntity?
    fun isAir(pos: BlockPosition): Boolean
    fun isEmptyCollision(pos: BlockPosition): Boolean
    fun getBiome(pos: BlockPosition): IRegistryEntry.Biome

    fun addTicket(chunk: ChunkPosition): IChunkTicketHandle

    fun isChunkLoaded(chunk: ChunkPosition): Boolean
    fun getChunk(blockPosition: BlockPosition): IChunk = getChunkSync(blockPosition.toChunkPos())
    fun getChunkSync(chunk: ChunkPosition): IChunk
    fun getChunkAsync(chunk: ChunkPosition): CompletableFuture<IChunk?>

    fun close()

    interface IChunkTicketHandle : Closeable
}

interface IWorldBorder {
    val centerX: Double
    val centerZ: Double
    val maxRadius: Int
    fun contains(blockPos: BlockPosition): Boolean
}