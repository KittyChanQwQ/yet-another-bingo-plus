package me.jfenn.bingo.common.text

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.decodeFromStream
import me.jfenn.bingo.common.MOD_ID_BINGO
import me.jfenn.bingo.common.config.TrackedFileService
import me.jfenn.bingo.common.data.ScopedData
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.utils.jsonStrict
import me.jfenn.bingo.platform.IJsonSerializers
import me.jfenn.bingo.platform.IModEnvironment
import me.jfenn.bingo.platform.resource.IResourceManager
import me.jfenn.bingo.platform.text.IText
import me.jfenn.bingo.platform.text.ITextSerialized
import org.slf4j.Logger

internal class MessageService(
    private val placeholderService: PlaceholderService,
    private val serializers: IJsonSerializers,
    private val scopedData: ScopedData,
    private val state: BingoState,
) {
    object MessageType {
        const val GAME_START = "game_start"
        const val GAME_END = "game_end"
        const val SCOREBOARD = "scoreboard"
    }

    internal class Loader(
        private val trackedFileService: TrackedFileService,
        private val environment: IModEnvironment,
        private val serializers: IJsonSerializers,
        private val log: Logger,
    ) {
        private fun readMessagesFiles() : List<String> {
            return environment.configDir
                .resolve(MESSAGES_PATH)
                .toFile()
                .listFiles()
                .orEmpty()
                .filter { it.name.endsWith(FILE_SUFFIX) }
                .map { it.name.removeSuffix(FILE_SUFFIX) }
        }

        private fun configFile(name: String) =
            environment.configDir.resolve("$MESSAGES_PATH/$name${FILE_SUFFIX}")

        fun loadMessages(manager: IResourceManager): Map<String, MessageData> {
            log.info("[MessageService.Loader] Reloading BINGO message formats...")

            val newMessages = mutableMapOf<String, MessageData>()

            val lists = manager.findAllResources(MESSAGES_PATH) { it.path.endsWith(FILE_SUFFIX) }
            for ((id, files) in lists) {
                val name = id.path.substringAfterLast('/').removeSuffix(FILE_SUFFIX)
                val configs = files.mapNotNull { file ->
                    try {
                        file.inputStream.use {
                            @OptIn(ExperimentalSerializationApi::class)
                            jsonStrict.decodeFromStream<MessageData>(it)
                        }
                    } catch (e: Throwable) {
                        log.error("[TagLoader] Error reading tag file $id: ${e.message}")
                        null
                    }
                }

                newMessages[name] = configs.firstOrNull() ?: continue
            }

            for (name in (readMessagesFiles() + newMessages.keys).distinct()) {
                val resource = newMessages[name]
                val (_, finalConfig) = trackedFileService.readFileOrResource(
                    path = configFile(name),
                    resource = resource,
                )

                if (finalConfig == null) continue
                newMessages[name] = finalConfig
            }

            // validation check to ensure encoded text can be parsed
            return newMessages.mapValues { (name, result) ->
                try {
                    result.lines.forEach { serializers.json.decodeFromJsonElement<ITextSerialized>(it) }
                    result
                } catch (e: Throwable) {
                    log.error("[messages/${name}.json] Could not parse text:", e)
                    MessageData(emptyList())
                }
            }
        }

        companion object {
            private const val MESSAGES_PATH = "$MOD_ID_BINGO/messages"
            private const val FILE_SUFFIX = ".json"
        }
    }

    fun getLines(
        type: String,
        replacements: Map<String, List<IText>>,
    ): List<IText> = getWrappedLines(
        type = type,
        replacements = replacements,
        wrap = { it }
    )

    fun <T> getWrappedLines(
        type: String,
        replacements: Map<String, List<T>>,
        wrap: (IText) -> T,
    ): List<T> {
        val typeWithState = "${type}_${state.state.identifier}"
        val message = scopedData.messages[typeWithState]
            ?: scopedData.messages[type]
            ?: MessageData(emptyList())

        val lines = message.lines
            .flatMap { line ->
                line
                    .takeIf { it is JsonPrimitive && it.isString }
                    ?.let { replacements[(it as? JsonPrimitive)?.content] }
                    ?: listOf(
                        wrap(
                            placeholderService.parseText(serializers.json.decodeFromJsonElement<ITextSerialized>(line))
                        )
                    )
            }

        return lines
    }
}