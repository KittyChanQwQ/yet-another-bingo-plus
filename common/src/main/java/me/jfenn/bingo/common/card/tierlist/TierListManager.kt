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

        if (config.isFailure) {
            log.error("Unable to read tier list '${file.name}'", config.exceptionOrNull())
        }

        return config.getOrNull()
    }

    private fun readTierListFiles() : Map<String, TierListConfig> {
        return Config.configDir
            .resolve(MOD_ID_BINGO)
            .toFile()
            .listFiles()
            .orEmpty()
            .filter { it.name.endsWith(".tierlist.json") }
            .mapNotNull {
                val name = it.name.removeSuffix(".tierlist.json")
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

        val lists = manager.findAllResources("$MOD_ID_BINGO/tierlists") { it.path.endsWith(".tierlist.json") }
        for ((id, files) in lists) {
            val name = id.path.substringAfterLast('/').removeSuffix(".tierlist.json")
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
            configs.forEach { config = config.combine(it) }

            newTierLists[name] = config
        }

        for ((name, config) in readTierListFiles()) {
            val otherConfig = newTierLists[name] ?: TierListConfig.EMPTY
            newTierLists[name] = otherConfig.combine(config)
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

}