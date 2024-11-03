package me.jfenn.bingo.common.card.tag

class TagService(
    private val providers: List<ITagProvider>
) {
    fun getTags() = providers
        .flatMap { it.listTags().toList() }
        .toMap()
}