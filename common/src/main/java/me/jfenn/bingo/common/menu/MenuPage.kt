package me.jfenn.bingo.common.menu

import me.jfenn.bingo.common.GameService
import me.jfenn.bingo.common.card.BingoTeam
import me.jfenn.bingo.common.card.CardService
import me.jfenn.bingo.common.card.TeamService
import me.jfenn.bingo.common.config.BingoConfig
import me.jfenn.bingo.common.integrations.BingoCardController
import me.jfenn.bingo.common.state.BingoContext
import me.jfenn.bingo.common.utils.sendWarningMessage
import net.minecraft.server.MinecraftServer
import net.minecraft.util.math.Vec3d
import org.koin.core.scope.Scope

fun MenuComponent.registerMenuPage(
    position: Vec3d,
    server: MinecraftServer = koinScope.get(),
    config: BingoConfig = koinScope.get(),
    gameService: GameService = koinScope.get(),
    cardService: CardService = koinScope.get(),
    teamService: TeamService = koinScope.get(),
) {
    var page = 0

    val itemDistributionProp = propertyRef(config::itemDistribution)

    val pages = listOf(
        component(koinScope) {
            registerGameModes(position.add(-3.5, 0.0, 0.0), config)
            registerFeatures(position.add(-1.5, 0.0, 0.0), config)
            registerEnumMenu(
                title = "Win Condition",
                valueProp = propertyRef(config::winCondition),
                position = position.add(0.6, 0.0, 0.0),
            )
            registerTimer(
                title = "Time Limit",
                timerMinutesProp = propertyRef(config::timeLimit),
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
                dimensionProperty = propertyRef(config::spawnDimension),
                server = server,
            )
            registerSpawnKit(
                position = position.add(-1.5, 0.0, 0.0),
                config = config,
            )
            registerTierLists(
                position = position.add(0.6, 0.0, 0.0),
                config = config,
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
                config = config,
            )
            registerCardInfo(
                position = position.add(0.0, 0.0, 0.0),
                config = config,
            )
            registerSpawnDistance(
                position = position.add(3.0, 0.0, 0.0),
                spawnDistanceProp = propertyRef(config::spawnDistance),
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
        isActiveProp = computedProperty { config.isValid() },
    ) { player ->
        if (!config.isValid()) {
            player.sendWarningMessage("Item distribution must add up to 25!")
            return@registerButton
        }

        val isEveryPlayerOnATeam = server.playerManager.playerList
            .all { teamPlayer -> teamService.getPlayerTeam(teamPlayer) != null }

        if (!isEveryPlayerOnATeam && !player.isSneaking) {
            // If there's more than one player, teams need to be set up manually!
            if (server.playerManager.playerList.size > 1) {
                player.sendWarningMessage("Some players have not picked a team! Run '/bingo shuffleteams' or sneak+click to ignore.")
                return@registerButton
            }
            // Otherwise, we can just auto-assign a team
            teamService.joinTeam(player, BingoTeam.TEAM_BLOCKS.values.random())
        }

        gameService.start()
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