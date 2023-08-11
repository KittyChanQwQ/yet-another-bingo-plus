package me.jfenn.bingo.config

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.serializer
import me.jfenn.bingo.utils.json
import net.fabricmc.loader.api.FabricLoader
import java.nio.file.Files
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

object Config {
    @OptIn(InternalSerializationApi::class)
    fun <T: Any> read(clazz: KClass<T>, file: String): T {
        val filePath = FabricLoader.getInstance().configDir.resolve(file)
        return try {
            json.decodeFromStream(clazz.serializer(), Files.newInputStream(filePath))
        } catch (e: Exception) {
            e.printStackTrace()
            json.decodeFromString(clazz.serializer(), "{}").also { write(clazz, file, it) }
        }
    }

    @OptIn(InternalSerializationApi::class)
    fun <T: Any> write(clazz: KClass<T>, file: String, config: T) {
        val filePath = FabricLoader.getInstance().configDir.resolve(file)
        if (!Files.exists(filePath)) Files.createFile(filePath)
        Files.write(filePath, json.encodeToString(clazz.serializer(), config).toByteArray())
    }
}

class ConfigDelegate<T: Any>(
    val clazz: KClass<T>,
    val file: String,
) : ReadWriteProperty<Any, T> {
    var value: T? = null

    override fun getValue(thisRef: Any, property: KProperty<*>): T {
        return value ?: Config.read(clazz, file).also { value = it }
    }

    override fun setValue(thisRef: Any, property: KProperty<*>, value: T) {
        this.value = value.also { Config.write(clazz, file, value) }
    }
}

inline fun <reified T: Any> config(file: String) = ConfigDelegate(T::class, file)
