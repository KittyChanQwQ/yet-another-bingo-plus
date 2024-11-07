package me.jfenn.bingo.common.card.tag

import me.jfenn.bingo.common.data.ScopedData

class TierListTagProvider(
    private val data: ScopedData,
): ITagProvider {
    override fun listTags(): Map<String, TagData> {
        return data.tierLists
            .mapValues { (_, config) ->
                TagData(
                    values = (config.s + config.a + config.b + config.c + config.d + config.values)
                        .map { it.item }
                        .toSet()
                )
            }
    }
}