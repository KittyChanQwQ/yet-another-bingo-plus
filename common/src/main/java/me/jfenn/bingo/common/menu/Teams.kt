package me.jfenn.bingo.common.menu

import me.jfenn.bingo.common.card.BingoTeam
import me.jfenn.bingo.common.card.TeamService
import me.jfenn.bingo.common.Bingo
import me.jfenn.bingo.common.utils.sendInfoMessage
import me.jfenn.bingo.common.utils.text.TextInfo
import net.minecraft.entity.EntityType
import net.minecraft.util.math.Vec3d

fun MenuComponent.registerTeams(
    position: Vec3d,
    requiredPermissionLevel: Int = 2,
    title: String = "Teams",
) {
    var teamCount = 3

    registerButton(
        position = position.add(0.0, -MENU_ITEM_OFFSET, 0.0),
        text = "+",
        requiredPermissionLevel = requiredPermissionLevel,
    ) {
        teamCount = (teamCount + 1).coerceIn(1, BingoTeam.entries.size)
        markDirty()
    }

    registerButton(
        position = position.add(0.0, -(MENU_ITEM_OFFSET + 2 * MENU_ITEM_HEIGHT), 0.0),
        text = "-",
        requiredPermissionLevel = requiredPermissionLevel,
    ) {
        teamCount = (teamCount - 1).coerceIn(1, BingoTeam.entries.size)
        markDirty()
    }

    registerButton(
        position = position.add(0.0, -(MENU_ITEM_OFFSET + 3 * MENU_ITEM_HEIGHT), 0.0),
        text = "Shuffle!",
        requiredPermissionLevel = requiredPermissionLevel,
    ) { player ->
        val ctx = player.server?.let { Bingo.getContext(it) } ?: return@registerButton
        TeamService.shuffleTeams(ctx, teamCount)
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
