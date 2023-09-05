package me.jfenn.bingo.card

import me.jfenn.bingo.minecraftServer
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.scoreboard.Team
import net.minecraft.text.HoverEvent
import net.minecraft.text.Style
import net.minecraft.text.Text
import net.minecraft.util.Formatting

enum class BingoTeam(
    val formatting: Formatting,
    val color: Byte,
) {
    BLUE(Formatting.BLUE, 12),
    RED(Formatting.RED, 4),
    GREEN(Formatting.GREEN, 33),
    YELLOW(Formatting.YELLOW, 30),
    PINK(Formatting.LIGHT_PURPLE, 20),
    AQUA(Formatting.AQUA, 31),
    ORANGE(Formatting.GOLD, 15),
    GRAY(Formatting.GRAY, 21),
    ;

    val id = this.name.lowercase()

    fun team(): Team {
        return minecraftServer.scoreboard.getTeam(id)
            ?: minecraftServer.scoreboard.addTeam(id).apply {
                color = formatting
                displayName = Text.of(name.substring(0, 1) + name.substring(1).lowercase())
            }
    }

    fun text(): Text {
        val text = Text.empty()
            .append("[")
            .append(Text.literal("$name team").formatted(formatting))
            .append("]")

        text.style = Style.EMPTY.withHoverEvent(HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal(team().playerList.joinToString(", "))))
        return text
    }

    fun broadcastMessage(text: Text) {
        minecraftServer.playerManager.playerList
            .filter { it.isTeamPlayer(team()) }
            .forEach { it.sendMessage(text) }
    }

    val mapId by lazy { minecraftServer.overworld.nextMapId }
    val mapName by lazy { "map_${mapId}" }

    companion object {

        private val TEAM_ID_INDEX = BingoTeam.values().associateBy { it.id }

        fun fromPlayer(player: PlayerEntity): BingoTeam? {
            val team = player.scoreboardTeam ?: return null
            return TEAM_ID_INDEX[team.name]
        }
    }

}