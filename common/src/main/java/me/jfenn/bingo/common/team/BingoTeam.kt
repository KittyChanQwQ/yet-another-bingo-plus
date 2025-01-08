package me.jfenn.bingo.common.team

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.JsonElement
import me.jfenn.bingo.common.chat.CoordsCommand
import me.jfenn.bingo.common.map.BingoMap
import me.jfenn.bingo.common.scope.BingoKoin
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.common.utils.*
import me.jfenn.bingo.generated.StringKey
import me.jfenn.bingo.platform.IPlayerHandle
import me.jfenn.bingo.platform.IServerWorld
import me.jfenn.bingo.platform.block.BlockPosition
import net.minecraft.block.Blocks
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.HoverEvent
import net.minecraft.text.Text
import net.minecraft.text.Texts
import net.minecraft.util.Formatting
import java.util.concurrent.CompletableFuture

@Serializable
@JvmInline
value class BingoTeamKey(
    val id: String,
) {
    val label get() = id.split('_').last()
}

@Serializable
data class BingoTeam(
    val id: String,
    val name: StringKey?,
    var nameText: JsonElement? = null,
    val symbol: String?,
    @Serializable(with = FormattingSerializer::class)
    var textColor: Formatting,
    val isTemporary: Boolean = false,
    var cardId: UuidAsString? = null,
    var score: TeamScore = TeamScore.ZERO,
    var winner: TeamWinner? = null,
    val completedCards: MutableList<TeamCompletedCard> = mutableListOf(),
    var spawnpoint: BlockPositionType? = null,
    var chestSpawnpoint: BlockPositionType? = null,
    val chatCoordinates: MutableList<CoordsCommand.CoordinatesEntry> = mutableListOf(),
    var map: BingoMap? = null,
    val players: MutableSet<PlayerProfileType> = mutableSetOf(),
) {

    @Transient
    var scoreUpdatedAt: Instant = Instant.DISTANT_PAST

    /**
     * Creates a copy of the team that does not share any mutable
     * references with the original
     */
    fun newInstance(): BingoTeam {
        return copy(
            completedCards = mutableListOf(),
            chatCoordinates = mutableListOf(),
            players = mutableSetOf(),
        )
    }

    @Transient
    var spawnpointFuture: CompletableFuture<BlockPosition>? = null

    @Transient
    var spawnpointTicket: IServerWorld.IChunkTicketHandle? = null

    val key get() = BingoTeamKey(id)

    val mapColor get() = when (textColor) {
        // gray is indistinguishable from the preview card map item
        Formatting.GRAY -> Formatting.DARK_GRAY
        else -> textColor
    }

    val singlePlayer get() = players.takeIf { it.size == 1 }?.first()

    val totalScore: TeamScore get() {
        val containsScore = completedCards.any { it.card.id == cardId }
        val scores = completedCards.map { it.score } + (if (containsScore) emptyList() else listOf(score))
        return TeamScore(
            cards = score.cards,
            lines = scores.sumOf { it.lines },
            items = scores.sumOf { it.items },
        )
    }

    fun isWinner(): Boolean = this.winner != null

    fun isPlaying() = !isWinner()

    fun includesPlayer(player: IPlayerHandle): Boolean {
        return players.any { it.uuid == player.uuid }
    }

    fun includesPlayer(player: ServerPlayerEntity): Boolean {
        return players.any { it.uuid == player.uuid }
    }

    fun getSimpleName(textProvider: TextProvider) =
        name?.let { textProvider.string(name) }
            ?: nameText?.let { textProvider.deserialized(it) }
            ?: Text.empty()

    fun getName(
        textProvider: TextProvider,
        playerName: Boolean = false,
        symbol: Boolean = playerName && players.size == 1,
        bracketed: Boolean = true,
        teamNameKey: StringKey? = StringKey.TeamName,
    ): Text {
        val teamName = (if (playerName) singlePlayer?.let { Text.literal(it.name) } else null)
            ?: name?.let { textProvider.string(name) }?.let {
                if (teamNameKey != null) textProvider.string(teamNameKey, it) else it
            }
            ?: getSimpleName(textProvider)

        return teamName
            .let { if (symbol && this.symbol != null) Text.empty().append("${this.symbol} ").append(it) else it }
            .formatted(this.textColor)
            .let { if (bracketed) Texts.bracketed(it) else it }
            .also { t ->
                t.style = t.style.withHoverEvent(
                    HoverEvent(
                        HoverEvent.Action.SHOW_TEXT,
                        players.map { Text.literal(it.name) }.joinText(Text.literal("\n"))
                    )
                )
            }
    }

    fun countCards(): Int = completedCards.count { it.isWinner }

    companion object {

        val BLUE get() = BingoTeam("bingo_blue", StringKey.TeamBlue, null, "\uD83C\uDF0A", Formatting.BLUE)
        val RED get() = BingoTeam("bingo_red", StringKey.TeamRed, null, "\uD83D\uDD25", Formatting.RED)
        val GREEN get() = BingoTeam("bingo_green", StringKey.TeamGreen, null, "\uD83C\uDF42", Formatting.GREEN)
        val YELLOW get() = BingoTeam("bingo_yellow", StringKey.TeamYellow, null, "\uD83D\uDC1D", Formatting.YELLOW)
        val PINK get() = BingoTeam("bingo_pink", StringKey.TeamPink, null, "\uD83C\uDF3A", Formatting.LIGHT_PURPLE)
        val AQUA get() = BingoTeam("bingo_aqua", StringKey.TeamAqua, null, "\uD83C\uDF44", Formatting.AQUA)
        val ORANGE get() = BingoTeam("bingo_orange", StringKey.TeamOrange, null, "\uD83C\uDFF0", Formatting.GOLD)
        val GRAY get() = BingoTeam("bingo_gray", StringKey.TeamGray, null, "\uD83E\uDEA8", Formatting.GRAY)

        val TEAM_BLOCKS get() = mapOf(
            Blocks.BLUE_CARPET to BLUE,
            Blocks.RED_CARPET to RED,
            Blocks.GREEN_CARPET to GREEN,
            Blocks.YELLOW_CARPET to YELLOW,
            Blocks.PINK_CARPET to PINK,
            Blocks.CYAN_CARPET to AQUA,
            Blocks.ORANGE_CARPET to ORANGE,
            Blocks.LIGHT_GRAY_CARPET to GRAY,
        )

        fun fromPlayer(player: ServerPlayerEntity): BingoTeam? {
            val scope = BingoKoin.getScope(player.server) ?: return null
            val teamService = scope.get<TeamService>()
            return teamService.getPlayerTeam(player)
        }
    }

}