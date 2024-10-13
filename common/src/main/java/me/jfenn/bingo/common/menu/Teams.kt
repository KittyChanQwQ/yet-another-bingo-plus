package me.jfenn.bingo.common.menu

import me.jfenn.bingo.api.EntityType
import me.jfenn.bingo.api.ITextDisplayEntity
import me.jfenn.bingo.common.team.BingoTeam
import me.jfenn.bingo.common.team.TeamService
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.common.utils.plus
import me.jfenn.bingo.generated.StringKey
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import org.joml.Matrix4f
import org.joml.Vector3d

object TeamCountState {
    var teamCount: Int = 3
}

fun MenuComponent.registerTeams(
    position: Vector3d,
    permissionGetter: (ServerPlayerEntity) -> Boolean,
    teamService: TeamService = koinScope.get(),
    text: TextProvider = koinScope.get(),
) {
    registerNumberInput(
        position = position,
        valueProp = propertyRef(TeamCountState::teamCount),
        minValueProp = ConstantProperty(1),
        maxValueProp = ConstantProperty(BingoTeam.TEAM_BLOCKS.size),
        format = { text.string(StringKey.OptionsTeamsCount, it) },
        permissionGetter = permissionGetter,
    )

    registerButton(
        position = position + Vector3d(0.0, -(MENU_ITEM_OFFSET + 3 * MENU_ITEM_HEIGHT), 0.0),
        text = text.string(StringKey.OptionsTeamsShuffle),
        permissionGetter = permissionGetter,
    ) { player ->
        teamService.shuffleTeams(TeamCountState.teamCount)
        player.sendMessage(
            Text.literal("ℹ  ")
                .append(text.string(StringKey.OptionsTeamsShuffleMessage).formatted(Formatting.ITALIC))
                .formatted(Formatting.AQUA)
        )
    }

    registerEntity(EntityType.TEXT_DISPLAY) {
        pos = position
        this.value = text.string(StringKey.OptionsTeams)
        billboard = ITextDisplayEntity.Billboard.FIXED
        alignment = ITextDisplayEntity.TextAlignment.CENTER
        background = 0
        shadow = true
        transformation = Matrix4f().scale(MENU_TEXT_SCALE)
    }
}
