package me.jfenn.bingo.impl

import com.mojang.brigadier.StringReader
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import me.jfenn.bingo.platform.IJsonSerializers
import me.jfenn.bingo.platform.IPersistentStateManager
import me.jfenn.bingo.platform.IPersistentStateType
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.StringNbtReader
import net.minecraft.server.world.ServerWorld
import net.minecraft.world.PersistentState
import org.slf4j.Logger
import kotlin.reflect.KType

class PersistentStateManager(
    private val logger: Logger,
    private val serializers: IJsonSerializers,
) : IPersistentStateManager {
    override fun <T : Any> register(id: String, kType: KType, default: () -> T): IPersistentStateType<T> {
        val serializer = @Suppress("UNCHECKED_CAST") (serializer(kType) as KSerializer<T>)
        val type = PersistentState.Type(
            {
                logger.info("[PersistentStateManager] Creating default $id state...")
                PersistentStateImpl(default(), serializer, serializers.json, logger)
            },
            { nbt ->
                logger.info("[PersistentStateManager] Reading $id state from save data...")
                val obj = IPersistentStateManager.nbtToJson(nbt)
                val value = serializers.json.decodeFromJsonElement(serializer, obj)
                PersistentStateImpl(value, serializer, serializers.json, logger)
            },
            null,
        )

        return PersistentStateType(id, serializer, type)
    }

    override fun <T : Any> getFromWorld(world: ServerWorld, type: IPersistentStateType<T>): T {
        require(type is PersistentStateType)
        val state = world.persistentStateManager.getOrCreate(type.type, type.id)
        state.markDirty()
        return state.value
    }

    override fun <T : Any> put(world: ServerWorld, type: IPersistentStateType<T>, value: T) {
        require(type is PersistentStateType)
        val state = PersistentStateImpl(value, type.serializer, serializers.json, logger)
        world.persistentStateManager.set(type.id, state)
        state.markDirty()
    }
}

class PersistentStateType<T: Any>(
    val id: String,
    val serializer: KSerializer<T>,
    val type: PersistentState.Type<PersistentStateImpl<T>>,
): IPersistentStateType<T>

class PersistentStateImpl<T: Any>(
    val value: T,
    private val serializer: KSerializer<T>,
    private val json: Json,
    private val logger: Logger,
) : PersistentState() {

    override fun writeNbt(nbt: NbtCompound): NbtCompound {
        try {
            logger.debug("[PersistentStateImpl] Writing {} state", value::class.simpleName)
            val string = json.encodeToString(serializer, value)
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
