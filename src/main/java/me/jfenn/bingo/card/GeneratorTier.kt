package me.jfenn.bingo.card

import me.jfenn.bingo.config.Config

class GeneratorTier(
    val items: List<Set<String>> = listOf(),
) {

    fun pick(count: Int): List<String> {
        return items.shuffled()
            .take(count)
            .map { it.random() }
    }

    companion object {
        fun fromFilename(name: String): GeneratorTier {
            val inputStream = Config.getResource(name)

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