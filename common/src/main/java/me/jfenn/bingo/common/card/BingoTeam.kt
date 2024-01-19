package me.jfenn.bingo.common.card

import kotlinx.serialization.Serializable
import me.jfenn.bingo.common.BingoKoin
import me.jfenn.bingo.common.commands.CoordsCommand
import me.jfenn.bingo.common.config.BlockPosition
import me.jfenn.bingo.common.map.BingoMap
import me.jfenn.bingo.common.state.TeamScore
import me.jfenn.bingo.common.state.TeamWinner
import me.jfenn.bingo.common.utils.text.TextColor
import net.minecraft.block.Blocks
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.HoverEvent
import net.minecraft.text.Style
import net.minecraft.text.Text
import net.minecraft.util.Formatting

@Serializable
@JvmInline
value class BingoTeamKey(
    val id: String,
)

@Serializable
data class BingoTeam(
    val id: String,
    val name: String,
    val textColor: TextColor,
    var score: TeamScore = TeamScore.ZERO,
    var winner: TeamWinner? = null,
    var spawnpoint: BlockPosition? = null,
    val chatCoordinates: MutableList<CoordsCommand.CoordinatesEntry> = mutableListOf(),
    var map: BingoMap? = null,
    val players: MutableSet<BingoPlayerProfile> = mutableSetOf(),
) {

    val key get() = BingoTeamKey(id)

    val formatting get() = Formatting.valueOf(textColor.name)

    val mapColor: Byte get() = when (textColor) {
        TextColor.BLUE -> 12
        TextColor.RED -> 4
        TextColor.GREEN -> 33
        TextColor.YELLOW -> 30
        TextColor.LIGHT_PURPLE -> 20
        TextColor.AQUA -> 31
        TextColor.GOLD -> 15
        TextColor.GRAY -> 21
        else -> 22
    }

    fun getMap(server: MinecraftServer): BingoMap {
        return this.map ?: run {
            val map = BingoMap(
                mapId = server.overworld.nextMapId,
            )
            this.map = map
            return@run map
        }
    }

    fun isWinner(): Boolean = this.winner != null

    fun includesPlayer(player: ServerPlayerEntity): Boolean {
        return fromPlayer(player) == this
    }

    fun text(): Text {
        val text = Text.empty()
            .append("[")
            .append(Text.literal("$name team").formatted(formatting))
            .append("]")

        text.style = Style.EMPTY.withHoverEvent(
            HoverEvent(
                HoverEvent.Action.SHOW_TEXT,
                Text.literal(
                    players.joinToString(", ") { it.name }
                )
            )
        )

        return text
    }

    fun broadcastMessage(server: MinecraftServer, text: Text) {
        server.playerManager.playerList
            .filter { includesPlayer(it) }
            .forEach { it.sendMessage(text) }
    }

    companion object {

        val BLUE get() = BingoTeam("bingo_blue", "BLUE", TextColor.BLUE)
        val RED get() = BingoTeam("bingo_red", "RED", TextColor.RED)
        val GREEN get() = BingoTeam("bingo_green", "GREEN", TextColor.GREEN)
        val YELLOW get() = BingoTeam("bingo_yellow", "YELLOW", TextColor.YELLOW)
        val PINK get() = BingoTeam("bingo_pink", "PINK", TextColor.LIGHT_PURPLE)
        val AQUA get() = BingoTeam("bingo_aqua", "AQUA", TextColor.AQUA)
        val ORANGE get() = BingoTeam("bingo_orange", "ORANGE", TextColor.GOLD)
        val GRAY get() = BingoTeam("bingo_gray", "GRAY", TextColor.GRAY)

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

        fun fromPlayer(server: MinecraftServer, player: PlayerEntity): BingoTeam? {
            return server.playerManager.getPlayer(player.uuid)
                ?.let { fromPlayer(it) }
        }
    }

}