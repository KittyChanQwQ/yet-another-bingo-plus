package me.jfenn.bingo.common.data

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.decodeFromStream
import me.jfenn.bingo.common.MOD_ID_BINGO
import me.jfenn.bingo.common.card.tag.TagData
import me.jfenn.bingo.common.config.Config
import me.jfenn.bingo.common.utils.json
import net.minecraft.resource.ResourceManager
import org.slf4j.Logger
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.nio.file.Files

class TagLoader(
    private val log: Logger,
) {
    private fun readTagsFile(file: File): TagData? {
        val tagData = runCatching {
            FileInputStream(file).use {
                @OptIn(ExperimentalSerializationApi::class)
                json.decodeFromStream<TagData>(it)
            }
        }

        val tagDataVal = tagData.getOrNull()
        if (tagDataVal == null) {
            log.error("Unable to read tag '${file.name}'", tagData.exceptionOrNull())
            return null
        }

        return tagDataVal
    }

    private fun readTagsFiles() : Map<String, TagData> {
        return Config.configDir
            .resolve(TAGS_PATH)
            .toFile()
            .listFiles()
            .orEmpty()
            .filter { it.name.endsWith(FILE_SUFFIX) }
            .mapNotNull {
                val name = it.name.removeSuffix(FILE_SUFFIX)
                val tagData = readTagsFile(it) ?: return@mapNotNull null
                name to tagData
            }
            .toMap()
    }

    fun loadTags(
        manager: ResourceManager,
    ): Map<String, TagData> {
        log.info("[TagLoader] Reloading BINGO objective tags...")

        val newTags = mutableMapOf<String, TagData>()

        val lists = manager.findAllResources(TAGS_PATH) { it.path.endsWith(FILE_SUFFIX) }
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
            val config = configs.reduce { acc, tag -> tag.plus(acc) }

            if (id.namespace == MOD_ID_BINGO && name in BUILTIN_TAGS) {
                // Default tags are written to the config dir as files
                // and should only be read from readTierListFiles() to avoid duplication
                if (!Files.exists(configFile(name))) {
                    writeTag(name, config)
                }
                continue
            }

            newTags[name] = (newTags[name] ?: TagData.EMPTY).plus(config)
        }

        for ((name, config) in readTagsFiles()) {
            // Files cannot completely overwrite the resource lists, otherwise this prevents
            // datapacks from modifying them...
            // However, the builtin lists should never be pulled from resources (due to the check above)
            val otherConfig = newTags[name]
            newTags[name] = otherConfig?.plus(config) ?: config
        }

        return newTags
    }

    private fun configFile(tagName: String) =
        Config.configDir.resolve("$TAGS_PATH/$tagName${FILE_SUFFIX}")

    fun writeTag(name: String, tag: TagData) {
        val filePath = configFile(name)

        if (tag.isEmpty()) {
            filePath.toFile().delete()
            return
        }

        try {
            filePath.parent.toFile().mkdirs()
            filePath.toFile().writeText(json.encodeToString(tag))
        } catch (e: IOException) {
            log.error("Error writing to $filePath:", e)
        }
    }

    companion object {
        val BUILTIN_TAGS = arrayOf("biome_specific", "in_overworld", "in_the_end", "in_the_nether", "tedious", "unobtainable")
        const val FILE_SUFFIX = ".json"
        const val TAGS_PATH = "$MOD_ID_BINGO/tags"
    }
}