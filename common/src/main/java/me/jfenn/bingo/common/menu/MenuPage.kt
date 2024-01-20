package me.jfenn.bingo.common.menu

import me.jfenn.bingo.common.GameService
import me.jfenn.bingo.common.card.CardService
import me.jfenn.bingo.common.config.BingoOptions
import me.jfenn.bingo.common.utils.sendWarningMessage
import net.minecraft.server.MinecraftServer
import net.minecraft.util.math.Vec3d

fun MenuComponent.registerMenuPage(
    position: Vec3d,
    server: MinecraftServer = koinScope.get(),
    options: BingoOptions = koinScope.get(),
    gameService: GameService = koinScope.get(),
    cardService: CardService = koinScope.get(),
) {
    var page = 0

    val itemDistributionProp = propertyRef(options::itemDistribution)

    val pages = listOf(
        component(koinScope) {
            registerGameModes(position.add(-3.5, 0.0, 0.0), options)
            registerFeatures(position.add(-1.5, 0.0, 0.0), options)
            registerEnumMenu(
                title = "Win Condition",
                valueProp = propertyRef(options::winCondition),
                position = position.add(0.6, 0.0, 0.0),
            )
            registerTimer(
                title = "Time Limit",
                timerMinutesProp = propertyRef(options::timeLimit),
                position = position.add(2.3, 0.0, 0.0),
            )
            registerTeams(
                title = "Teams",
                position = position.add(3.6, 0.0, 0.0),
            )
        },
        component(koinScope) {
            registerDimensionMenu(
                position = position.add(-3.5, 0.0, 0.0),
                dimensionProperty = propertyRef(options::spawnDimension),
                server = server,
            )
            registerSpawnKit(
                position = position.add(-1.5, 0.0, 0.0),
                options = options,
            )
            registerTierLists(
                position = position.add(0.6, 0.0, 0.0),
                options = options,
            ) {
                // when changed, re-roll the bingo card
                cardService.generateCard()
            }
            registerItemDistribution(
                position = position.add(3.0, 0.0, 0.0),
                itemDistributionProp = itemDistributionProp,
            ) {
                // when changed, re-roll the bingo card
                cardService.generateCard()
            }
        },
        component(koinScope) {
            registerScoreInfo(
                position = position.add(-3.0, 0.0, 0.0),
                options = options,
            )
            registerCardInfo(
                position = position.add(0.0, 0.0, 0.0),
                options = options,
            )
            registerSpawnDistance(
                position = position.add(3.0, 0.0, 0.0),
                spawnDistanceProp = propertyRef(options::spawnDistance),
            )
        },
    )

    for (i in pages.indices) {
        registerButton(
            position = position.add(-((pages.size-1) * 0.7) + (i*1.4), 0.5, 0.0),
            text = "Page ${i+1}",
            isActiveProp = computedProperty { page == i },
        ) {
            page = i
            markDirty()
        }
    }

    registerButton(
        position = position.add(5.0, -MENU_ITEM_OFFSET, 0.0),
        text = "START",
        isActiveProp = computedProperty { options.isValid() },
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