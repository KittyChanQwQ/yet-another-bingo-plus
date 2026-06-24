package me.jfenn.bingo.common.state

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import kotlinx.serialization.serializer
import me.jfenn.bingo.platform.IJsonSerializers
import me.jfenn.bingo.platform.IServerWorldFactory
import me.jfenn.bingo.platform.event.IEventBus
import me.jfenn.bingo.platform.event.model.ServerEvent
import org.slf4j.Logger
import java.nio.file.Files
import java.nio.file.OpenOption
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import kotlin.reflect.KType
import kotlin.reflect.typeOf

@OptIn(ExperimentalSerializationApi::class)
internal class PersistentStateManager(
    val logger: Logger,
    serializers: IJsonSerializers,
    val serverWorldFactory: IServerWorldFactory,
    eventBus: IEventBus,
) {

    private val json = Json(serializers.json) {
        prettyPrint = true
    }
    private val registeredTypes = mutableMapOf<String, Type<*>>()
    private val writeOptions = arrayOf<OpenOption>(
        StandardOpenOption.SYNC,
        StandardOpenOption.WRITE,
        StandardOpenOption.CREATE,
        StandardOpenOption.TRUNCATE_EXISTING
    )

    inline fun <reified T: Any> register(id: String, noinline default: () -> T) = register(id, typeOf<T>(), default)

    fun <T : Any> register(
        id: String,
        kType: KType,
        default: () -> T
    ): Type<T> {
        val path = serverWorldFactory.serverDataDir.resolve("$id.json.gz")

        val type = Type(
            id,
            path,
            @Suppress("UNCHECKED_CAST") (serializer(kType) as KSerializer<T>),
            default()
        )
        registeredTypes[type.id] = type
        return type
    }

    fun <T : Any> read(
        type: Type<T>
    ): T {
        val path = type.path
        if (Files.exists(path)) {
            try {
                logger.debug("Reading persistent state ${type.id}...")
                type.value = Files.newInputStream(path)
                    .buffered()
                    .let { GZIPInputStream(it) }
                    .use {
                        json.decodeFromStream(type.serializer, it)
                    }
            } catch (e: Throwable) {
                logger.error("Error decoding persistent state ${type.id}", e)
            }
        }

        return type.value
    }

    fun <T : Any> write(
        type: Type<T>,
        value: T
    ) {
        val path = type.path

        try {
            logger.debug("Writing persistent state ${type.id}...")
            Files.newOutputStream(path, *writeOptions)
                .buffered()
                .let { GZIPOutputStream(it) }
                .use {
                    json.encodeToStream(type.serializer, value, it)
                }
        } catch (e: Throwable) {
            logger.error("Error encoding persistent state ${type.id}", e)
        }

        type.value = value
        logger.debug("Writing persistent state ${type.id}... Done!")
    }

    fun writeAll() {
        registeredTypes.values.forEach {
            this.write(@Suppress("UNCHECKED_CAST") (it as Type<Any>), it.value)
        }
    }

    class Type<T: Any>(
        val id: String,
        val path: Path,
        val serializer: KSerializer<T>,
        var value: T,
    )

    init {
        eventBus.register(ServerEvent.Saved) {
            writeAll()
        }
    }
}