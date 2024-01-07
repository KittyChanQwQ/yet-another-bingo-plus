package me.jfenn.bingo.common.card

import me.jfenn.bingo.api.ITeamHandle
import me.jfenn.bingo.api.ITeamManager
import me.jfenn.bingo.common.state.BingoContext
import me.jfenn.bingo.common.utils.text.TextColor
import net.minecraft.block.Block
import net.minecraft.block.Blocks
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.HoverEvent
import net.minecraft.text.MutableText
import net.minecraft.text.Style
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

enum class BingoTeam(
    val formatting: Formatting,
    val textColor: TextColor,
    val color: Byte,
    val carpetBlock: Block,
) {
    BLUE(Formatting.BLUE, TextColor.BLUE, 12, Blocks.BLUE_CARPET),
    RED(Formatting.RED, TextColor.RED, 4, Blocks.RED_CARPET),
    GREEN(Formatting.GREEN, TextColor.GREEN, 33, Blocks.GREEN_CARPET),
    YELLOW(Formatting.YELLOW, TextColor.YELLOW, 30, Blocks.YELLOW_CARPET),
    PINK(Formatting.LIGHT_PURPLE, TextColor.LIGHT_PURPLE, 20, Blocks.PINK_CARPET),
    AQUA(Formatting.AQUA, TextColor.AQUA, 31, Blocks.CYAN_CARPET),
    ORANGE(Formatting.GOLD, TextColor.GOLD, 15, Blocks.ORANGE_CARPET),
    GRAY(Formatting.GRAY, TextColor.GRAY, 21, Blocks.LIGHT_GRAY_CARPET),
    ;

    val id = this.name.lowercase()

    fun includesPlayer(player: ServerPlayerEntity): Boolean {
        return fromPlayer(player) == this
    }

    fun team(server: MinecraftServer): ITeamHandle {
        return teamManager.createTeam(
            server,
            teamName = id,
            displayName = Text.of(name.substring(0, 1) + name.substring(1).lowercase()),
            color = formatting,
        )
    }

    fun text(): MutableText {
        return Text.empty()
            .append("[")
            .append(Text.literal("$name team").formatted(formatting))
            .append("]")
    }

    fun text(server: MinecraftServer): Text {
        val text = this.text()
        text.style = Style.EMPTY.withHoverEvent(HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal(team(server).playerList.joinToString(", "))))
        return text
    }

    fun broadcastMessage(server: MinecraftServer, text: Text) {
        server.playerManager.playerList
            .filter { includesPlayer(it) }
            .forEach { it.sendMessage(text) }
    }

    fun getMapId(ctx: BingoContext): Int {
        return ctx.state.teamMapIds.getOrPut(this) {
            ctx.server.overworld.nextMapId
        }
    }

    fun getMapName(ctx: BingoContext): String {
        return "map_${getMapId(ctx)}"
    }

    companion object : KoinComponent {

        private val TEAM_ID_INDEX = entries.associateBy { it.id }

        private val teamManager by inject<ITeamManager>()

        fun fromPlayer(player: ServerPlayerEntity): BingoTeam? {
            val team = teamManager.getPlayerTeam(player) ?: return null
            return TEAM_ID_INDEX[team.name]
        }

        fun fromPlayer(server: MinecraftServer, player: PlayerEntity): BingoTeam? {
            return server.playerManager.getPlayer(player.uuid)
                ?.let { fromPlayer(it) }
        }
    }

}