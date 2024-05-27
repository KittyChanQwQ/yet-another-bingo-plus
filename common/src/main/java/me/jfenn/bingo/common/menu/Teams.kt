package me.jfenn.bingo.common.menu

import me.jfenn.bingo.common.team.BingoTeam
import me.jfenn.bingo.common.team.TeamService
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.common.utils.sendInfoMessage
import me.jfenn.bingo.generated.StringKey
import net.minecraft.entity.EntityType
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.math.Vec3d

fun MenuComponent.registerTeams(
    position: Vec3d,
    permissionGetter: (ServerPlayerEntity) -> Boolean,
    teamService: TeamService = koinScope.get(),
    text: TextProvider = koinScope.get(),
) {
    var teamCount = 3

    registerButton(
        position = position.add(0.0, -MENU_ITEM_OFFSET, 0.0),
        text = Text.literal("+"),
        permissionGetter = permissionGetter,
    ) {
        teamCount = (teamCount + 1).coerceIn(1, BingoTeam.TEAM_BLOCKS.size)
        markDirty()
    }

    registerButton(
        position = position.add(0.0, -(MENU_ITEM_OFFSET + 2 * MENU_ITEM_HEIGHT), 0.0),
        text = Text.literal("-"),
        permissionGetter = permissionGetter,
    ) {
        teamCount = (teamCount - 1).coerceIn(1, BingoTeam.TEAM_BLOCKS.size)
        markDirty()
    }

    registerButton(
        position = position.add(0.0, -(MENU_ITEM_OFFSET + 3 * MENU_ITEM_HEIGHT), 0.0),
        text = text.string(StringKey.OptionsTeamsShuffle),
        permissionGetter = permissionGetter,
    ) { player ->
        teamService.shuffleTeams(teamCount)
        player.sendInfoMessage("Teams have been shuffled!")
    }

    registerEntity(EntityType.TEXT_DISPLAY, position.add(0.0, -(MENU_ITEM_OFFSET + MENU_ITEM_HEIGHT), 0.0)) {
        putString("text", text.serialized(
            text.string(StringKey.OptionsTeamsCount, teamCount)
        ))
        putString("billboard", "fixed")
        putString("alignment", "center")
        putInt("background", 0)
        putBoolean("shadow", true)
    }

    registerEntity(EntityType.TEXT_DISPLAY, position) {
        putString("text", text.serialized(
            text.string(StringKey.OptionsTeams)
        ))
        putString("billboard", "fixed")
        putString("alignment", "center")
        putInt("background", 0)
        putBoolean("shadow", true)
    }
}
