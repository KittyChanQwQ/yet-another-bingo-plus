package me.jfenn.bingo.platform

import kotlinx.serialization.json.*
import net.minecraft.nbt.*
import net.minecraft.server.world.ServerWorld
import kotlin.reflect.KType
import kotlin.reflect.typeOf

interface IPersistentStateManager {

    companion object {
        val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = true
            prettyPrint = false
        }

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

    fun <T: Any> getFromWorld(world: ServerWorld, id: String, kType: KType, default: () -> T): T

    fun <T: Any> put(world: ServerWorld, id: String, kType: KType, value: T)

}

inline fun <reified T: Any> IPersistentStateManager.getFromWorld(world: ServerWorld, id: String, noinline default: () -> T): T =
    getFromWorld(world, id, typeOf<T>(), default)

inline fun <reified T: Any> IPersistentStateManager.put(world: ServerWorld, id: String, value: T) =
    put(world, id, typeOf<T>(), value)
