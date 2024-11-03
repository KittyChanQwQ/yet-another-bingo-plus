package me.jfenn.bingo.common.card.tag

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.decodeFromStream
import me.jfenn.bingo.common.MOD_ID_BINGO
import me.jfenn.bingo.common.utils.json
import net.fabricmc.fabric.api.resource.ResourceManagerHelper
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener
import net.minecraft.resource.ResourceManager
import net.minecraft.resource.ResourceType
import net.minecraft.util.Identifier
import org.slf4j.Logger

class TagManager(
    private val state: DataTagProvider,
    private val log: Logger,
) {

    private fun reloadTags(
        manager: ResourceManager
    ) {
        log.info("[TierListManager] Reloading BINGO objective tags...")

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
                    log.error("[TierListManager] Error reading tag file $id: ${e.message}")
                    null
                }
            }

            if (configs.isEmpty()) continue
            var config = TagData.EMPTY
            configs.forEach { config = it.combine(config) }

            newTags[name] = config
        }

        state.tags = newTags
    }

    init {
        ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(
            object : SimpleSynchronousResourceReloadListener {
                override fun reload(manager: ResourceManager) {
                    reloadTags(manager)
                }

                override fun getFabricId(): Identifier = Identifier.of(MOD_ID_BINGO, "tag_manager")!!
            }
        )
    }

    companion object {
        const val FILE_SUFFIX = ".json"
    }

}