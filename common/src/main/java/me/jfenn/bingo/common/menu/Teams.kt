package me.jfenn.bingo.common.menu

import me.jfenn.bingo.common.card.BingoTeam
import me.jfenn.bingo.common.card.TeamService
import me.jfenn.bingo.common.utils.sendInfoMessage
import me.jfenn.bingo.common.utils.text.TextInfo
import net.minecraft.entity.EntityType
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.math.Vec3d

fun MenuComponent.registerTeams(
    position: Vec3d,
    title: String = "Teams",
    permissionGetter: (ServerPlayerEntity) -> Boolean,
    teamService: TeamService = koinScope.get(),
) {
    var teamCount = 3

    registerButton(
        position = position.add(0.0, -MENU_ITEM_OFFSET, 0.0),
        text = "+",
        permissionGetter = permissionGetter,
    ) {
        teamCount = (teamCount + 1).coerceIn(1, BingoTeam.TEAM_BLOCKS.size)
        markDirty()
    }

    registerButton(
        position = position.add(0.0, -(MENU_ITEM_OFFSET + 2 * MENU_ITEM_HEIGHT), 0.0),
        text = "-",
        permissionGetter = permissionGetter,
    ) {
        teamCount = (teamCount - 1).coerceIn(1, BingoTeam.TEAM_BLOCKS.size)
        markDirty()
    }

    registerButton(
        position = position.add(0.0, -(MENU_ITEM_OFFSET + 3 * MENU_ITEM_HEIGHT), 0.0),
        text = "Shuffle!",
        permissionGetter = permissionGetter,
    ) { player ->
        teamService.shuffleTeams(teamCount)
        player.sendInfoMessage("Teams have been shuffled!")
    }

    registerEntity(EntityType.TEXT_DISPLAY, position.add(0.0, -(MENU_ITEM_OFFSET + MENU_ITEM_HEIGHT), 0.0)) {
        putString("text", "\"$teamCount teams\"")
        putString("billboard", "fixed")
        putString("alignment", "center")
        putInt("background", 0)
        putBoolean("shadow", true)
    }

    registerEntity(EntityType.TEXT_DISPLAY, position) {
        putString(
            "text", TextInfo.toJsonString(
                TextInfo.literal(title)
            )
        )
        putString("billboard", "fixed")
        putString("alignment", "center")
        putInt("background", 0)
        putBoolean("shadow", true)
    }
}
