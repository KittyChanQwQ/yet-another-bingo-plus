package me.jfenn.bingo.common.card.tag

import me.jfenn.bingo.common.data.ScopedData

class DataTagProvider(
    private val data: ScopedData,
): ITagProvider {
    override fun listTags(): Map<String, TagData> = data.tags
}