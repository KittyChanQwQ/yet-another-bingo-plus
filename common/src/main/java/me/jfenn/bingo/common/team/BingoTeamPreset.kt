package me.jfenn.bingo.common.team

import kotlinx.serialization.Serializable
import me.jfenn.bingo.platform.text.Formatting
import me.jfenn.bingo.platform.text.ITextSerialized

@Serializable
class BingoTeamPreset(
    val name: ITextSerialized,
    val shouldFormatName: Boolean = false,
    val symbol: String? = null,
    val color: Formatting = Formatting.RESET,
    val blockId: String? = null,
)
