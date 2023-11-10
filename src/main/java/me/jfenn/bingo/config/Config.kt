package me.jfenn.bingo.config

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.serializer
import me.jfenn.bingo.utils.json
import net.fabricmc.loader.api.FabricLoader
import org.apache.commons.io.IOUtils
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

object Config {

    val configDir get() = try {
        FabricLoader.getInstance().configDir
    } catch (e: Exception) {
        Path.of("").toAbsolutePath()
    }

    @OptIn(InternalSerializationApi::class)
    fun <T: Any> read(clazz: KClass<T>, file: String): T {
        val filePath = configDir.resolve(file)
        return try {
            Files.newInputStream(filePath).use { json.decodeFromStream(clazz.serializer(), it) }
        } catch (e: Exception) {
            json.decodeFromString(clazz.serializer(), "{}").also { write(clazz, file, it) }
        }
    }

    @OptIn(InternalSerializationApi::class)
    fun <T: Any> write(clazz: KClass<T>, file: String, config: T) {
        val filePath = configDir.resolve(file)
        if (!Files.exists(filePath)) {
            filePath.parent.toFile().mkdirs()
            Files.createFile(filePath)
        }
        Files.write(filePath, json.encodeToString(clazz.serializer(), config).toByteArray())
    }

    /**
     * Obtains a resource InputStream from the config directory, or
     * creates it from the java resources if it doesn't exist
     */
    fun readStream(
        file: String,
        shouldWriteDefault: Boolean = true,
        default: () -> InputStream = {
            javaClass.getResourceAsStream("/bingo/$file")!!
        },
    ): InputStream {
        val filePath = configDir.resolve("bingo/$file")
        if (!Files.exists(filePath)) {
            if (!shouldWriteDefault)
                return default()

            filePath.parent.toFile().mkdirs()
            Files.createFile(filePath)

            // write the default resource file to the config dir
            FileOutputStream(filePath.toFile()).use { output ->
                default().use { input -> IOUtils.copy(input, output) }
            }
        }

        // return the config dir file content
        return FileInputStream(filePath.toFile())
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
