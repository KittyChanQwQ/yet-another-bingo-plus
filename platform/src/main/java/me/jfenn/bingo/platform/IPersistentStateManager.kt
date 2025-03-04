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
            explicitNulls = false
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

    fun <T: Any> register(id: String, kType: KType, default: () -> T): IPersistentStateType<T>

    fun <T: Any> getFromWorld(world: ServerWorld, type: IPersistentStateType<T>): T

    fun <T: Any> put(world: ServerWorld, type: IPersistentStateType<T>, value: T)

}

interface IPersistentStateType<T>

inline fun <reified T: Any> IPersistentStateManager.register(id: String, noinline default: () -> T): IPersistentStateType<T> =
    register(id, typeOf<T>(), default)
