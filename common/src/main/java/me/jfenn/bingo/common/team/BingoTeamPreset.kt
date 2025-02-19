package me.jfenn.bingo.common.team

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import me.jfenn.bingo.common.utils.FormattingSerializer
import net.minecraft.util.Formatting

@Serializable
class BingoTeamPreset(
    val name: JsonElement,
    val shouldFormatName: Boolean = false,
    val symbol: String? = null,
    @Serializable(with = FormattingSerializer::class)
    val color: Formatting = Formatting.RESET,
    val blockId: String? = null,
)
