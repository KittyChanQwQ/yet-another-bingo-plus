package me.jfenn.bingo.common.menu

import me.jfenn.bingo.common.config.BlockPosition
import me.jfenn.bingo.common.event.InteractionEntityEvents
import me.jfenn.bingo.common.lobbyWorld
import net.minecraft.entity.Entity
import net.minecraft.nbt.NbtCompound
import net.minecraft.server.MinecraftServer
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.ChunkPos
import net.minecraft.util.math.Vec3d
import org.koin.core.scope.Scope
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*

class MenuInstance(
    private val log: Logger,
    koinScope: Scope,
    private val server: MinecraftServer,
    private val interactionEntityEvents: InteractionEntityEvents,
    private val spawnPos: BlockPosition,
) {

    val instanceTag = "bingo-${UUID.randomUUID()}"

    companion object {
        val log = LoggerFactory.getLogger(this::class.java)!!
    }

    // if the lobby world doesn't exist, the mod should error before this point!
    val world get() = server.lobbyWorld!!

    private val menuComponent = component(koinScope) {
        registerPlayerCount(Vec3d(0.0, spawnPos.y + 3.0, -4.45))
        registerMenuPage(Vec3d(0.0, spawnPos.y + 2.0, -4.45))
    }

    fun tick() {
        menuComponent.tick(this)
    }

    fun markDirty() {
        menuComponent.markDirty()
    }

    fun cleanup() {
        menuComponent.despawn()
    }

    fun <T: Entity> spawn(info: MenuEntityHandle<T>, nbt: NbtCompound): T? {
        @Suppress("UNCHECKED_CAST")
        var entity: T? = world.getEntity(info.id) as? T

        val chunkPos = ChunkPos.toLong(BlockPos(info.pos.x.toInt(), 0, info.pos.z.toInt()))
        val isLoaded = world.isChunkLoaded(chunkPos)
        if (!isLoaded) return null

        if (entity == null) {
            entity = info.type.create(world)!!
            entity.uuid = info.id
            entity.readNbt(nbt)
            entity.addCommandTag(instanceTag)
            // Try to spawn the entity into the world...
            val isSuccess = world.spawnEntity(entity)
            // If the entity cannot be spawned, discard it and return null
            // (the MenuComponent should then be marked dirty, and will try again on the next tick)
            if (!isSuccess) {
                log.error("Unable to spawn ${info.type} menu entity - discarding...")
                entity.discard()
                return null
            }
        } else {
            entity.readNbt(nbt)
            entity.addCommandTag(instanceTag)
        }

        if (entity.type != info.type) {
            log.error("Entity type ${info.type} does not match the entity being updated")
            return null
        }

        info.onUpdate(entity)
        return entity
    }

    fun despawn(info: MenuEntityHandle<*>) {
        val entity = world.getEntity(info.id)
        if (entity != null && entity.type == info.type) {
            interactionEntityEvents.INTERACT_LISTENERS.remove(info.id)
            entity.kill()
        } else {
            log.error("Entity type ${info.type} does not match the entity being despawned")
        }
    }

}