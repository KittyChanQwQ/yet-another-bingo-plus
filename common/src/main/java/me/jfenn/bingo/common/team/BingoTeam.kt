package me.jfenn.bingo.common.team

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import me.jfenn.bingo.api.IServerWorld
import me.jfenn.bingo.common.commands.CoordsCommand
import me.jfenn.bingo.common.config.BlockPosition
import me.jfenn.bingo.common.map.BingoMap
import me.jfenn.bingo.common.scope.BingoKoin
import me.jfenn.bingo.common.state.TeamScore
import me.jfenn.bingo.common.state.TeamWinner
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.common.utils.FormattingSerializer
import me.jfenn.bingo.common.utils.text.TextColor
import me.jfenn.bingo.generated.StringKey
import net.minecraft.block.Blocks
import net.minecraft.server.network.ServerPlayerEntity
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
    val name: StringKey,
    val symbol: String,
    @Serializable(with = FormattingSerializer::class)
    val mapColor: Formatting,
    val textColor: TextColor,
    var score: TeamScore = TeamScore.ZERO,
    var winner: TeamWinner? = null,
    var spawnpoint: BlockPosition? = null,
    var chestSpawnpoint: BlockPosition? = null,
    val chatCoordinates: MutableList<CoordsCommand.CoordinatesEntry> = mutableListOf(),
    var map: BingoMap? = null,
    val players: MutableSet<BingoPlayerProfile> = mutableSetOf(),
) {

    @Transient
    var spawnpointFuture: CompletableFuture<BlockPosition>? = null

    @Transient
    var spawnpointTicket: IServerWorld.IChunkTicketHandle? = null

    val key get() = BingoTeamKey(id)

    val formatting get() = Formatting.valueOf(textColor.name)

    val singlePlayer get() = players.takeIf { it.size == 1 }?.first()

    fun isWinner(): Boolean = this.winner != null

    fun includesPlayer(player: ServerPlayerEntity): Boolean {
        return players.any { it.uuid == player.uuid }
    }

    fun getName(
        textProvider: TextProvider,
        playerName: Boolean = false,
        symbol: Boolean = playerName && players.size == 1,
        bracketed: Boolean = true,
        teamNameKey: StringKey? = StringKey.TeamName,
    ): Text {
        val teamName = (if (playerName) singlePlayer?.let { Text.literal(it.name) } else null)
            ?: textProvider.string(name).let {
                if (teamNameKey != null) textProvider.string(teamNameKey, it) else it
            }

        return teamName
            .let { if (symbol) Text.empty().append("${this.symbol} ").append(it) else it }
            .formatted(formatting)
            .let { if (bracketed) Texts.bracketed(it) else it }
    }

    companion object {

        val BLUE get() = BingoTeam("bingo_blue", StringKey.TeamBlue, "\uD83C\uDF0A", Formatting.BLUE, TextColor.BLUE)
        val RED get() = BingoTeam("bingo_red", StringKey.TeamRed, "\uD83D\uDD25", Formatting.RED, TextColor.RED)
        val GREEN get() = BingoTeam("bingo_green", StringKey.TeamGreen, "\uD83C\uDF42", Formatting.GREEN, TextColor.GREEN)
        val YELLOW get() = BingoTeam("bingo_yellow", StringKey.TeamYellow, "\uD83D\uDC1D", Formatting.YELLOW, TextColor.YELLOW)
        val PINK get() = BingoTeam("bingo_pink", StringKey.TeamPink, "\uD83C\uDF3A", Formatting.LIGHT_PURPLE, TextColor.LIGHT_PURPLE)
        val AQUA get() = BingoTeam("bingo_aqua", StringKey.TeamAqua, "\uD83C\uDF44", Formatting.AQUA, TextColor.AQUA)
        val ORANGE get() = BingoTeam("bingo_orange", StringKey.TeamOrange, "\uD83C\uDFF0", Formatting.GOLD, TextColor.GOLD)
        val GRAY get() = BingoTeam("bingo_gray", StringKey.TeamGray, "\uD83E\uDEA8", Formatting.DARK_GRAY, TextColor.GRAY)

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