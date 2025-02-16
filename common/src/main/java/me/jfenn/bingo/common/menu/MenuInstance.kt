package me.jfenn.bingo.common.menu

import me.jfenn.bingo.platform.IEntity
import me.jfenn.bingo.platform.IEntityManager
import me.jfenn.bingo.common.event.InteractionEntityEvents
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.ChunkPos
import org.joml.Matrix4f
import org.joml.Vector3d
import org.joml.Vector3f
import org.joml.Vector4f
import org.koin.core.scope.Scope
import org.slf4j.Logger
import org.slf4j.LoggerFactory

internal class MenuInstance(
    private val log: Logger,
    koinScope: Scope,
    private val world: ServerWorld,
    private val entityManager: IEntityManager,
    private val interactionEntityEvents: InteractionEntityEvents,
    private val matrix: Matrix4f,
    private val instanceTag: String,
) {

    companion object {
        val log = LoggerFactory.getLogger(this::class.java)!!
    }

    private val menuComponent = component(koinScope) {
        registerMenuPage(Vector3d(0.5, 2.0, 0.05))
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

    private fun IEntity.resetPos() {
        pos = Vector3d()
        yaw = 0f
    }

    private fun IEntity.transformPos() {
        val pos4f = with(pos) { Vector4f(x.toFloat(), y.toFloat(), z.toFloat(), 1f) }
        pos = matrix.transform(pos4f).let { Vector3d(it.x.toDouble(), it.y.toDouble(), it.z.toDouble()) }

        val angles = Vector3f()
        matrix.getEulerAnglesXYZ(angles)
        yaw += Math.toDegrees(-angles.y.toDouble()).toFloat()
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
            entity.resetPos()
            info.init.invoke(entity)
            entity.transformPos()

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
                log.debug("Unable to spawn {} menu entity - discarding...", info.type)
                entity.discard()
                return null
            }
        } else {
            entity.commandTags = setOf(instanceTag)
            entity.resetPos()
            info.init.invoke(entity)
            entity.transformPos()
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