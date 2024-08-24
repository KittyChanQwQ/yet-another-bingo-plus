package me.jfenn.bingo.common.menu

import me.jfenn.bingo.api.IEntity
import me.jfenn.bingo.api.IEntityManager
import me.jfenn.bingo.common.config.BlockPosition
import me.jfenn.bingo.common.event.InteractionEntityEvents
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.ChunkPos
import org.joml.Vector3d
import org.koin.core.scope.Scope
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class MenuInstance(
    private val log: Logger,
    koinScope: Scope,
    private val world: ServerWorld,
    private val entityManager: IEntityManager,
    private val interactionEntityEvents: InteractionEntityEvents,
    private val menuPos: BlockPosition,
    private val instanceTag: String,
) {

    companion object {
        val log = LoggerFactory.getLogger(this::class.java)!!
    }

    private val menuComponent = component(koinScope) {
        registerPlayerCount(Vector3d(menuPos.x.toDouble(), menuPos.y + 2.0, menuPos.z + 0.55))
        registerMenuPage(Vector3d(menuPos.x.toDouble(), menuPos.y + 1.0, menuPos.z + 0.55))
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

    fun <T: IEntity> spawn(info: MenuEntityHandle<T>): T? {
        @Suppress("UNCHECKED_CAST")
        var entity: T? = entityManager.getEntity(world, info.id)
            ?.takeIf { it.type == info.type }
                as? T

        if (entity == null) {
            entity = entityManager.createEntity(info.type, world)
            entity.uuid = info.id
            entity.commandTags = setOf(instanceTag)
            info.init.invoke(entity)

            val chunkPos = ChunkPos.toLong(BlockPos(entity.pos.x.toInt(), 0, entity.pos.z.toInt()))
            val isLoaded = world.isChunkLoaded(chunkPos)
            if (!isLoaded) {
                entity.discard()
                return null
            }

            // Try to spawn the entity into the world...
            val isSuccess = entityManager.spawnEntity(world, entity)
            // If the entity cannot be spawned, discard it and return null
            // (the MenuComponent should then be marked dirty, and will try again on the next tick)
            if (!isSuccess) {
                log.error("Unable to spawn ${info.type} menu entity - discarding...")
                entity.discard()
                return null
            }
        } else {
            entity.commandTags = setOf(instanceTag)
            info.init.invoke(entity)
        }

        if (entity.type != info.type) {
            log.error("Entity type ${info.type} does not match the entity being updated")
            return null
        }

        info.onUpdate(entity)
        return entity
    }

    fun despawn(info: MenuEntityHandle<*>) {
        interactionEntityEvents.INTERACT_LISTENERS.remove(info.id)
        val entity = entityManager.getEntity(world, info.id)
        entity?.discard()
    }

}