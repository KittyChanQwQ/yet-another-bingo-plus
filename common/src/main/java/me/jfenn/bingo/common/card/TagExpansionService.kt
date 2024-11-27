package me.jfenn.bingo.common.card

import me.jfenn.bingo.platform.ITagAccessor
import org.slf4j.Logger

class TagExpansionService(
    private val log: Logger,
    private val tagAccessor: ITagAccessor,
) {

    fun expandItemTag(item: String) : Set<String> {
        return if (item.startsWith("#")) {
            tagAccessor.getItemTag(item.substring(1))
                ?.toSet()
                ?: run {
                    log.error("[TagExpansionService] Could not find item tag '$item'")
                    emptySet()
                }
        } else {
            setOf(item)
        }
    }

    fun expandItemTags(items: Iterable<String>) : Set<String> {
        return items
            .flatMap { expandItemTag(it) }
            .toSet()
    }

}