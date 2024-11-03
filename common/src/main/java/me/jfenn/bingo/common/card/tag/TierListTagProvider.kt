package me.jfenn.bingo.common.card.tag

import me.jfenn.bingo.common.card.tierlist.TierListState

class TierListTagProvider(
    private val tierLists: TierListState,
): ITagProvider {
    override fun listTags(): Map<String, TagData> {
        return tierLists.tierLists
            .mapValues { (_, config) ->
                TagData(
                    values = (config.s + config.a + config.b + config.c + config.d + config.values)
                        .map { it.item }
                        .toSet()
                )
            }
    }
}