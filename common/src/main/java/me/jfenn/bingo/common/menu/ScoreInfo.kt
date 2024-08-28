package me.jfenn.bingo.common.menu

import me.jfenn.bingo.api.EntityType
import me.jfenn.bingo.api.ITextDisplayEntity
import me.jfenn.bingo.common.config.BingoOptions
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.common.utils.plus
import me.jfenn.bingo.generated.StringKey
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.Formatting
import org.joml.Matrix4f
import org.joml.Vector3d

fun MenuComponent.registerScoreInfo(
    position: Vector3d,
    options: BingoOptions,
    permissionGetter: (ServerPlayerEntity) -> Boolean,
    text: TextProvider = koinScope.get(),
) {
    registerEntity(EntityType.TEXT_DISPLAY) {
        pos = position
        this.value = text.string(StringKey.OptionsScoreInfo)
        billboard = ITextDisplayEntity.Billboard.FIXED
        alignment = ITextDisplayEntity.TextAlignment.CENTER
        background = 0
        shadow = true
        transformation = Matrix4f().scale(MENU_TEXT_SCALE)
    }

    registerToggleButton(
        position = position + Vector3d(0.0, -(MENU_ITEM_OFFSET), 0.0),
        text = text.string(StringKey.OptionsRemainingTime),
        tooltip = buildList {
            add(text.string(StringKey.OptionsRemainingTime).formatted(Formatting.GREEN))
            add(text.string(StringKey.OptionsRemainingTimeTooltip))
        },
        toggleProp = propertyRef(options::showRemainingTime),
        permissionGetter = permissionGetter,
    )

    registerToggleButton(
        position = position + Vector3d(0.0, -(MENU_ITEM_OFFSET + MENU_ITEM_HEIGHT), 0.0),
        text = text.string(StringKey.OptionsCompletedItems),
        tooltip = buildList {
            add(text.string(StringKey.OptionsCompletedItems).formatted(Formatting.GREEN))
            add(text.string(StringKey.OptionsCompletedItemsTooltip))
        },
        toggleProp = propertyRef(options::showCompletedItems),
        permissionGetter = permissionGetter,
    )

    registerToggleButton(
        position = position + Vector3d(0.0, -(MENU_ITEM_OFFSET + 2*MENU_ITEM_HEIGHT), 0.0),
        text = text.string(StringKey.OptionsCompletedLines),
        tooltip = buildList {
            add(text.string(StringKey.OptionsCompletedLines).formatted(Formatting.GREEN))
            add(text.string(StringKey.OptionsCompletedLinesTooltip))
        },
        toggleProp = propertyRef(options::showCompletedLines),
        permissionGetter = permissionGetter,
    )

    registerToggleButton(
        position = position + Vector3d(0.0, -(MENU_ITEM_OFFSET + 3*MENU_ITEM_HEIGHT), 0.0),
        text = text.string(StringKey.OptionsLeadingTeam),
        tooltip = buildList {
            add(text.string(StringKey.OptionsLeadingTeam).formatted(Formatting.GREEN))
            add(text.string(StringKey.OptionsLeadingTeamTooltip))
        },
        toggleProp = propertyRef(options::showLeadingTeam),
        permissionGetter = permissionGetter,
    )
}