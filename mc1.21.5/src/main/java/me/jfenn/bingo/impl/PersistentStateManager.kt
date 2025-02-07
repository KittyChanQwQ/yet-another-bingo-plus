package me.jfenn.bingo.impl

import com.mojang.datafixers.util.Pair
import com.mojang.serialization.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer
import me.jfenn.bingo.platform.IPersistentStateManager
import me.jfenn.bingo.platform.IPersistentStateType
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
        return persistentStateTypes.getOrPut(id) {
            @Suppress("UNCHECKED_CAST")
            val serializer = serializer(kType) as KSerializer<T>

            val type: PersistentStateType<PersistentStateImpl<T>> = PersistentStateType(
                id,
                {
                    logger.info("[PersistentStateManager] Creating default $id state...")
                    PersistentStateImpl(default())
                },
                Codec.of(
                    JsonEncoder(serializer, logger),
                    JsonDecoder(serializer, logger),
                ),
                null,
            )

            PersistentStateTypeImpl(id, type)
        } as IPersistentStateType<T>
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

class JsonEncoder<A: Any>(
    private val serializer: KSerializer<A>,
    private val logger: Logger,
) : Encoder<PersistentStateImpl<A>> {
    override fun <T : Any?> encode(value: PersistentStateImpl<A>, ops: DynamicOps<T>, prefix: T): DataResult<T> {
        return try {
            logger.debug("[JsonEncoder] Writing {} state", value::class.simpleName)
            val string = IPersistentStateManager.json.encodeToString(serializer, value.value)
            Codec.STRING.encode(string, ops, prefix).also {
                logger.debug("[JsonEncoder] Writing {} state: Success!", value::class.simpleName)
            }
        } catch (e: Throwable) {
            logger.debug("[JsonEncoder] Error serializing {} state:", value::class.simpleName, e)
            DataResult.error { e.message }
        }
    }
}

class JsonDecoder<A: Any>(
    private val serializer: KSerializer<A>,
    private val logger: Logger,
) : Decoder<PersistentStateImpl<A>> {
    override fun <T : Any?> decode(ops: DynamicOps<T>, input: T): DataResult<Pair<PersistentStateImpl<A>, T>> {
        return Codec.STRING.decode(ops, input)
            .flatMap {
                val json = it.first
                try {
                    logger.info("[JsonDecoder] Reading state from save data...")
                    val value = IPersistentStateManager.json.decodeFromString(serializer, json)
                    DataResult.success(Pair.of(PersistentStateImpl(value), it.second))
                } catch (e: Throwable) {
                    logger.error("[JsonDecoder] Error reading save data:", e)
                    DataResult.error { e.message }
                }
            }
    }
}

class PersistentStateImpl<T: Any>(
    val value: T
) : PersistentState() {
    override fun isDirty(): Boolean {
        return true
    }
}
