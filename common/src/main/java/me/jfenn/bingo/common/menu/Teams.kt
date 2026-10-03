package me.jfenn.bingo.common.menu

import me.jfenn.bingo.common.options.BingoOptions
import me.jfenn.bingo.common.team.TeamService
import me.jfenn.bingo.common.utils.plus
import me.jfenn.bingo.generated.StringKey
import me.jfenn.bingo.platform.IMinecraftServer
import me.jfenn.bingo.platform.text.Formatting
import org.joml.Vector3d

internal const val MENU_TEAMS_WIDTH = 2.0

internal fun MenuComponent.registerTeams(
    position: Vector3d,
    teamService: TeamService = koinScope.get(),
    server: IMinecraftServer = koinScope.get(),
    options: BingoOptions = koinScope.get(),
) {
    val offsetY = MENU_LINE_PADDING*2 + MENU_LINE_HEIGHT
    registerTitlePanel(
        position = position + Vector3d(0.0, -offsetY, 0.0),
        width = MENU_TEAMS_WIDTH,
        title = text.string(StringKey.OptionsTeams),
    )

    registerToggleButton(
        position = position + Vector3d(0.0, -offsetY - (MENU_LINE_HEIGHT + MENU_LINE_PADDING), 0.0),
        width = MENU_TEAMS_WIDTH,
        height = MENU_LINE_HEIGHT,
        text = text.string(StringKey.OptionsTeamsShuffleAuto),
        tooltip = buildTooltip(StringKey.OptionsTeamsShuffleAutoTooltip),
        toggleProp = propertyRef(options::teamAutoShuffle),
    )

    registerNumberInput(
        position = position + Vector3d(0.0, -offsetY - 2*(MENU_LINE_HEIGHT + MENU_LINE_PADDING), 0.0),
        width = MENU_TEAMS_WIDTH,
        height = MENU_LINE_HEIGHT,
        valueProp = propertyRef(options::teamCount),
        minValueProp = ConstantProperty(1),
        maxValueProp = ConstantProperty(server.maxPlayerCount.coerceAtMost(50)),
        format = { text.teamCount(it) },
    )

    registerTileButton(
        position = position + Vector3d(0.0, -offsetY - 3*(MENU_LINE_HEIGHT + MENU_LINE_PADDING), 0.0),
        width = MENU_TEAMS_WIDTH,
        height = MENU_LINE_HEIGHT,
        text = text.empty(),
        textProp = computedProperty {
            when {
                options.teamAutoShuffle -> text.empty() // if auto-shuffle is on, remove the shuffle button
                else -> text.string(StringKey.OptionsTeamsShuffle)
            }
        },
    ) { player ->
        teamService.shuffleTeams(options.teamCount)
        player.sendMessage(
            text.literal("ℹ  ")
                .append(text.string(StringKey.OptionsTeamsShuffleMessage).formatted(Formatting.ITALIC))
                .formatted(Formatting.AQUA)
        )
    }
}
