package me.jfenn.bingo.common.config

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.decodeFromStream
import me.jfenn.bingo.common.scope.BingoKoin
import me.jfenn.bingo.common.utils.json
import org.slf4j.Logger
import java.io.FileInputStream

object TierListService {

    private val log by BingoKoin.koinApp.koin.inject<Logger>()

    init {
        // initialize default tier lists
        Config.readStream("all_items.tierlist.json").close()
        Config.readStream("default.tierlist.json").close()
        Config.readStream("nether-only.tierlist.json").close()
        Config.readStream("overworld-only.tierlist.json").close()
    }

    val tierLists by lazy {
        Config.configDir
            .resolve("bingo")
            .toFile()
            .listFiles()
            .orEmpty()
            .filter { it.name.endsWith(".tierlist.json") }
            .map { it.name.removeSuffix(".tierlist.json") }
            .sorted()
    }

    fun getTierList(name: String): TierListConfig? {
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

    fun getTierLists(names: List<String>): TierListConfig {
        var config = TierListConfig.EMPTY

        names.sorted()
            .mapNotNull { getTierList(it) }
            .forEach { config = config.combine(it) }

        return config
    }

}