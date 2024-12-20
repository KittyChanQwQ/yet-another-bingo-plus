package me.jfenn.bingo.common.data

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.decodeFromStream
import me.jfenn.bingo.common.MOD_ID_BINGO
import me.jfenn.bingo.common.card.tierlist.TierListConfig
import me.jfenn.bingo.common.config.Config
import me.jfenn.bingo.common.utils.json
import net.minecraft.resource.ResourceManager
import org.slf4j.Logger
import java.io.File
import java.io.FileInputStream
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
            .resolve(MOD_ID_BINGO)
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

        val lists = manager.findAllResources("$MOD_ID_BINGO/tierlists") { it.path.endsWith(FILE_SUFFIX) }
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
            var config = TierListConfig.EMPTY
            configs.forEach { config = it.combine(config) }

            newTierLists[name] = config
        }

        for ((name, config) in readTierListFiles()) {
            newTierLists[name] = config
        }

        return newTierLists
    }

    companion object {
        val BUILTIN_TIER_LISTS = listOf("advancements", "challenge", "items", "simplified")
        const val FILE_SUFFIX = ".tierlist.json"
    }
}