package me.jfenn.bingo.common.state

import com.mojang.brigadier.StringReader
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import me.jfenn.bingo.api.IPersistentStateManager
import me.jfenn.bingo.common.MOD_ID
import me.jfenn.bingo.common.utils.json
import net.minecraft.nbt.*
import net.minecraft.server.world.ServerWorld
import net.minecraft.world.PersistentState
import org.koin.core.component.KoinComponent

@Serializable
sealed class SerializedPersistentState: PersistentState() {

    companion object : KoinComponent {
        fun nbtToJson(nbt: NbtElement?): JsonElement {
            if (nbt == null) return JsonNull
            return when (nbt) {
                is NbtString -> if (nbt.asString() == "null") JsonNull else JsonPrimitive(nbt.asString())
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
    }

    override fun isDirty(): Boolean {
        return true
    }

    override fun writeNbt(nbt: NbtCompound): NbtCompound {
        val string = json.encodeToString(this)
        val compound = StringNbtReader(StringReader(string)).parseCompound()
        return nbt.copyFrom(compound)
    }

}

class SerializedPersistentStateFactory(
    val persistentStateManager: IPersistentStateManager,
) {

    inline fun <reified T: SerializedPersistentState> fromWorld(world: ServerWorld): T {
        return persistentStateManager.getFromWorld(world, T::class, MOD_ID)
    }

}
