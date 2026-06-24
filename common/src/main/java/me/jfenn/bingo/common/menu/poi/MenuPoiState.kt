package me.jfenn.bingo.common.menu.poi

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import me.jfenn.bingo.common.team.BingoTeamKey
import me.jfenn.bingo.common.utils.BlockPositionType
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import kotlin.io.path.exists

@Serializable
internal data class MenuPoiState(
    var poi: MutableList<MenuPoi> = mutableListOf(),
) {
    @OptIn(ExperimentalSerializationApi::class)
    fun read(path: Path) {
        val state = path.takeIf { it.exists() }
            ?.let { Files.newInputStream(it) }
            ?.buffered()
            ?.let { GZIPInputStream(it) }
            ?.use {
                Json.decodeFromStream<MenuPoiState>(it)
            }

        poi = state?.poi ?: mutableListOf()
    }

    @OptIn(ExperimentalSerializationApi::class)
    fun write(path: Path) {
        path.parent.toFile().mkdirs()
        Files.newOutputStream(path)
            .buffered()
            .let { GZIPOutputStream(it) }
            .use {
                Json.encodeToStream<MenuPoiState>(this, it)
            }
    }
}

@Serializable
internal sealed interface MenuPoi {
    val pos: BlockPositionType

    @Serializable
    @SerialName("spawn")
    data class Spawn(override val pos: BlockPositionType, val yaw: Float) : MenuPoi

    @Serializable
    @SerialName("menu")
    data class Menu(
        override val pos: BlockPositionType,
        val yaw: Double,
    ): MenuPoi

    @Serializable
    @SerialName("team")
    data class Team(
        override val pos: BlockPositionType,
        val key: BingoTeamKey,
    ): MenuPoi
}