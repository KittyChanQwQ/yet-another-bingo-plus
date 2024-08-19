package me.jfenn.bingo.common.card.tierlist

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.decodeFromStream
import me.jfenn.bingo.common.MOD_ID_BINGO
import me.jfenn.bingo.common.config.Config
import me.jfenn.bingo.common.utils.json
import net.fabricmc.fabric.api.resource.ResourceManagerHelper
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener
import net.minecraft.resource.ResourceManager
import net.minecraft.resource.ResourceType
import net.minecraft.util.Identifier
import org.slf4j.Logger
import java.io.File
import java.io.FileInputStream
import java.math.BigInteger
import java.nio.file.Files
import java.security.MessageDigest

class TierListManager(
    private val tierLists: TierListState,
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
                    javaClass.getResourceAsStream("/$MOD_ID_BINGO/${file.name}")!!
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

    private fun reloadTierLists(
        manager: ResourceManager
    ) {
        log.info("[TierListManager] Reloading BINGO tier lists...")

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
                    log.error("[TierListManager] Error reading tier list $id: ${e.message}")
                    null
                }
            }

            if (configs.isEmpty()) continue
            var config = TierListConfig.EMPTY
            configs.forEach { config = it.combine(config) }

            newTierLists[name] = config
        }

        for ((name, config) in readTierListFiles()) {
            val otherConfig = newTierLists[name]
            newTierLists[name] = otherConfig?.combine(config) ?: config
        }

        tierLists.tierLists = newTierLists
    }

    init {
        ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(
            object : SimpleSynchronousResourceReloadListener {
                override fun reload(manager: ResourceManager) {
                    reloadTierLists(manager)
                }

                override fun getFabricId(): Identifier = Identifier.of(MOD_ID_BINGO, "tier_list_manager")!!
            }
        )
    }

    companion object {
        val BUILTIN_TIER_LISTS = listOf("advancements", "all_items", "challenge", "simplified")
        const val FILE_SUFFIX = ".tierlist.json"
    }

}