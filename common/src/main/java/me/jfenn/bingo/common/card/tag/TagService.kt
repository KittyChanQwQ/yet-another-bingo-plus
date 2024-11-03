package me.jfenn.bingo.common.card.tag

import me.jfenn.bingo.common.utils.cacheFor
import kotlin.time.Duration.Companion.seconds

class TagService(
    private val providers: List<ITagProvider>
) {

    private val tagsCache = cacheFor(5.seconds) { _: Unit ->
        providers
            .flatMap { it.listTags().toList() }
            .toMap()
    }

    fun getTags() = tagsCache.get(Unit)
}