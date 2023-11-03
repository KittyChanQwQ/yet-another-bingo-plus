package me.jfenn.bingo.menu

import me.jfenn.bingo.card.BingoTeam
import me.jfenn.bingo.card.TeamService
import me.jfenn.bingo.utils.sendInfoMessage
import me.jfenn.bingo.utils.toNbt
import net.minecraft.entity.EntityType
import net.minecraft.text.Text
import net.minecraft.util.math.Vec3d

fun MenuComponent.registerTeams(
    position: Vec3d,
    requiredPermissionLevel: Int = 2,
    title: String = "Teams",
) {
    var teamCount by property(MutableProperty<Int>(3))

    registerButton(
        position = position.add(0.0, -MENU_ITEM_OFFSET, 0.0),
        text = "+",
        requiredPermissionLevel = requiredPermissionLevel,
    ) {
        teamCount = (teamCount + 1).coerceIn(1, BingoTeam.values().size)
    }

    registerButton(
        position = position.add(0.0, -(MENU_ITEM_OFFSET + 2 * MENU_ITEM_HEIGHT), 0.0),
        text = "-",
        requiredPermissionLevel = requiredPermissionLevel,
    ) {
        teamCount = (teamCount - 1).coerceIn(1, BingoTeam.values().size)
    }

    registerButton(
        position = position.add(0.0, -(MENU_ITEM_OFFSET + 3 * MENU_ITEM_HEIGHT), 0.0),
        text = "Shuffle!",
        requiredPermissionLevel = requiredPermissionLevel,
    ) { player ->
        TeamService.shuffleTeams(teamCount)
        player.sendInfoMessage("Teams have been shuffled!")
    }

    registerEntity(EntityType.TEXT_DISPLAY) {
        put("Pos", position.add(0.0, -(MENU_ITEM_OFFSET + MENU_ITEM_HEIGHT), 0.0).toNbt())
        putString(
            "text", Text.Serializer.toJson(
                Text.literal("$teamCount teams")
            )
        )
        putString("billboard", "fixed")
        putString("alignment", "center")
        putInt("background", 0)
        putBoolean("shadow", true)
    }

    registerEntity(EntityType.TEXT_DISPLAY) {
        put("Pos", position.toNbt())
        putString(
            "text", Text.Serializer.toJson(
                Text.literal(title)
            )
        )
        putString("billboard", "fixed")
        putString("alignment", "center")
        putInt("background", 0)
        putBoolean("shadow", true)
    }
}
