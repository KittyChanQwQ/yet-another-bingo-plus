package me.jfenn.bingo.data

import me.jfenn.bingo.minecraftServer
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.scoreboard.Team
import net.minecraft.text.Text
import net.minecraft.util.Formatting

enum class BingoTeam(
    val color: Formatting,
) {
    BLUE(Formatting.BLUE),
    RED(Formatting.RED),
    GREEN(Formatting.GREEN),
    ;

    val id = this.name.lowercase()

    init {
        val team = minecraftServer.scoreboard.addTeam(id)
        team.color = color
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