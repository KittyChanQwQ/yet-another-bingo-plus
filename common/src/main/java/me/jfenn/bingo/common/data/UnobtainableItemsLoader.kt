package me.jfenn.bingo.common.data

import me.jfenn.bingo.api.item.IItemStack
import me.jfenn.bingo.common.card.CardGeneratorState
import me.jfenn.bingo.common.card.objective.BingoObjective
import me.jfenn.bingo.common.card.objective.BingoObjectiveManager
import net.minecraft.resource.ResourceFinder
import net.minecraft.resource.ResourceManager
import org.slf4j.Logger
import kotlin.random.Random

class UnobtainableItemsLoader(
    private val log: Logger,
    private val objectiveManager: BingoObjectiveManager,
) {
    private fun isReferencedInLootTables(
        item: IItemStack,
        manager: ResourceManager,
    ): Boolean {
        return ResourceFinder.json("loot_table")
            .findResources(manager)
            .any { (_, resource) ->
                resource.inputStream.use { stream ->
                    stream.bufferedReader()
                        .lines()
                        .anyMatch {
                            it.contains("\"${item.identifier}\"")
                        }
                }
            }
    }

    private fun isReferencedInCraftingRecipes(
        item: IItemStack,
        manager: ResourceManager,
    ): Boolean {
        return ResourceFinder.json("recipe")
            .findResources(manager)
            .any { (_, resource) ->
                resource.inputStream.use { stream ->
                    stream.bufferedReader()
                        .lines()
                        .anyMatch {
                            it.contains("\"${item.identifier}\"")
                        }
                }
            }
    }

    fun loadUnobtainableItems(
        tierLists: TierListType,
        tags: TagType,
        manager: ResourceManager,
    ): Set<String> {
        // Determine the set of unobtainable items from those that are uncategorized
        return objectiveManager.list()
            .asSequence()
            // ignore items that are manually categorized to a tier list or tag
            .filterNot { obj -> tierLists.values.any { it.isCategorized(obj) } }
            .filterNot { obj -> tags.values.any { it.contains(obj) } }
            // get an item instance from the id, if it exists
            .mapNotNull {
                objectiveManager.find(it, CardGeneratorState(Random.Default))
            }
            .filterIsInstance<BingoObjective.ItemEntry>()
            // itemStack should already be initialized if found
            .filterNot {
                isReferencedInLootTables(it.itemStack!!, manager) ||
                        isReferencedInCraftingRecipes(it.itemStack!!, manager)
            }
            .map { it.id }
            .toSet()
            .also {
                log.info("[UnobtainableItemsLoader] Excluding ${it.size} unobtainable items")
            }
    }
}