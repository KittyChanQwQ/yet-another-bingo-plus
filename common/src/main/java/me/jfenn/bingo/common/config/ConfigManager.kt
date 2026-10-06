package me.jfenn.bingo.common.config

import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer
import me.jfenn.bingo.common.MOD_ID_BINGO
import me.jfenn.bingo.common.utils.json
import me.jfenn.bingo.platform.IModEnvironment
import me.jfenn.bingo.platform.config.IConfigManager
import org.apache.commons.io.IOUtils
import org.slf4j.LoggerFactory
import java.io.FileInputStream
import java.io.InputStream
import java.io.OutputStream
import java.lang.reflect.Field
import java.nio.file.Files
import java.nio.file.NoSuchFileException
import java.nio.file.StandardOpenOption
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty
import kotlin.reflect.KType
import kotlin.reflect.jvm.jvmErasure
import kotlin.reflect.typeOf

class ConfigManager(
    environment: IModEnvironment,
) : IConfigManager {

    private val log = LoggerFactory.getLogger("bingo")
    override val configDir = environment.configDir

    private fun readText(path: String): String {
        val filePath = configDir.resolve(path)
        return Files.readString(filePath)
    }

    private fun writeText(path: String, text: String) {
        val filePath = configDir.resolve(path)
        if (!Files.exists(filePath)) {
            filePath.parent.toFile().mkdirs()
        }
        Files.writeString(
            filePath,
            text,
            StandardOpenOption.SYNC,
            StandardOpenOption.WRITE,
            StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING
        )
    }

    override fun <T: Any> read(type: KType, file: String): T {
        @Suppress("UNCHECKED_CAST")
        val serializer = serializer(type) as KSerializer<T>
        return try {
            json.decodeFromString(serializer, readText(file))
        } catch (e: NoSuchFileException) {
            null
        } catch (e: Exception) {
            log.error("Error reading $file", e)
            null
        } ?: run {
            json.decodeFromString(serializer, "{}").also { write(type, file, it) }
        }
    }

    override fun <T: Any> write(type: KType, file: String, config: T) {
        @Suppress("UNCHECKED_CAST")
        val serializer = serializer(type) as KSerializer<T>
        val jsonString = json.encodeToString(serializer, config)
        val jsonStringWithComments = injectDescriptions(type.jvmErasure.java, jsonString)
        writeText(file, jsonStringWithComments)
    }

    class ConfigDelegate<T: Any>(
        private val configManager: IConfigManager,
        private val type: KType,
        private val file: String,
    ) : ReadWriteProperty<Any, T> {
        var value: T? = null

        override fun getValue(thisRef: Any, property: KProperty<*>): T {
            return value ?: configManager.read<T>(type, file).also { value = it }
        }

        override fun setValue(thisRef: Any, property: KProperty<*>, value: T) {
            this.value = value.also { configManager.write(type, file, value) }
        }
    }
}

/**
 * Obtains a resource InputStream from the config directory, or
 * creates it from the java resources if it doesn't exist
 */
fun IConfigManager.readStream(
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

fun IConfigManager.writeStream(
    file: String,
): OutputStream {
    val filePath = configDir.resolve("$MOD_ID_BINGO/$file")
    return Files.newOutputStream(
        filePath,
        StandardOpenOption.SYNC,
        StandardOpenOption.WRITE,
        StandardOpenOption.CREATE,
        StandardOpenOption.TRUNCATE_EXISTING
    )
}

inline fun <reified T: Any> IConfigManager.config(file: String) =
    ConfigManager.ConfigDelegate<T>(this, typeOf<T>(), file)

internal data class FieldInfo(
    val field: Field,
    val description: String,
    val indent: String,
    val jsonProperty: String,
)

private fun injectDescriptions(
    javaClass: Class<*>,
    config: String,
): String = injectDescriptionsBase(
    javaClass = javaClass,
    config = config,
    replace = { (_, description, indent, jsonProperty) ->
        val jsonComment = description.trim().lines().joinToString("\n") { "${indent}// ${it.trim()}" }
        "\n${jsonComment}\n${indent}${jsonProperty}"
    }
)

internal fun injectDescriptionsBase(
    javaClass: Class<*>,
    config: String,
    replace: (FieldInfo) -> String,
    visited: MutableSet<Class<*>> = mutableSetOf(),
): String {
    visited.add(javaClass)

    var ret = config

    ret = ret.replace(Regex("\\[([\\s\\d,]+)]")) { matchResult ->
        "[" + matchResult.groupValues[1].split(",").joinToString(", ") { it.trim() } + "]"
    }

    for (field in javaClass.declaredFields) {
        val description = field.getAnnotation(Description::class.java)
        if (description != null) {
            val regex = Regex("^(\\s+)(\"${Regex.escape(field.name)}\":)", RegexOption.MULTILINE)
            ret = ret.replace(regex) { match ->
                val indent = match.groupValues[1]
                val jsonProperty = match.groupValues[2]
                replace(FieldInfo(field, description.description, indent, jsonProperty))
            }
        }
        if (field.type.name.startsWith("me.jfenn.bingo") && !visited.contains(field.type)) {
            ret = injectDescriptionsBase(field.type, ret, replace, visited)
        }
    }
    return ret
}

internal inline fun <reified T> createDocumentationConfig(config: T): String {
    return injectDescriptionsBase(
        javaClass = T::class.java,
        config = json.encodeToString(config),
        replace = { (field, description, indent, jsonProperty) ->
            val jsonComment = description.trim().lines().joinToString("\n") { "${indent}// [!callout /${field.name}/] ${it.trim()}" }
            "\n${jsonComment}\n${indent}${jsonProperty}"
        }
    )
}
