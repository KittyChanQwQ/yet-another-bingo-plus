package me.jfenn.bingo.impl

import com.mojang.brigadier.StringReader
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer
import me.jfenn.bingo.platform.IPersistentStateManager
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.StringNbtReader
import net.minecraft.registry.RegistryWrapper
import net.minecraft.server.world.ServerWorld
import net.minecraft.world.PersistentState
import kotlin.reflect.KClass

class PersistentStateManager : IPersistentStateManager {
    override fun <T : Any> getFromWorld(
        world: ServerWorld,
        id: String,
        clazz: KClass<T>,
        default: () -> T,
    ): T {
        @OptIn(InternalSerializationApi::class)
        val serializer = clazz.serializer()
        val type = PersistentState.Type(
            { PersistentStateImpl(default(), serializer) },
            { nbt, _ ->
                val obj = IPersistentStateManager.nbtToJson(nbt)
                val value = IPersistentStateManager.json.decodeFromJsonElement(serializer, obj)
                PersistentStateImpl(value, serializer)
            },
            null,
        )

        val state = world.persistentStateManager.getOrCreate(type, id)
        state.markDirty()
        return state.value
    }
}

class PersistentStateImpl<T: Any>(
    val value: T,
    private val serializer: KSerializer<T>,
) : PersistentState() {

    override fun writeNbt(nbt: NbtCompound, registryLookup: RegistryWrapper.WrapperLookup?): NbtCompound {
        val string = IPersistentStateManager.json.encodeToString(serializer, value)
        val compound = StringNbtReader(StringReader(string)).parseCompound()
        return nbt.copyFrom(compound)
    }

    override fun isDirty(): Boolean {
        return true
    }
}
