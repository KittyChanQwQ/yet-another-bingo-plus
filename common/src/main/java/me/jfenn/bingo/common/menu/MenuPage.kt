package me.jfenn.bingo.common.menu

import me.jfenn.bingo.common.BingoGame
import me.jfenn.bingo.common.card.BingoTeam
import me.jfenn.bingo.common.card.TeamService
import me.jfenn.bingo.common.integrations.BingoCardController
import me.jfenn.bingo.common.state.BingoContext
import me.jfenn.bingo.common.utils.sendWarningMessage
import net.minecraft.util.math.Vec3d

fun MenuComponent.registerMenuPage(
    position: Vec3d,
    ctx: BingoContext,
) {

    var page = 0

    val itemDistributionProp = propertyRef(ctx.config::itemDistribution)

    val pages = listOf(
        component {
            registerGameModes(position.add(-3.5, 0.0, 0.0), ctx.config)
            registerFeatures(position.add(-1.5, 0.0, 0.0), ctx.config)
            registerEnumMenu(
                title = "Win Condition",
                valueProp = propertyRef(ctx.config::winCondition),
                position = position.add(0.6, 0.0, 0.0),
            )
            registerTimer(
                title = "Time Limit",
                timerMinutesProp = propertyRef(ctx.config::timeLimit),
                position = position.add(2.3, 0.0, 0.0),
            )
            registerTeams(
                title = "Teams",
                position = position.add(3.6, 0.0, 0.0),
            )
        },
        component {
            registerDimensionMenu(
                position = position.add(-3.5, 0.0, 0.0),
                dimensionProperty = propertyRef(ctx.config::spawnDimension),
                server = ctx.server,
            )
            registerSpawnKit(
                position = position.add(-1.5, 0.0, 0.0),
                config = ctx.config,
            )
            registerTierLists(
                position = position.add(0.6, 0.0, 0.0),
                config = ctx.config,
            ) {
                // when changed, re-roll the bingo card
                BingoCardController.generateCard(ctx)
            }
            registerItemDistribution(
                position = position.add(3.0, 0.0, 0.0),
                itemDistributionProp = itemDistributionProp,
            ) {
                // when changed, re-roll the bingo card
                BingoCardController.generateCard(ctx)
            }
        },
        component {
            registerScoreInfo(
                position = position.add(-3.0, 0.0, 0.0),
                config = ctx.config,
            )
            registerCardInfo(
                position = position.add(0.0, 0.0, 0.0),
                ctx = ctx,
            )
            registerSpawnDistance(
                position = position.add(3.0, 0.0, 0.0),
                spawnDistanceProp = propertyRef(ctx.config::spawnDistance),
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
        isActiveProp = computedProperty { ctx.config.isValid() },
    ) { player ->
        if (!ctx.config.isValid()) {
            player.sendWarningMessage("Item distribution must add up to 25!")
            return@registerButton
        }

        val isEveryPlayerOnATeam = ctx.server.playerManager.playerList
            .all { teamPlayer -> BingoTeam.fromPlayer(teamPlayer) != null }

        if (!isEveryPlayerOnATeam && !player.isSneaking) {
            // If there's more than one player, teams need to be set up manually!
            if (ctx.server.playerManager.playerList.size > 1) {
                player.sendWarningMessage("Some players have not picked a team! Run '/bingo shuffleteams' or sneak+click to ignore.")
                return@registerButton
            }
            // Otherwise, we can just auto-assign a team
            TeamService.joinTeam(player, BingoTeam.TEAM_BLOCKS.values.random())
        }

        BingoGame.start(ctx)
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