package me.jfenn.bingo.common.text

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromStream
import me.jfenn.bingo.common.MOD_ID_BINGO
import me.jfenn.bingo.common.config.Config
import me.jfenn.bingo.common.config.TrackedFileService
import me.jfenn.bingo.common.utils.json
import net.minecraft.resource.ResourceManager
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import org.slf4j.Logger

class MessageService(
    private val trackedFileService: TrackedFileService,
    private val placeholderService: PlaceholderService,
    private val text: TextProvider,
    private val log: Logger,
) {

    enum class MessageType {
        GAME_START,
        GAME_END,
        SCOREBOARD,
    }

    private var messages = emptyMap<MessageType, MessageData>()

    private fun filePath(type: MessageType) = "$MOD_ID_BINGO/messages/${type.name.lowercase()}.json"

    private fun loadMessage(
        manager: ResourceManager,
        type: MessageType
    ): MessageData {
        val pathStr = filePath(type)
        val path = Config.configDir.resolve(pathStr)
        val resource = manager.getResourceOrThrow(Identifier.of(MOD_ID_BINGO, pathStr))
            .inputStream
            .use {
                @OptIn(ExperimentalSerializationApi::class)
                json.decodeFromStream<MessageData>(it)
            }

        val result = trackedFileService.readFileOrResource(
            path = path,
            resource = resource,
        ).config ?: MessageData(emptyList())

        try {
            result.lines.forEach { text.deserialized(it) }
        } catch (e: Throwable) {
            log.error("[messages/${type.name.lowercase()}.json] Could not parse text:", e)
            return MessageData(emptyList())
        }

        return result
    }

    fun loadMessages(manager: ResourceManager) {
        messages = MessageType.entries.associateWith { loadMessage(manager, it) }
    }

    fun getLines(
        type: MessageType,
        replacements: Map<String, List<Text>>
    ): List<Text> {
        val message = messages[type] ?: MessageData(emptyList())

        val lines = message.lines
            .flatMap { line ->
                line
                    .takeIf { it is JsonPrimitive && it.isString }
                    ?.let { replacements[(it as? JsonPrimitive)?.content] }
                    ?: listOf(text.deserialized(line))
            }

        return lines.map { line ->
            placeholderService.parseText(line)
        }
    }

}