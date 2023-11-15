package me.jfenn.bingo.state

import com.mojang.brigadier.StringReader
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import me.jfenn.bingo.MOD_ID
import me.jfenn.bingo.utils.json
import net.minecraft.nbt.*
import net.minecraft.server.world.ServerWorld
import net.minecraft.world.PersistentState

@Serializable
sealed class SerializedPersistentState: PersistentState() {

    companion object {

        fun nbtToJson(nbt: NbtElement?): JsonElement {
            if (nbt == null) return JsonNull
            return when (nbt) {
                is NbtString -> JsonPrimitive(nbt.asString())
                is NbtInt -> JsonPrimitive(nbt.intValue())
                is NbtFloat -> JsonPrimitive(nbt.floatValue())
                is NbtByte -> JsonPrimitive(nbt.byteValue() != 0.toByte())
                is NbtDouble -> JsonPrimitive(nbt.doubleValue())
                // handles NbtList, NbtIntArray, NbtByteArray
                is AbstractNbtList<*> -> JsonArray(nbt.map { nbtToJson(it) })
                is NbtCompound -> JsonObject(nbt.keys.associateWith { key ->
                    nbtToJson(nbt.get(key))
                })
                else -> JsonNull
            }
        }

        inline fun <reified T: SerializedPersistentState> getType() = Type<T>(
            { json.decodeFromString("{}") },
            { nbt ->
                val obj = nbtToJson(nbt)
                json.decodeFromJsonElement(obj)
            },
            null,
        )

        inline fun <reified T: SerializedPersistentState> fromWorld(world: ServerWorld): T {
            val stateManager = world.persistentStateManager
            val state = stateManager.getOrCreate(getType<T>(), MOD_ID)
            state.markDirty()
            return state
        }
    }

    override fun writeNbt(nbt: NbtCompound): NbtCompound {
        val string = json.encodeToString(this)
        val compound = StringNbtReader(StringReader(string)).parseCompound()
        return nbt.copyFrom(compound)
    }

}