package me.jfenn.bingo.common.menu

import me.jfenn.bingo.api.IPlayerManager
import me.jfenn.bingo.common.Permission
import me.jfenn.bingo.common.card.CardService
import me.jfenn.bingo.common.config.BingoCardOptions
import me.jfenn.bingo.common.config.BingoOptions
import me.jfenn.bingo.common.config.ConfigService
import me.jfenn.bingo.common.game.GameService
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.common.utils.plus
import me.jfenn.bingo.generated.StringKey
import me.jfenn.bingo.integrations.PermissionsIntegration
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import org.joml.Vector3d
import org.slf4j.Logger

fun MenuComponent.registerMenuPage(
    position: Vector3d,
    state: BingoState = koinScope.get(),
    options: BingoOptions = koinScope.get(),
    gameService: GameService = koinScope.get(),
    cardService: CardService = koinScope.get(),
    permissionsIntegration: PermissionsIntegration = koinScope.get(),
    playerManager: IPlayerManager = koinScope.get(),
    configService: ConfigService = koinScope.get(),
    text: TextProvider = koinScope.get(),
    log: Logger = koinScope.get(),
) {
    var page = 0

    val itemDistributionProp = DelegatedProperty(
        getter = {
            state.getActiveCard().options.itemDistribution
        },
        setter = {
            val card = state.getActiveCard()
            card.options.itemDistribution = it

            // Attempt to re-roll the card, but log if there is an error
            kotlin.runCatching {
                cardService.generateCard(card)
            }
                .exceptionOrNull()
                ?.let { e -> log.error("Error generating card: ${e::class.simpleName} ${e.message}") }

            markDirty()
        }
    )
    val permissionGetter = { player: ServerPlayerEntity ->
        permissionsIntegration.hasPermission(playerManager.forPlayer(player), Permission.CONFIGURE_GAME)
    }

    val pages = listOf(
        component(koinScope) {
            registerGameModes(
                position = position + Vector3d(-3.5, 0.0, 0.0),
                permissionGetter = permissionGetter,
            )
            registerGoal(
                position = position + Vector3d(-1.5, 0.0, 0.0),
                permissionGetter = permissionGetter,
            )
            registerWinCondition(
                position = position + Vector3d(0.3, 0.0, 0.0),
                permissionGetter = permissionGetter,
            )
            registerTimer(
                timerMinutesProp = propertyRef(options::timeLimit),
                position = position + Vector3d(1.8, 0.0, 0.0),
                permissionGetter = permissionGetter,
            )
            registerTeams(
                position = position + Vector3d(3.5, 0.0, 0.0),
                permissionGetter = permissionGetter,
            )
        },
        component(koinScope) {
            registerFeatures(
                position = position + Vector3d(-3.5, 0.0, 0.0),
                options = options,
                permissionGetter = permissionGetter,
            )
            registerSpawnKit(
                position = position + Vector3d(-1.5, 0.0, 0.0),
                options = options,
                permissionGetter = permissionGetter,
            )
            registerItemFilters(
                position = position + Vector3d(0.6, 0.0, 0.0),
                options = options,
                permissionGetter = permissionGetter,
            )
            registerItemDistribution(
                position = position + Vector3d(3.0, 0.0, 0.0),
                itemDistributionProp = itemDistributionProp,
                permissionGetter = permissionGetter,
            )
        },
        component(koinScope) {
            registerScoreInfo(
                position = position + Vector3d(-3.5, 0.0, 0.0),
                options = options,
                permissionGetter = permissionGetter,
            )
            registerWinBehavior(
                position = position + Vector3d(-0.9, 0.0, 0.0),
                permissionGetter = permissionGetter,
            )
            registerDimensionMenu(
                position = position + Vector3d(1.4, 0.0, 0.0),
                dimensionProperty = propertyRef(options::spawnDimension),
                permissionGetter = permissionGetter,
            )
            registerSpawnDistance(
                position = position + Vector3d(3.5, 0.0, 0.0),
                spawnDistanceProp = propertyRef(options::spawnDistance),
                permissionGetter = permissionGetter,
            )
        },
    )

    registerButton(
        position = position + Vector3d(-3.7, 0.5, 0.0),
        text = Text.literal("⟳ ")
            .append(text.string(StringKey.OptionsResetDefaults)),
        isActiveProp = computedProperty { false },
        permissionGetter = permissionGetter,
    ) {
        // Reset the menu to the default options
        options.copyFrom(configService.optionsDefault)

        // Reset the active card options to the defaults
        val cards = options.cards
            .ifEmpty { listOf(BingoCardOptions()) }
            .mapIndexed { i, options ->
                state.cards.getOrNull(i)
                    ?.let { cardService.generateCard(it, options) }
                    ?: cardService.newCard(options)
            }

        // Clear & re-index all cards
        state.replaceCards(cards)

        markDirty()
    }

    registerButton(
        position = position + Vector3d(3.7, 0.5, 0.0),
        text = Text.literal("⚄ ")
            .append(text.string(StringKey.OptionsRerollCard)),
        isActiveProp = computedProperty { false },
        permissionGetter = permissionGetter,
    ) {
        // Shuffle the current card + seed
        cardService.shuffleCard(state.getActiveCard())
        markDirty()
    }

    for (i in pages.indices) {
        registerButton(
            position = position + Vector3d(-((pages.size-1) * 0.6) + (i*1.2), 0.5, 0.0),
            text = text.string(StringKey.OptionsPage, i+1),
            isActiveProp = computedProperty { page == i },
            permissionGetter = permissionGetter,
        ) {
            page = i
            markDirty()
        }
    }

    registerButton(
        position = position + Vector3d(5.0, -MENU_ITEM_OFFSET, 0.0),
        text = Text.literal("✔ ").append(text.string(StringKey.OptionsStart)),
        isActiveProp = computedProperty { options.isValid() },
        permissionGetter = permissionGetter,
    ) { player ->
        // Start the game!
        gameService.start(
            sendFeedback = { feedback ->
                Text.literal("⚠  ")
                    .append(feedback.copy().formatted(Formatting.ITALIC))
                    .formatted(Formatting.YELLOW)
                    .also { player.sendMessage(it) }
            },
            allowSpectators = player.isSneaking,
        )
    }

    onTick { instance ->
        for (i in pages.indices) {
            if (page == i) {
                pages[i].tick(instance)
            }
        }
    }

    onUpdate { instance ->
        for (i in pages.indices) {
            if (page == i) {
                pages[i].spawn(instance)
            } else {
                pages[i].despawn()
            }
        }
    }

    onDespawn {
        pages.forEach { it.despawn() }
    }
}