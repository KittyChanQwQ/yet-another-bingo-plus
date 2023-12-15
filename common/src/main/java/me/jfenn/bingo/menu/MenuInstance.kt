package me.jfenn.bingo.menu

import me.jfenn.bingo.common.LOBBY_WORLD_ID
import me.jfenn.bingo.config.BlockPosition
import me.jfenn.bingo.event.InteractionEntityEvents
import me.jfenn.bingo.state.BingoContext
import net.minecraft.entity.Entity
import net.minecraft.nbt.NbtCompound
import net.minecraft.util.math.*
import org.slf4j.LoggerFactory

class MenuInstance(
    val ctx: BingoContext,
    val spawnPos: BlockPosition,
) {

    companion object {
        val log = LoggerFactory.getLogger(this::class.java)!!
    }

    // if the lobby world doesn't exist, the mod should error before this point!
    val world = ctx.server.worlds.find { it.registryKey.value == LOBBY_WORLD_ID }!!

    val menuComponent = component {
        registerPlayerCount(Vec3d(0.0, spawnPos.y + 3.0, -4.45))
        registerMenuPage(Vec3d(0.0, spawnPos.y + 2.0, -4.45), ctx)
        registerPlayerMenu(Vec3d(4.0, spawnPos.y + 3.0, 4.45), Direction.NORTH)
    }

    fun tick() {
        menuComponent.tick(this)
    }

    fun markDirty() {
        menuComponent.markDirty()
    }

    fun cleanup() {
        menuComponent.despawn()
        world.chunkManager.removePersistentTickets()
    }

    fun <T: Entity> spawn(info: MenuEntityHandle<T>, nbt: NbtCompound = NbtCompound()): T? {
        var entity: T? = world.getEntity(info.id) as? T

        val chunkPos = ChunkPos.toLong(BlockPos(info.pos.x.toInt(), 0, info.pos.z.toInt()))
        val isLoaded = world.isChunkLoaded(chunkPos)
        if (!isLoaded) return null

        if (entity == null) {
            entity = info.type.create(world)!!
            entity.uuid = info.id
            world.spawnEntity(entity)
        }

        if (entity.type != info.type) {
            log.error("Entity type ${info.type} does not match the entity being updated")
            return null
        }

        entity.readNbt(nbt)
        info.onUpdate(entity)
        return entity
    }

    fun despawn(info: MenuEntityHandle<*>) {
        val entity = world.getEntity(info.id)
        if (entity != null && entity.type == info.type) {
            InteractionEntityEvents.INTERACT_LISTENERS.remove(info.id)
            entity.kill()
        } else {
            log.error("Entity type ${info.type} does not match the entity being despawned")
        }
    }

}