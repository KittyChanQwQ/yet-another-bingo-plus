package me.jfenn.bingo.common.menu

import me.jfenn.bingo.common.commands.formatWarning
import me.jfenn.bingo.common.game.GameService
import me.jfenn.bingo.common.options.OptionsService
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.utils.plus
import me.jfenn.bingo.generated.StringKey
import me.jfenn.bingo.platform.EntityType
import me.jfenn.bingo.platform.IDisplayEntity
import me.jfenn.bingo.platform.ITextDisplayEntity
import org.joml.Matrix4f
import org.joml.Vector3d
import org.koin.core.scope.Scope

private fun createRootPage(
    koinScope: Scope,
    position: Vector3d,
    optionsService: OptionsService = koinScope.get(),
    gameService: GameService = koinScope.get(),
    state: BingoState = koinScope.get(),
) = component(koinScope) {
    val pos = position + Vector3d(-5.5 + MENU_LINE_PADDING*2, 0.0, 0.0)
    registerGameModes(
        position = pos + Vector3d(GAME_MODES_WIDTH/2, 0.0, 0.0),
    )
    pos.add(GAME_MODES_WIDTH + MENU_LINE_PADDING, 0.0, 0.0)

    registerGoal(
        position = pos + Vector3d(MENU_GOAL_WIDTH/2, 0.0, 0.0),
    )
    pos.add(MENU_GOAL_WIDTH + MENU_LINE_PADDING, 0.0, 0.0)

    registerCardDifficulty(
        position = pos + Vector3d(CARD_DIFFICULTY_WIDTH/2, 0.0, 0.0),
    )
    pos.add(CARD_DIFFICULTY_WIDTH + MENU_LINE_PADDING, 0.0, 0.0)

    registerTeams(
        position = pos + Vector3d(MENU_TEAMS_WIDTH/2, 0.0, 0.0),
    )
    registerCardReroll(
        position = pos + Vector3d(MENU_TEAMS_WIDTH/2, -0.5 - 3*MENU_LINE_HEIGHT - 4*MENU_LINE_PADDING - 0.8, 0.0),
        width = MENU_TEAMS_WIDTH,
        height = 0.8,
    )
    pos.add(MENU_TEAMS_WIDTH + MENU_LINE_PADDING, 0.0, 0.0)

    val startWidth = (position.x + 5.5 - MENU_LINE_PADDING*2) - pos.x

    registerPlayerCount(
        position = pos + Vector3d(startWidth/2, 0.0, 0.0),
        width = startWidth,
    )

    val startButtonHeight = 2.0 - MENU_PLAYER_COUNT_HEIGHT + 2*MENU_LINE_PADDING
    registerTileButton(
        position = pos + Vector3d(startWidth/2, -2.0 - 2*MENU_LINE_PADDING, 0.0),
        width = startWidth,
        height = startButtonHeight,
        text = text.string(StringKey.OptionsStart),
        brightness = MENU_BRIGHTNESS_ALT,
    ) { player ->
        // Start the game!
        optionsService.broadcastHotbarMessage(player, text.string(StringKey.CommandStartSuccess))
        gameService.start(
            allowSpectators = player.isSneaking,
        )
    }

    registerTileButton(
        position = pos + Vector3d(startWidth/2, -1.0 - 4*MENU_LINE_PADDING - startButtonHeight, 0.0),
        width = startWidth,
        height = MENU_LINE_HEIGHT,
        icon = "⟳",
        text = text.string(StringKey.OptionsResetDefaults),
        brightness = MENU_BRIGHTNESS_ALT,
        tooltip = buildTooltip(StringKey.OptionsResetDefaultsTooltip)
    ) {
        state.menu = MenuState.ResetPrompt
        markDirty()
    }
}

private const val MENU_BACK_WIDTH = 0.8

private fun MenuComponent.registerBackButton(
    position: Vector3d,
    state: BingoState = koinScope.get(),
) {
    registerTileButton(
        position = position + Vector3d(0.0, -2.4 - 2*MENU_LINE_PADDING, 0.0),
        width = MENU_BACK_WIDTH,
        height = 2.4,
        text = text.literal("⬅"),
        textScale = 3.5f,
        brightness = MENU_BRIGHTNESS_ALT,
    ) {
        state.menu = MenuState.Root
    }
}

private fun createFeaturesPage(
    koinScope: Scope,
    position: Vector3d,
) = component(koinScope) {
    val pos = position + Vector3d(-5.5 + MENU_LINE_PADDING*2, 0.0, 0.0)
    registerBackButton(
        position = pos + Vector3d(MENU_BACK_WIDTH/2, 0.0, 0.0),
    )
    pos.add(MENU_BACK_WIDTH + MENU_LINE_PADDING, 0.0, 0.0)

    registerFeatures(
        position = pos + Vector3d(MENU_FEATURES_WIDTH/2, 0.0, 0.0),
    )
    pos.add(MENU_FEATURES_WIDTH + MENU_LINE_PADDING, 0.0, 0.0)

    registerSpawnKit(
        position = pos + Vector3d(MENU_SPAWN_KIT_WIDTH/2, 0.0, 0.0),
    )
    pos.add(MENU_SPAWN_KIT_WIDTH + MENU_LINE_PADDING, 0.0, 0.0)

    registerDimensionMenu(
        position = pos + Vector3d(MENU_DIMENSION_WIDTH/2, 0.0, 0.0),
        height = 2.6,
    )
    pos.add(MENU_DIMENSION_WIDTH + MENU_LINE_PADDING, 0.0, 0.0)

    registerSpawnDistance(
        position = pos + Vector3d(MENU_SPAWN_DISTANCE_WIDTH/2, 0.0, 0.0),
    )
    pos.add(MENU_SPAWN_DISTANCE_WIDTH + MENU_LINE_PADDING, 0.0, 0.0)
}

private fun createGoalPage(
    koinScope: Scope,
    position: Vector3d,
) = component(koinScope) {
    val pos = position + Vector3d(-5.5 + MENU_LINE_PADDING*2, 0.0, 0.0)
    registerBackButton(
        position = pos + Vector3d(MENU_BACK_WIDTH/2, 0.0, 0.0),
    )
    pos.add(MENU_BACK_WIDTH + MENU_LINE_PADDING, 0.0, 0.0)

    registerWinCondition(
        position = pos + Vector3d(MENU_GOAL_WIDTH/2, -MENU_LINE_PADDING, 0.0),
        width = MENU_GOAL_WIDTH,
    )
    pos.add(MENU_GOAL_WIDTH + MENU_LINE_PADDING, 0.0, 0.0)

    registerEndGameWhen(
        position = pos + Vector3d(MENU_END_WHEN_WIDTH/2, -MENU_LINE_PADDING, 0.0),
    )
    pos.add(MENU_END_WHEN_WIDTH + MENU_LINE_PADDING, 0.0, 0.0)

    registerStalemateBehavior(
        position = pos + Vector3d(MENU_STALEMATE_WIDTH/2, -MENU_LINE_PADDING, 0.0),
    )
    pos.add(MENU_STALEMATE_WIDTH + MENU_LINE_PADDING, 0.0, 0.0)

    registerScoreInfo(
        position = pos + Vector3d(MENU_SCORE_INFO_WIDTH/2, -MENU_LINE_PADDING, 0.0),
    )
    pos.add(MENU_SCORE_INFO_WIDTH + MENU_LINE_PADDING, 0.0, 0.0)
}

private fun createItemsPage(
    koinScope: Scope,
    position: Vector3d,
) = component(koinScope) {
    val pos = position + Vector3d(-5.5 + MENU_LINE_PADDING*2, 0.0, 0.0)
    registerBackButton(
        position = pos + Vector3d(MENU_BACK_WIDTH/2, 0.0, 0.0),
    )
    pos.add(MENU_BACK_WIDTH + MENU_LINE_PADDING, 0.0, 0.0)

    registerItemFilters(
        position = pos + Vector3d(MENU_ITEM_FILTERS_WIDTH/2, 0.0, 0.0),
        height = 2.6,
    )
    pos.add(MENU_ITEM_FILTERS_WIDTH + MENU_LINE_PADDING, 0.0, 0.0)

    registerItemDistribution(
        position = pos + Vector3d(0.0, -MENU_LINE_PADDING, 0.0),
    )
    pos.add(MENU_ITEM_DISTRIBUTION_WIDTH + MENU_LINE_PADDING, 0.0, 0.0)
}

private fun createWarningPage(
    koinScope: Scope,
    position: Vector3d,
    state: BingoState = koinScope.get(),
    optionsService: OptionsService = koinScope.get(),
    gameService: GameService = koinScope.get(),
) = component(koinScope) {
    registerEntity(EntityType.BLOCK_DISPLAY) {
        pos = position + Vector3d(-2.0, -2.1, -0.051)
        blockIdentifier = MENU_BUTTON_OFF_MATERIAL
        brightness = MENU_BRIGHTNESS_ALT
        transformation = Matrix4f().scale(4f, 2f, 0.05f)
    }

    registerEntity(EntityType.TEXT_DISPLAY) {
        this.pos = position + Vector3d(0.0, -0.5, 0.0)
        this.value = text.formatWarning(text.string(StringKey.OptionsWarning))

        billboard = ITextDisplayEntity.Billboard.FIXED
        alignment = ITextDisplayEntity.TextAlignment.LEFT
        background = 0
        transformation = Matrix4f().scale(MENU_TEXT_SCALE)
    }

    registerEntity(EntityType.TEXT_DISPLAY) {
        this.pos = position + Vector3d(0.0, -1.7, 0.0)
        this.value = when (val menu = state.menu) {
            is MenuState.Warnings -> text.joinText(menu.warnings, text.literal("\n\n"))
                .apply { setColor(0x999955) }
            is MenuState.ResetPrompt -> text.string(StringKey.OptionsResetDefaultsWarning)
                .apply { setColor(0x999955) }
            else -> text.empty()
        }

        billboard = ITextDisplayEntity.Billboard.FIXED
        alignment = ITextDisplayEntity.TextAlignment.LEFT
        background = 0
        transformation = Matrix4f().scale(MENU_TEXT_SCALE)
    }

    registerTileButton(
        position = position + Vector3d(-0.1 - (1.9/2), -2.55, 0.0),
        width = 1.9,
        height = MENU_LINE_HEIGHT,
        text = text.string(StringKey.GuiClose),
        brightness = MENU_BRIGHTNESS_ALT,
    ) {
        state.menu = MenuState.Root
    }

    registerTileButton(
        position = position + Vector3d(0.1 + (1.9/2), -2.55, 0.0),
        width = 1.9,
        height = MENU_LINE_HEIGHT,
        text = text.empty(),
        textProp = computedProperty {
            when (state.menu) {
                is MenuState.Warnings -> text.string(StringKey.OptionsWarningStartAnyway)
                is MenuState.ResetPrompt -> text.string(StringKey.OptionsResetDefaults)
                else -> text.empty()
            }.apply { setColor(0xff9999) }
        },
        brightness = IDisplayEntity.Brightness(0, 3),
        blockIdentifier = MENU_DANGER_MATERIAL,
    ) { player ->
        when (state.menu) {
            is MenuState.Warnings -> {
                // Start the game!
                optionsService.broadcastHotbarMessage(player, text.string(StringKey.CommandStartSuccess))
                gameService.start(
                    ignoreWarnings = true,
                    allowSpectators = player.isSneaking,
                )
            }
            is MenuState.ResetPrompt -> {
                optionsService.resetToDefaults()
                state.menu = MenuState.Root
            }
            else -> {}
        }
    }
}

internal fun MenuComponent.registerMenuPage(
    position: Vector3d,
    state: BingoState = koinScope.get(),
) {
    val menuState by state::menu

    val pages = MenuPage.entries.associateWith {
        when (it) {
            MenuPage.ROOT -> createRootPage(koinScope, position)
            MenuPage.FEATURES -> createFeaturesPage(koinScope, position)
            MenuPage.GOAL -> createGoalPage(koinScope, position)
            MenuPage.ITEMS -> createItemsPage(koinScope, position)
            MenuPage.PROMPT -> createWarningPage(koinScope, position)
        }
    }

    onTick { instance ->
        pages[menuState.page]?.tick(instance)
    }

    onUpdate { instance ->
        for ((page, component) in pages.entries) {
            if (menuState.page != page) {
                component.despawn()
            }
        }
        pages[menuState.page]?.spawn(instance)
    }

    onDespawn {
        pages.values.forEach { it.despawn() }
    }
}