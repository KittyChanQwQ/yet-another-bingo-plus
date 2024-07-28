package me.jfenn.bingo.common.card.data

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

class ObjectiveRegistryManager(
    private val state: ObjectiveRegistryState,
    private val log: Logger,
) {

    private fun reloadObjectives(
        manager: ResourceManager
    ) {
        log.info("[ObjectiveRegistryManager] Reloading BINGO objectives...")

        val newObjectives = mutableMapOf<String, ObjectiveData>()

        val startingPath = "$MOD_ID_BINGO/objectives"
        val lists = manager.findResources(startingPath) { it.path.endsWith(".json") }
        for ((id, file) in lists) {
            val objectiveId = id.namespace + ":" + id.path.removePrefix("$startingPath/").removeSuffix(".json")
            val objectiveData = try {
                file.inputStream.use {
                    @OptIn(ExperimentalSerializationApi::class)
                    json.decodeFromStream<ObjectiveData>(it)
                }
            } catch (e: Throwable) {
                log.error("[ObjectiveRegistryManager] Error reading objective $id: ${e.message}")
                continue
            }

            newObjectives[objectiveId] = objectiveData
        }

        state.objectives = newObjectives
    }

    init {
        ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(
            object : SimpleSynchronousResourceReloadListener {
                override fun reload(manager: ResourceManager) {
                    reloadObjectives(manager)
                }

                override fun getFabricId(): Identifier = Identifier.of(MOD_ID_BINGO, "objective_registry_manager")!!
            }
        )
    }

}