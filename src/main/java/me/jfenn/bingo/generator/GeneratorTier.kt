package me.jfenn.bingo.generator

import net.fabricmc.loader.api.FabricLoader
import java.io.File

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
            val path = FabricLoader.getInstance().configDir
                .resolve("bingo/item_data/${name}")

            return fromFile(path.toFile())
        }

        fun fromFile(file: File): GeneratorTier {
            val items = mutableListOf<Set<String>>()

            file.bufferedReader().useLines { lines ->
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