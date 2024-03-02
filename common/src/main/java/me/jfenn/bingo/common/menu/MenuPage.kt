package me.jfenn.bingo.common.menu

import me.jfenn.bingo.common.GameService
import me.jfenn.bingo.common.Permission
import me.jfenn.bingo.common.card.CardService
import me.jfenn.bingo.common.config.BingoOptions
import me.jfenn.bingo.common.config.ConfigService
import me.jfenn.bingo.common.utils.sendWarningMessage
import me.jfenn.bingo.integrations.PermissionsIntegration
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.math.Vec3d

fun MenuComponent.registerMenuPage(
    position: Vec3d,
    options: BingoOptions = koinScope.get(),
    gameService: GameService = koinScope.get(),
    cardService: CardService = koinScope.get(),
    permissionsIntegration: PermissionsIntegration = koinScope.get(),
    configService: ConfigService = koinScope.get(),
) {
    var page = 0

    val itemDistributionProp = propertyRef(options::itemDistribution)
    val permissionGetter = { player: ServerPlayerEntity ->
        permissionsIntegration.hasPermission(player, Permission.CONFIGURE_GAME)
    }

    val pages = listOf(
        component(koinScope) {
            registerGameModes(
                position = position.add(-3.5, 0.0, 0.0),
                options = options,
                permissionGetter = permissionGetter,
            )
            registerFeatures(
                position = position.add(-1.5, 0.0, 0.0),
                options = options,
                permissionGetter = permissionGetter,
            )
            registerEnumMenu(
                title = "Goal",
                valueProp = propertyRef(options::goal),
                position = position.add(0.6, 0.0, 0.0),
                permissionGetter = permissionGetter,
            )
            registerTimer(
                title = "Time Limit",
                timerMinutesProp = propertyRef(options::timeLimit),
                position = position.add(2.3, 0.0, 0.0),
                permissionGetter = permissionGetter,
            )
            registerTeams(
                title = "Teams",
                position = position.add(3.6, 0.0, 0.0),
                permissionGetter = permissionGetter,
            )
        },
        component(koinScope) {
            registerDimensionMenu(
                position = position.add(-3.5, 0.0, 0.0),
                dimensionProperty = propertyRef(options::spawnDimension),
                permissionGetter = permissionGetter,
            )
            registerSpawnKit(
                position = position.add(-1.5, 0.0, 0.0),
                options = options,
                permissionGetter = permissionGetter,
            )
            registerTierLists(
                position = position.add(0.6, 0.0, 0.0),
                options = options,
                permissionGetter = permissionGetter,
            )
            registerItemDistribution(
                position = position.add(3.0, 0.0, 0.0),
                itemDistributionProp = itemDistributionProp,
                permissionGetter = permissionGetter,
            )
        },
        component(koinScope) {
            registerScoreInfo(
                position = position.add(-3.0, 0.0, 0.0),
                options = options,
                permissionGetter = permissionGetter,
            )
            registerCardInfo(
                position = position.add(0.0, 0.0, 0.0),
                options = options,
                permissionGetter = permissionGetter,
            )
            registerSpawnDistance(
                position = position.add(3.0, 0.0, 0.0),
                spawnDistanceProp = propertyRef(options::spawnDistance),
                permissionGetter = permissionGetter,
            )
        },
    )

    registerButton(
        position = position.add(-3.7, 0.5, 0.0),
        text = "⟳ Reset Defaults",
        isActiveProp = computedProperty { false },
        permissionGetter = permissionGetter,
    ) {
        // Reset the menu to the default options
        options.copyFrom(configService.optionsDefault)
        cardService.generateCard()
        markDirty()
    }

    registerButton(
        position = position.add(3.7, 0.5, 0.0),
        text = "⚄ Re-roll Card",
        isActiveProp = computedProperty { false },
        permissionGetter = permissionGetter,
    ) {
        // Shuffle the current card + seed
        cardService.shuffleCard()
        markDirty()
    }

    for (i in pages.indices) {
        registerButton(
            position = position.add(-((pages.size-1) * 0.6) + (i*1.2), 0.5, 0.0),
            text = "Page ${i+1}",
            isActiveProp = computedProperty { page == i },
            permissionGetter = permissionGetter,
        ) {
            page = i
            markDirty()
        }
    }

    registerButton(
        position = position.add(5.0, -MENU_ITEM_OFFSET, 0.0),
        text = "START",
        isActiveProp = computedProperty { options.isValid() },
        permissionGetter = permissionGetter,
    ) { player ->
        // Start the game!
        gameService.start(
            sendFeedback = {
                player.sendWarningMessage(it)
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