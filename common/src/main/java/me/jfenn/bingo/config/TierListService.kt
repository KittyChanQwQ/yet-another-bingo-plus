package me.jfenn.bingo.config

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.decodeFromStream
import me.jfenn.bingo.utils.json
import me.jfenn.bingo.utils.lazyRef
import org.slf4j.LoggerFactory
import java.io.FileInputStream

object TierListService {

    private val log = LoggerFactory.getLogger(this::class.java)

    init {
        // initialize default tier lists
        Config.readStream("overworld.tierlist.json").close()
        Config.readStream("nether.tierlist.json").close()
    }

    val tierLists = Config.configDir
        .resolve("bingo")
        .toFile()
        .listFiles()
        .orEmpty()
        .filter { it.name.endsWith(".tierlist.json") }
        .map { it.name.removeSuffix(".tierlist.json") }

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

        names.mapNotNull { getTierList(it) }
            .forEach { config = config.combine(it) }

        return config
    }

}