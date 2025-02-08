package me.jfenn.bingo.impl

import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer
import me.jfenn.bingo.platform.IPersistentStateManager
import me.jfenn.bingo.platform.IPersistentStateType
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtOps
import net.minecraft.nbt.StringNbtReader
import net.minecraft.server.world.ServerWorld
import net.minecraft.world.PersistentState
import net.minecraft.world.PersistentStateType
import org.slf4j.Logger
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KType

class PersistentStateManager(
    private val logger: Logger,
) : IPersistentStateManager {
    private var persistentStateTypes = ConcurrentHashMap<String, PersistentStateTypeImpl<*>>()

    override fun <T : Any> register(id: String, kType: KType, default: () -> T): IPersistentStateType<T> {
        val type = persistentStateTypes.getOrPut(id) {
            @Suppress("UNCHECKED_CAST")
            val serializer = serializer(kType) as KSerializer<T>

            val type: PersistentStateType<PersistentStateImpl<T>> = PersistentStateType(
                id,
                {
                    logger.info("[PersistentStateManager] Creating default $id state...")
                    PersistentStateImpl(default())
                },
                NbtCompound.CODEC.xmap(
                    { nbt ->
                        logger.debug("[PersistentStateManager] Decoding $id...")
                        val obj = IPersistentStateManager.nbtToJson(nbt)
                        val value = IPersistentStateManager.json.decodeFromJsonElement(serializer, obj)
                        PersistentStateImpl(value)
                    },
                    {
                        logger.debug("[PersistentStateManager] Encoding $id...")
                        val string = IPersistentStateManager.json.encodeToString(serializer, it.value)
                        val compound = StringNbtReader.read(NbtOps.INSTANCE, string)
                        compound as NbtCompound
                    }
                ),
                null,
            )

            PersistentStateTypeImpl(id, type)
        }

        @Suppress("UNCHECKED_CAST")
        return type as IPersistentStateType<T>
    }

    override fun <T : Any> getFromWorld(
        world: ServerWorld,
        type: IPersistentStateType<T>,
    ): T {
        require(type is PersistentStateTypeImpl)
        val state = world.persistentStateManager.getOrCreate(type.type)
        state.markDirty()
        return state.value
    }

    override fun <T : Any> put(world: ServerWorld, type: IPersistentStateType<T>, value: T) {
        require(type is PersistentStateTypeImpl)
        val state = PersistentStateImpl(value)
        world.persistentStateManager.set(type.type, state)
        state.markDirty()
    }
}

class PersistentStateTypeImpl<T: Any>(
    val id: String,
    val type: PersistentStateType<PersistentStateImpl<T>>,
): IPersistentStateType<T>

class PersistentStateImpl<T: Any>(
    val value: T
) : PersistentState() {
    override fun isDirty(): Boolean {
        return true
    }
}
