package me.jfenn.bingo.card

import me.jfenn.bingo.config.Config
import me.jfenn.bingo.utils.lazyRef
import net.minecraft.registry.Registries
import net.minecraft.util.Formatting
import net.minecraft.util.Identifier
import org.slf4j.LoggerFactory

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

    fun pick(count: Int, excluding: Set<Identifier>): List<Identifier> {
        return items.shuffled()
            .asSequence()
            .map { set -> set.filter { !excluding.contains(it) } }
            .filter { it.isNotEmpty() }
            .take(count)
            .map { it.random() }
            .toList()
    }

    companion object {

        private val log = LoggerFactory.getLogger(this::class.java)

        private fun parseIdentifier(str: String): Identifier? {
            val name = str.trim()
                .lowercase()
                .let {
                    if (it.contains(":"))
                        it
                    else "minecraft:$it"
                }

            val identifier = Identifier.tryParse(name) ?: run {
                log.error("Could not parse item identifier '${name}'")
                return null
            }

            if (!Registries.ITEM.containsId(identifier)) {
                log.error("Item identifier '${name}' does not exist")
                return null
            }

            return identifier
        }

        private fun readFile(name: String): List<Set<Identifier>> {
            val inputStream = Config.readStream(name)

            val items = mutableListOf<Set<Identifier>>()
            inputStream.bufferedReader().useLines { lines ->
                for (line in lines) {
                    line.split("|")
                        // TODO: filter only valid item ids?
                        .filter { it.isNotEmpty() }
                        .mapNotNull { parseIdentifier(it) }
                        .toSet()
                        .takeIf { it.isNotEmpty() }
                        ?.also { items.add(it) }
                }
            }

            return items
        }
    }
}