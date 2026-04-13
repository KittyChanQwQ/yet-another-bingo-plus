package me.jfenn.bingo.impl

import me.jfenn.bingo.common.MOD_ID_BINGO
import me.jfenn.bingo.common.utils.measureTime
import me.jfenn.bingo.impl.block.BlockStateImpl
import me.jfenn.bingo.impl.world.ChestBlockEntityImpl
import me.jfenn.bingo.impl.world.ChunkImpl
import me.jfenn.bingo.mixinhelper.ServerChunkManagerMixinHelper
import me.jfenn.bingo.platform.*
import me.jfenn.bingo.platform.block.*
import me.jfenn.bingo.platform.item.IItemStackFactory
import me.jfenn.bingo.platform.scope.BingoKoin
import me.jfenn.bingo.platform.world.IBlockEntity
import me.jfenn.bingo.platform.world.IChunk
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.Ticket
import net.minecraft.server.level.TicketType
import net.minecraft.world.clock.WorldClocks
import net.minecraft.world.entity.projectile.ProjectileUtil
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.entity.ChestBlockEntity
import net.minecraft.world.level.border.WorldBorder
import net.minecraft.world.level.chunk.status.ChunkStatus
import net.minecraft.world.level.dimension.end.EnderDragonFight
import net.minecraft.world.phys.Vec3
import org.joml.Vector3d
import org.slf4j.Logger
import java.nio.file.Path
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.function.Function
import kotlin.jvm.optionals.getOrNull

internal object ChunkTickets {
    lateinit var TICKET_HELD: TicketType
    lateinit var TICKET_ASYNC: TicketType

    fun init() {
        TICKET_HELD = Registry.register(BuiltInRegistries.TICKET_TYPE, "$MOD_ID_BINGO-held", TicketType(0, TicketType.FLAG_PERSIST or TicketType.FLAG_LOADING))
        TICKET_ASYNC = Registry.register(BuiltInRegistries.TICKET_TYPE, "$MOD_ID_BINGO-async", TicketType(0, TicketType.FLAG_PERSIST or TicketType.FLAG_LOADING))
    }
}

class ServerWorldFactory(
    private val log: Logger,
    private val server: MinecraftServer,
    private val itemStackFactory: IItemStackFactory,
) : IServerWorldFactory {
    override val overworld: IServerWorld
        get() = forWorld(server.overworld())

    override fun forWorld(world: ServerLevel): IServerWorld = ServerWorldImpl(world, itemStackFactory)
    override fun listWorlds(): List<IServerWorld> {
        return server.allLevels.map { forWorld(it) }
    }

    private fun tickKeepAlive() {
        server.connection.tick()
    }

    override fun recreateWorlds(
        seed: Long,
        callback: () -> Unit
    ) {
        log.debug("[Reset] Started recreateWorlds")
        tickKeepAlive()

        // Remove scoreboard objectives to avoid a crash when data is reloaded in loadWorld()
        server.scoreboard.objectives
            .toList()
            .forEach {
                try {
                    server.scoreboard.removeObjective(it)
                } catch (e: Throwable) {
                    log.error("[Reset] Error removing objective:", e)
                }
            }

        // Clear any ongoing raids
        server.allLevels.forEach { world ->
            world.raids.accessor.raids.values
                .forEach { it.stop() }
        }

        server.accessor.setSaving(true)
        try {
            // First, ensure the world/data is saved
            log.measureTime("[Reset] Saving...") {
                // MinecraftServer::saveAll
                server.playerList.saveAll()

                for (world in listWorlds()) {
                    // ServerWorld::save -> ServerWorld::saveLevel
                    val serverWorld: ServerLevel = world.world
                    serverWorld.chunkSource.dataStorage.saveAndJoin()
                }
            }

            tickKeepAlive()

            server.blockableEventLoopAccessor.invokeDropAllTasks()

            // Use mixins to skip save() calls within world.close(), so that it
            // *only* closes resources without waiting to write to disk
            ServerChunkManagerMixinHelper.shouldCancelSaving = true

            for (world in listWorlds()) {
                log.measureTime("[Reset] Closing ${world.identifier}...") {
                    world.close()
                }

                tickKeepAlive()

                arrayOf("region", "poi", "entities")
                    .map { world.directory.resolve(it).toFile() }
                    .filter { it.exists() }
                    .onEach { it.deleteRecursively() }

                tickKeepAlive()
            }

            callback()
            tickKeepAlive()

            log.measureTime("[Reset] Loading new worlds...") {
                server.worldGenSettings.options().withSeed(OptionalLong.of(seed))
                server.allLevels.forEach {
                    if (it.dragonFight != null) {
                        it.accessor.setDragonFight(EnderDragonFight.createDefault())
                    }
                }
                server.worldGenSettings.accessor.apply {
                    worldOptions = worldOptions.withSeed(OptionalLong.of(seed))
                }
                server.accessor.invokeLoadWorld()
            }
            tickKeepAlive()

            // invoke "#minecraft:load" again, as scoreboards will be reset
            with(server.functions) {
                getTag(Identifier.withDefaultNamespace("load"))
                    .forEach { execute(it, gameLoopSender) }
            }
        } finally {
            server.accessor.setSaving(false)
            ServerChunkManagerMixinHelper.shouldCancelSaving = false
        }
    }
}

class ServerWorldImpl(
    override val world: ServerLevel,
    private val itemStackFactory: IItemStackFactory? = null,
): IServerWorld {

    private val serverOrThrow get() = requireNotNull(world.server) { "ServerWorldImpl: world.server reference is null!" }

    private val itemStackFactoryOrThrow get() = itemStackFactory
        ?: requireNotNull(
            BingoKoin.getScope(serverOrThrow)?.get<IItemStackFactory>()
        )

    override val identifier: String
        get() = world.dimension().identifier().toString()

    override val directory: Path
        get() = serverOrThrow.accessor.storageSource.getDimensionPath(world.dimension())

    override val seed: Long
        get() = world.seed

    override val worldBorder: IWorldBorder
        get() = WorldBorderImpl(world.worldBorder)
    override val coordinateScale: Double
        get() = world.dimensionType().coordinateScale()
    override val logicalHeight: Int
        get() = world.height
    override val bottomY: Int
        get() = world.minY
    override val seaLevel: Int
        get() = world.seaLevel
    override val spawnPos: BlockPosition
        get() = BlockPosition.fromBlockPos(world.respawnData.pos())
    override val hasCeiling: Boolean
        get() = world.dimensionType().hasCeiling()
    override val isOverworld: Boolean
        get() = world.dimension() == Level.OVERWORLD

    override var timeOfDay: Long
        get() = world.overworldClockTime
        set(value) {
            world.registryAccess().get(WorldClocks.OVERWORLD).getOrNull()?.let { holder ->
                world.server.clockManager().setTotalTicks(holder, value)
            }
        }

    override fun createBlockState(identifier: String): IBlockState {
        val block = BuiltInRegistries.BLOCK.get(Identifier.parse(identifier)).get().value()
        return BlockStateImpl.fromBlockState(block.defaultBlockState())
    }

    override fun createBlockState(type: IBlockState.Type): IBlockState {
        val block = when (type) {
            is IChestBlockState.Type -> Blocks.OAK_WALL_SIGN
            is ITorchBlockState.Type -> Blocks.TORCH
        }
        return BlockStateImpl.fromBlockState(block.defaultBlockState())
    }

    override fun getBlockState(pos: BlockPosition): IBlockState {
        return world.getBlockState(pos.toBlockPos())
            .let { BlockStateImpl.fromBlockState(it) }
    }

    override fun setBlockState(pos: BlockPosition, blockState: IBlockState) {
        require(blockState is BlockStateImpl)
        world.setBlockAndUpdate(pos.toBlockPos(), blockState.blockState)
    }

    override fun getBlockEntity(pos: BlockPosition): IBlockEntity? {
        val entity = world.getBlockEntity(pos.toBlockPos()) ?: return null
        return when (entity) {
            is ChestBlockEntity -> ChestBlockEntityImpl(itemStackFactoryOrThrow, entity)
            else -> null
        }
    }

    override fun isAir(pos: BlockPosition): Boolean {
        return world.getBlockState(pos.toBlockPos()).isAir
    }

    override fun isEmptyCollision(pos: BlockPosition): Boolean {
        val blockPos = pos.toBlockPos()
        return world.getBlockState(blockPos).getCollisionShape(world, blockPos).isEmpty
    }

    override fun getEntityHitResult(
        except: IEntity,
        from: Vector3d,
        to: Vector3d,
        aabb: AABB,
        predicate: (IEntity) -> Boolean,
        maxValue: Double
    ): EntityHitResult? {
        require(except is EntityImpl)
        val hit = ProjectileUtil.getEntityHitResult(
            except.entity,
            Vec3(from.x, from.y, from.z),
            Vec3(to.x, to.y, to.z),
            net.minecraft.world.phys.AABB(
                aabb.min.x, aabb.min.y, aabb.min.z,
                aabb.max.x, aabb.max.y, aabb.max.z,
            ),
            { entity -> predicate(EntityImpl(entity)) },
            maxValue,
        )
        return hit?.let { EntityHitResult(EntityImpl(it.entity)) }
    }

    override fun getBiome(pos: BlockPosition): IRegistryEntry.Biome {
        return BiomeRegistryEntry(world.getBiome(pos.toBlockPos()))
    }

    private val taskExecutor = Executors.createServerTaskExecutor(serverOrThrow)

    override fun addTicket(chunk: ChunkPosition): IServerWorld.IChunkTicketHandle {
        val ticket = Ticket(ChunkTickets.TICKET_HELD, 0)
        val chunkPos = chunk.toChunkPos()
        world.chunkSource.addTicket(ticket, chunkPos)
        return ChunkTicketHandle(ticket, chunkPos)
    }

    inner class ChunkTicketHandle(
        private val ticket: Ticket,
        private val chunkPos: ChunkPos
    ) : IServerWorld.IChunkTicketHandle {
        override fun close() {
            world.chunkSource.accessor.ticketStorage.removeTicket(ticket, chunkPos)
        }
    }

    override fun getChunkSync(chunk: ChunkPosition): IChunk {
        return ChunkImpl(world.getChunk(chunk.x, chunk.z))
    }

    override fun getChunkAsync(chunk: ChunkPosition): CompletableFuture<IChunk?> {
        if (!serverOrThrow.isSameThread) {
            return CompletableFuture.supplyAsync({ getChunkAsync(chunk) }, serverOrThrow)
                .thenCompose({ it })
        }

        // Add a ticket for the chunk
        val chunkSource = world.chunkSource
        val chunkPos = chunk.toChunkPos()
        val ticket = Ticket(ChunkTickets.TICKET_ASYNC, 0)
        chunkSource.addTicket(ticket, chunkPos)

        chunkSource.accessor.invokeRunDistanceManagerUpdates()

        val chunkFuture = chunkSource.accessor.invokeGetChunkFutureMainThread(chunk.x, chunk.z, ChunkStatus.FULL, false)
            .thenApplyAsync(Function.identity(), chunkSource.chunkMap.accessor.mainThreadExecutor)

        // Remove the chunk ticket once loaded
        chunkFuture.whenCompleteAsync({ _, _ ->
            chunkSource.accessor.ticketStorage.removeTicket(ticket, chunkPos)
        }, taskExecutor)

        return chunkFuture.thenApply { result ->
            result.orElse(null)?.let { ChunkImpl(it) }
        }
    }

    override fun isChunkLoaded(chunk: ChunkPosition): Boolean {
        return world.isPositionTickingWithEntitiesLoaded(chunk.toChunkPos().pack())
    }

    override fun close() {
        world.close()
    }
}

class WorldBorderImpl(
    val worldBorder: WorldBorder
): IWorldBorder {
    override val centerX: Double by worldBorder::centerX
    override val centerZ: Double by worldBorder::centerZ
    override val maxRadius: Int by worldBorder::absoluteMaxSize
    override fun contains(blockPos: BlockPosition): Boolean = worldBorder.isWithinBounds(blockPos.toBlockPos())
}
