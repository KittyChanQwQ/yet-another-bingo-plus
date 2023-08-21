package me.jfenn.bingo.card

import me.jfenn.bingo.config.Config
import me.jfenn.bingo.utils.lazyRef
import net.minecraft.util.Formatting

enum class GeneratorTier(
    val formatting: Formatting,
) {
    S(Formatting.DARK_RED),
    A(Formatting.RED),
    B(Formatting.GOLD),
    C(Formatting.YELLOW),
    D(Formatting.GREEN),
    ;

    val items by lazyRef {
        GeneratorTier.readFile("${name.lowercase()}_tier.txt")
    }

    fun pick(count: Int): List<String> {
        return items.shuffled()
            .take(count)
            .map { it.random() }
    }

    companion object {
        private fun readFile(name: String): List<Set<String>> {
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

            return items
        }
    }
}