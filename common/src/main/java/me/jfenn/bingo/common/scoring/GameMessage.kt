package me.jfenn.bingo.common.scoring

import kotlinx.serialization.Serializable
import me.jfenn.bingo.client.platform.text.ITextSerialized
import me.jfenn.bingo.common.map.CardTile
import me.jfenn.bingo.common.team.BingoTeamKey
import me.jfenn.bingo.platform.item.IItemStackSerialized
import me.jfenn.bingo.platform.player.PlayerProfile
import me.jfenn.bingo.platform.utils.UuidAsString
import java.util.*
import kotlin.time.Duration

@Serializable
sealed class GameMessage {

    val id: UuidAsString = UUID.randomUUID()
    abstract val timeElapsed: Duration
    abstract val team: BingoTeamKey?

    @Serializable
    sealed interface MessageIcon {
        @Serializable
        class Item(
            val item: IItemStackSerialized,
            val itemList: List<IItemStackSerialized>,
        ) : MessageIcon
        @Serializable
        class Texture(
            val texture: String,
        ) : MessageIcon
    }

    @Serializable
    data class ItemScored(
        override val timeElapsed: Duration,
        override val team: BingoTeamKey,
        val cardId: UuidAsString,
        val item: IItemStackSerialized?,
        val itemList: List<IItemStackSerialized>?,
        val texture: String?,
        val decoration: CardTile.Decoration?,
        val itemName: ITextSerialized,
        val player: PlayerProfile?,
        val isLost: Boolean,
    ) : GameMessage()

    @Serializable
    data class LineScored(
        override val timeElapsed: Duration,
        override val team: BingoTeamKey,
        val lines: Int,
    ) : GameMessage()

    @Serializable
    data class CardCompleted(
        override val timeElapsed: Duration,
        override val team: BingoTeamKey,
        val isAutoWin: Boolean,
    ) : GameMessage()

    @Serializable
    data class LeadingTeam(
        override val timeElapsed: Duration,
        override val team: BingoTeamKey,
        val cards: Int?,
        val lines: Int?,
        val items: Int?,
        val isTied: Boolean,
    ) : GameMessage()
}