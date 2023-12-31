package me.jfenn.bingo.impl

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.serializer
import me.jfenn.bingo.api.IPersistentStateManager
import me.jfenn.bingo.common.state.SerializedPersistentState
import me.jfenn.bingo.common.utils.json
import net.minecraft.server.world.ServerWorld
import net.minecraft.world.PersistentState
import kotlin.reflect.KClass

class PersistentStateManager : IPersistentStateManager {
    @OptIn(InternalSerializationApi::class)
    override fun <T : PersistentState> getFromWorld(world: ServerWorld, clazz: KClass<T>, id: String): T {
        val serializer = clazz.serializer()

        val stateManager = world.persistentStateManager
        val state = stateManager.getOrCreate(
            { nbt ->
                val obj = SerializedPersistentState.nbtToJson(nbt)
                json.decodeFromJsonElement(serializer, obj)
            },
            { json.decodeFromString(serializer, "{}") },
            id,
        )
        state.markDirty()
        return state
    }
}