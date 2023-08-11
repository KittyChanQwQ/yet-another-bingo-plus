package me.jfenn.bingo.team

import me.jfenn.bingo.minecraftServer
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.scoreboard.Team
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

    init {
        val team = minecraftServer.scoreboard.addTeam(id)
        team.color = formatting
        team.displayName = Text.of(name.substring(0, 1) + name.substring(1).lowercase())
    }

    fun team(): Team {
        return minecraftServer.scoreboard.getTeam(id)!!
    }

    fun addPlayer(player: PlayerEntity) {
        minecraftServer.scoreboard.addPlayerToTeam(player.entityName, team())
    }

    fun removePlayer(player: PlayerEntity) {
        minecraftServer.scoreboard.removePlayerFromTeam(player.entityName, team())
    }

    val mapId by lazy { minecraftServer.overworld.nextMapId }
    val mapName by lazy { "map_${mapId}" }

    companion object {
        fun fromPlayer(player: PlayerEntity): BingoTeam? {
            val team = minecraftServer.scoreboard.getPlayerTeam(player.entityName)
            return BingoTeam.values().find { team?.name == it.id }
        }
    }

}