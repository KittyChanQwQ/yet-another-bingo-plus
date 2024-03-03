package me.jfenn.bingo.common.config

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.decodeFromStream
import me.jfenn.bingo.common.scope.BingoComponent
import me.jfenn.bingo.common.utils.json
import org.slf4j.Logger
import java.io.FileInputStream

class TierListService(
    private val log: Logger,
) : BingoComponent() {

    companion object {
        fun readTierLists() : List<String> {
            return Config.configDir
                .resolve("bingo")
                .toFile()
                .listFiles()
                .orEmpty()
                .filter { it.name.endsWith(".tierlist.json") }
                .map { it.name.removeSuffix(".tierlist.json") }
                .sorted()
        }
    }

    private fun readTierList(name: String): TierListConfig? {
        val file = Config.configDir
            .resolve("bingo/$name.tierlist.json")
            .toFile()

        val config = runCatching {
            FileInputStream(file).use {
                @OptIn(ExperimentalSerializationApi::class)
                json.decodeFromStream<TierListConfig>(it)
            }
        }

        if (config.isFailure) {
            log.error("Unable to read tier list '$name.tierlist.json'", config.exceptionOrNull())
        }

        return config.getOrNull()
    }

    val tierLists = readTierLists()
        .mapNotNull { name ->
            readTierList(name)?.let { name to it }
        }
        .toMap()

    fun getTierLists(names: List<String>): TierListConfig {
        var config = TierListConfig.EMPTY

        names.sorted()
            .mapNotNull { tierLists[it] }
            .forEach { config = config.combine(it) }

        return config
    }

}