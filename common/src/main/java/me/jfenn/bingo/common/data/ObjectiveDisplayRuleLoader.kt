package me.jfenn.bingo.common.data

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.decodeFromStream
import me.jfenn.bingo.common.MOD_ID_BINGO
import me.jfenn.bingo.common.card.objective.display.ObjectiveDisplayRule
import me.jfenn.bingo.common.utils.jsonStrict
import me.jfenn.bingo.platform.resource.IResourceManager
import org.slf4j.Logger

internal class ObjectiveDisplayRuleLoader(
    private val log: Logger,
) {
    fun loadObjectiveDisplayRules(
        manager: IResourceManager
    ): Map<String, ObjectiveDisplayRule> {
        log.info("[ObjectiveDisplayRuleLoader] Reloading BINGO objective rules...")

        val newObjectives = mutableMapOf<String, ObjectiveDisplayRule>()

        val startingPath = "$MOD_ID_BINGO/objective_display_rule"
        val lists = manager.findResources(startingPath) { it.path.endsWith(".json") }
        for ((id, file) in lists) {
            val objectiveId = id.namespace + ":" + id.path.removePrefix("$startingPath/").removeSuffix(".json")
            val objectiveData = try {
                file.inputStream.use {
                    @OptIn(ExperimentalSerializationApi::class)
                    jsonStrict.decodeFromStream<ObjectiveDisplayRule>(it)
                }
            } catch (e: Throwable) {
                log.error("[ObjectiveDisplayRuleLoader] Error reading display rule $id: ${e.message}")
                log.debug("Error reading display rule {}", id, e)
                continue
            }

            newObjectives[objectiveId] = objectiveData
        }

        return newObjectives
    }
}