package me.jfenn.bingo.api

import kotlinx.serialization.json.*
import net.minecraft.nbt.*
import net.minecraft.server.world.ServerWorld
import kotlin.reflect.KClass

interface IPersistentStateManager {

    companion object {
        val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = true
            prettyPrint = true
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

    fun <T: Any> getFromWorld(world: ServerWorld, id: String, clazz: KClass<T>, default: () -> T): T

}
