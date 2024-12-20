package me.jfenn.bingo.impl

import com.mojang.brigadier.StringReader
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer
import me.jfenn.bingo.platform.IPersistentStateManager
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.StringNbtReader
import net.minecraft.server.world.ServerWorld
import net.minecraft.world.PersistentState
import org.slf4j.Logger
import kotlin.reflect.KType

class PersistentStateManager(
    private val logger: Logger,
) : IPersistentStateManager {
    override fun <T : Any> getFromWorld(
        world: ServerWorld,
        id: String,
        kType: KType,
        default: () -> T,
    ): T {
        @Suppress("UNCHECKED_CAST")
        val serializer = serializer(kType) as KSerializer<T>
        val type = PersistentState.Type(
            {
                logger.info("[PersistentStateManager] Creating default $id state...")
                PersistentStateImpl(default(), serializer, logger)
            },
            { nbt ->
                logger.info("[PersistentStateManager] Reading $id state from save data...")
                val obj = IPersistentStateManager.nbtToJson(nbt)
                val value = IPersistentStateManager.json.decodeFromJsonElement(serializer, obj)
                PersistentStateImpl(value, serializer, logger)
            },
            null,
        )

        val state = world.persistentStateManager.getOrCreate(type, id)
        state.markDirty()
        return state.value
    }

    override fun <T : Any> put(world: ServerWorld, id: String, kType: KType, value: T) {
        @Suppress("UNCHECKED_CAST")
        val serializer = serializer(kType) as KSerializer<T>

        val state = PersistentStateImpl(value, serializer, logger)
        world.persistentStateManager.set(id, state)
        state.markDirty()
    }
}

class PersistentStateImpl<T: Any>(
    val value: T,
    private val serializer: KSerializer<T>,
    private val logger: Logger,
) : PersistentState() {

    override fun writeNbt(nbt: NbtCompound): NbtCompound {
        try {
            logger.debug("[PersistentStateImpl] Writing {} state", value::class.simpleName)
            val string = IPersistentStateManager.json.encodeToString(serializer, value)
            val compound = StringNbtReader(StringReader(string)).parseCompound()
            return nbt.copyFrom(compound).also {
                logger.debug("[PersistentStateImpl] Writing {} state: Success!", value::class.simpleName)
            }
        } catch (e: Throwable) {
            logger.debug("[PersistentStateImpl] Error serializing {} state:", value::class.simpleName, e)
            return nbt
        }
    }

    override fun isDirty(): Boolean {
        return true
    }
}
