package me.jfenn.bingo.common.menu

import me.jfenn.bingo.common.controller.GameRuleController
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.common.utils.plus
import me.jfenn.bingo.generated.StringKey
import net.minecraft.server.MinecraftServer
import org.joml.Vector3d

private const val TILE_HEIGHT = 0.6
private const val TILE_WIDTH = 1.2
private const val TILE_MARGIN = 0.1
internal const val MENU_FEATURES_WIDTH = TILE_WIDTH + TILE_MARGIN + TILE_WIDTH

internal fun MenuComponent.registerFeatures(
    position: Vector3d,
    state: BingoState = koinScope.get(),
    gameRuleController: GameRuleController = koinScope.get(),
    server: MinecraftServer = koinScope.get(),
    text: TextProvider = koinScope.get(),
) {
    val options by state::options

    registerTitlePanel(
        position = position + Vector3d(0.0, -0.5, 0.0),
        width = MENU_FEATURES_WIDTH,
        title = text.string(StringKey.OptionsFeatures),
    )

    val offsetY = 0.6
    val offsetX = (TILE_WIDTH + TILE_MARGIN)/2.0

    registerIconButton(
        position = position + Vector3d(-offsetX, -(offsetY + TILE_HEIGHT), 0.0),
        width = TILE_WIDTH,
        height = TILE_HEIGHT,
        icon = "᎒᎒᎒",
        text = text.string(StringKey.OptionsPreviewCard),
        tooltip = buildTooltip(StringKey.OptionsPreviewCard),
        isActiveProp = computedProperty { options.showPreviewCard },
    ) {
        options.showPreviewCard = !options.showPreviewCard
    }

    registerIconButton(
        position = position + Vector3d(offsetX, -(offsetY + TILE_HEIGHT), 0.0),
        width = TILE_WIDTH,
        height = TILE_HEIGHT,
        icon = "\uD83C\uDF92",
        text = text.string(StringKey.OptionsKeepInventory),
        isActiveProp = computedProperty { options.isKeepInventory },
    ) {
        options.isKeepInventory = !options.isKeepInventory
        gameRuleController.writeToServer(server, options)
    }

    registerIconButton(
        position = position + Vector3d(-offsetX, -(offsetY + 2*TILE_HEIGHT + TILE_MARGIN), 0.0),
        width = TILE_WIDTH,
        height = TILE_HEIGHT,
        icon = "\uD83E\uDD87",
        text = text.string(StringKey.OptionsElytra),
        isActiveProp = computedProperty { options.isElytra },
    ) {
        options.isElytra = !options.isElytra
    }

    registerIconButton(
        position = position + Vector3d(offsetX, -(offsetY + 2*TILE_HEIGHT + TILE_MARGIN), 0.0),
        width = TILE_WIDTH,
        height = TILE_HEIGHT,
        icon = "\uD83D\uDC41",
        text = text.string(StringKey.OptionsNightVis),
        isActiveProp = computedProperty { options.isNightVision },
    ) {
        options.isNightVision = !options.isNightVision
    }

    registerIconButton(
        position = position + Vector3d(-offsetX, -(offsetY + 3*TILE_HEIGHT + 2*TILE_MARGIN), 0.0),
        width = TILE_WIDTH,
        height = TILE_HEIGHT,
        icon = "\uD83D\uDDE1",
        text = text.string(StringKey.OptionsAllowPvp),
        isActiveProp = computedProperty { options.isPvpEnabled },
    ) {
        options.isPvpEnabled = !options.isPvpEnabled
        gameRuleController.writeToServer(server, options)
    }

//    registerIconButton(
//        position = position + Vector3d(offsetX, -(offsetY + 3*TILE_HEIGHT + 2*TILE_MARGIN), 0.0),
//        width = TILE_WIDTH,
//        height = TILE_HEIGHT,
//        icon = "❓",
//        text = text.string(StringKey.OptionsElytra),
//        isActiveProp = computedProperty { options.isElytra },
//    ) {
//        options.isElytra = !options.isElytra
//    }
}
