package me.jfenn.bingo.common.card.tag

class DataTagProvider: ITagProvider {
    var tags: Map<String, TagData> = emptyMap()

    override fun listTags(): Map<String, TagData> = tags
}