package me.jfenn.bingo.common.data

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.decodeFromStream
import me.jfenn.bingo.common.MOD_ID_BINGO
import me.jfenn.bingo.common.card.tierlist.TierListConfig
import me.jfenn.bingo.common.config.Config
import me.jfenn.bingo.common.utils.json
import net.minecraft.resource.ResourceManager
import org.slf4j.Logger
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.math.BigInteger
import java.nio.file.Files
import java.security.MessageDigest

class TierListLoader(
    private val log: Logger,
) {
    private fun readTierList(file: File): TierListConfig? {
        val config = runCatching {
            FileInputStream(file).use {
                @OptIn(ExperimentalSerializationApi::class)
                json.decodeFromStream<TierListConfig>(it)
            }
        }

        val configVal = config.getOrNull()
        if (configVal == null) {
            log.error("Unable to read tier list '${file.name}'", config.exceptionOrNull())
            return null
        }

        if (BUILTIN_TIER_LISTS.any { file.name == it + FILE_SUFFIX }) {
            val currentHash = MessageDigest.getInstance("MD5")
                .digest(Files.readAllBytes(file.toPath()))
                .let { BigInteger(1, it).toString(16) }

            val originalHash = MessageDigest.getInstance("MD5")
                .digest(
                    javaClass.getResourceAsStream("/data/$MOD_ID_BINGO/$MOD_ID_BINGO/tierlists/${file.name}")!!
                        .readAllBytes()
                )
                .let { BigInteger(1, it).toString(16) }

            // If the tier list in the config file doesn't match the new config, validate it!
            if (currentHash == originalHash) {
                configVal.shouldValidate = false
            } else {
                log.info("Tier list ${file.name} has been modified ($currentHash != $originalHash) - enabling validation.")
            }
        }

        return configVal
    }

    private fun readTierListFiles() : Map<String, TierListConfig> {
        return Config.configDir
            .resolve(TIERLISTS_PATH)
            .toFile()
            .listFiles()
            .orEmpty()
            .filter { it.name.endsWith(FILE_SUFFIX) }
            .mapNotNull {
                val name = it.name.removeSuffix(FILE_SUFFIX)
                val config = readTierList(it) ?: return@mapNotNull null
                name to config
            }
            .toMap()
    }

    fun loadTierLists(
        manager: ResourceManager
    ): Map<String, TierListConfig> {
        log.info("[TierListLoader] Reloading BINGO tier lists...")

        val newTierLists = mutableMapOf<String, TierListConfig>()

        val lists = manager.findAllResources(TIERLISTS_PATH) { it.path.endsWith(FILE_SUFFIX) }
        for ((id, files) in lists) {
            val name = id.path.substringAfterLast('/').removeSuffix(FILE_SUFFIX)

            val configs = files.mapNotNull { file ->
                try {
                    file.inputStream.use {
                        @OptIn(ExperimentalSerializationApi::class)
                        json.decodeFromStream<TierListConfig>(it)
                    }
                } catch (e: Throwable) {
                    log.error("[TierListLoader] Error reading tier list $id: ${e.message}")
                    null
                }
            }

            if (configs.isEmpty()) continue
            val config = configs.reduce { acc, list -> list.combine(acc) }

            if (id.namespace == MOD_ID_BINGO && name in BUILTIN_TIER_LISTS) {
                // Default tier lists are written to the config dir as files
                // and should only be read from readTierListFiles() to avoid duplication
                if (!Files.exists(configFile(name))) {
                    writeTierList(name, config)
                }
                continue
            }

            newTierLists[name] = (newTierLists[name] ?: TierListConfig.EMPTY).combine(config)
        }

        for ((name, config) in readTierListFiles()) {
            // Files cannot completely overwrite the resource lists, otherwise this prevents
            // datapacks from modifying them...
            // However, the builtin lists should never be pulled from resources (due to the check above)
            val otherConfig = newTierLists[name]
            newTierLists[name] = otherConfig?.combine(config) ?: config
        }

        return newTierLists
    }

    private fun configFile(tierListName: String) =
        Config.configDir.resolve("$TIERLISTS_PATH/$tierListName$FILE_SUFFIX")

    fun writeTierList(name: String, tierList: TierListConfig) {
        val filePath = configFile(name)

        if (tierList.isEmpty()) {
            filePath.toFile().delete()
            return
        }

        try {
            filePath.parent.toFile().mkdirs()
            filePath.toFile().writeText(json.encodeToString(tierList))
        } catch (e: IOException) {
            log.error("Error writing to $filePath:", e)
        }
    }

    companion object {
        val BUILTIN_TIER_LISTS = arrayOf("advancements", "challenge", "items", "simplified")
        const val FILE_SUFFIX = ".tierlist.json"
        const val TIERLISTS_PATH = "$MOD_ID_BINGO/tierlists"
    }
}