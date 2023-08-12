package me.jfenn.bingo.card

import me.jfenn.bingo.config.Config

class GeneratorTier(
    val items: List<Set<String>> = listOf(),
) {

    init {
        // ensure that each item has a texture available
        for (itemRow in items) for (item in itemRow) {
            assert(BingoCardEntry(item).image != null) { "No item texture is available for '$item.png'" }
        }
    }

    fun pick(count: Int): List<String> {
        return items.shuffled()
            .take(count)
            .map { it.random() }
    }

    companion object {
        fun fromFilename(name: String): GeneratorTier {
            val inputStream = Config.readStream(name)

            val items = mutableListOf<Set<String>>()
            inputStream.bufferedReader().useLines { lines ->
                for (line in lines) {
                    items.add(
                        line.split("|")
                            .map { it.uppercase().trim() }
                            .toSet()
                    )
                }
            }

            return GeneratorTier(items)
        }
    }
}