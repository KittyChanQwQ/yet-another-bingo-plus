package me.jfenn.bingo.common.config

import kotlinx.datetime.Instant
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import kotlinx.serialization.serializer
import me.jfenn.bingo.common.MOD_ID_BINGO
import me.jfenn.bingo.common.utils.json
import me.jfenn.bingo.platform.config.IConfigManager
import net.fabricmc.loader.api.FabricLoader
import org.apache.commons.io.IOUtils
import org.slf4j.LoggerFactory
import java.io.FileInputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

object Config : IConfigManager {

    private val log = LoggerFactory.getLogger("bingo")
    override val configDir get() = try {
        FabricLoader.getInstance().configDir
    } catch (e: Exception) {
        Path.of("").toAbsolutePath()
    }!!

    override fun inputStream(path: String): InputStream {
        val filePath = configDir.resolve(path)
        return Files.newInputStream(filePath)
    }

    override fun outputStream(path: String): OutputStream {
        val filePath = configDir.resolve(path)
        if (!Files.exists(filePath)) {
            filePath.parent.toFile().mkdirs()
        }
        return Files.newOutputStream(filePath)
    }

    @OptIn(InternalSerializationApi::class, ExperimentalSerializationApi::class)
    fun <T: Any> read(clazz: KClass<T>, file: String): T {
        return try {
            inputStream(file).use { json.decodeFromStream(clazz.serializer(), it) }
        } catch (e: NoSuchFileException) {
            null
        } catch (e: Exception) {
            log.error("Error reading $file", e)
            null
        } ?: run {
            json.decodeFromString(clazz.serializer(), "{}").also { write(clazz, file, it) }
        }
    }

    @OptIn(InternalSerializationApi::class, ExperimentalSerializationApi::class)
    fun <T: Any> write(clazz: KClass<T>, file: String, config: T) {
        outputStream(file).use {
            json.encodeToStream(clazz.serializer(), config, it)
        }
    }

    /**
     * Returns the last modified timestamp if the file exists on disk
     * Otherwise, returns DISTANT_PAST
     */
    fun readLastModified(fileName: String): Instant {
        val file = configDir.resolve("$MOD_ID_BINGO/$fileName").toFile()
        return if (file.exists()) {
            Instant.fromEpochMilliseconds(file.lastModified())
        } else {
            Instant.DISTANT_PAST
        }
    }

    /**
     * Obtains a resource InputStream from the config directory, or
     * creates it from the java resources if it doesn't exist
     */
    fun readStream(
        file: String,
        shouldWriteDefault: Boolean = true,
        default: () -> InputStream = {
            javaClass.getResourceAsStream("/$MOD_ID_BINGO/$file")!!
        },
    ): InputStream {
        val filePath = configDir.resolve("$MOD_ID_BINGO/$file")
        if (!Files.exists(filePath)) {
            if (!shouldWriteDefault)
                return default()

            filePath.parent.toFile().mkdirs()

            // write the default resource file to the config dir
            Files.newOutputStream(filePath, StandardOpenOption.CREATE).use { output ->
                default().use { input -> IOUtils.copy(input, output) }
            }
        }

        // return the config dir file content
        return FileInputStream(filePath.toFile())
    }
}

class ConfigDelegate<T: Any>(
    private val clazz: KClass<T>,
    private val file: String,
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
