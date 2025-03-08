package me.jfenn.bingo.common.team

import kotlinx.serialization.Serializable
import me.jfenn.bingo.client.platform.text.ITextSerialized
import me.jfenn.bingo.common.utils.FormattingSerializer
import net.minecraft.util.Formatting

@Serializable
class BingoTeamPreset(
    val name: ITextSerialized,
    val shouldFormatName: Boolean = false,
    val symbol: String? = null,
    @Serializable(with = FormattingSerializer::class)
    val color: Formatting = Formatting.RESET,
    val blockId: String? = null,
)
