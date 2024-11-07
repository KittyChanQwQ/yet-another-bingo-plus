package me.jfenn.bingo.common.data

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.decodeFromStream
import me.jfenn.bingo.common.MOD_ID_BINGO
import me.jfenn.bingo.common.card.tag.TagData
import me.jfenn.bingo.common.utils.json
import net.minecraft.resource.ResourceManager
import org.slf4j.Logger

class TagLoader(
    private val log: Logger,
) {
    fun loadTags(
        manager: ResourceManager,
    ): Map<String, TagData> {
        log.info("[TagLoader] Reloading BINGO objective tags...")

        val newTags = mutableMapOf<String, TagData>()

        val lists = manager.findAllResources("$MOD_ID_BINGO/tags") { it.path.endsWith(FILE_SUFFIX) }
        for ((id, files) in lists) {
            val name = id.path.substringAfterLast('/').removeSuffix(FILE_SUFFIX)
            val configs = files.mapNotNull { file ->
                try {
                    file.inputStream.use {
                        @OptIn(ExperimentalSerializationApi::class)
                        json.decodeFromStream<TagData>(it)
                    }
                } catch (e: Throwable) {
                    log.error("[TagLoader] Error reading tag file $id: ${e.message}")
                    null
                }
            }

            if (configs.isEmpty()) continue
            var config = TagData.EMPTY
            configs.forEach { config = it.combine(config) }

            newTags[name] = config
        }

        return newTags
    }

    companion object {
        const val FILE_SUFFIX = ".json"
    }
}