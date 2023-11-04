package me.jfenn.bingo.menu

import me.jfenn.bingo.BingoGame
import me.jfenn.bingo.card.BingoTeam
import me.jfenn.bingo.config.BingoConfig
import me.jfenn.bingo.minecraftServer
import me.jfenn.bingo.utils.sendWarningMessage
import net.minecraft.util.math.Vec3d

fun MenuComponent.registerMenuPage(
    position: Vec3d,
    config: BingoConfig,
) {

    var page by property(MutableProperty<Int>(1))

    val itemDistributionProp = property(MutableProperty.ref(config::itemDistribution))

    val page1 = component {
        registerEnumMenu(
            title = "Game Mode",
            valueProp = MutableProperty.ref(config::gameMode),
            position = position.add(-3.0, 0.0, 0.0),
        )
        registerFeatures(position.add(-1.0, 0.0, 0.0), config)
        registerEnumMenu(
            title = "Win Condition",
            valueProp = MutableProperty.ref(config::winCondition),
            position = position.add(1.2, 0.0, 0.0),
        )
        registerTimer(
            title = "Time Limit",
            timerMinutesProp = MutableProperty.ref(config::timerMinutes),
            position = position.add(3.0, 0.0, 0.0),
        )
    }

    val page2 = component {
        registerDimensionMenu(
            position = position.add(-3.0, 0.0, 0.0),
            dimensionProperty = MutableProperty.ref(config::spawnDimension),
        )
        registerSpawnKit(
            position = position.add(-1.0, 0.0, 0.0),
            config = config,
        )
        registerTeams(
            title = "Teams",
            position = position.add(0.7, 0.0, 0.0),
        )
        registerItemDistribution(
            position = position.add(2.7, 0.0, 0.0),
            itemDistributionProp = itemDistributionProp,
        )
    }

    registerButton(
        position = position.add(-1.0, 0.5, 0.0),
        text = "Page 1",
        isActiveProp = computedProperty { page == 1 },
    ) {
        page = 1
    }

    registerButton(
        position = position.add(1.0, 0.5, 0.0),
        text = "Page 2",
        isActiveProp = computedProperty { page == 2 },
    ) {
        page = 2
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

        val isEveryPlayerOnATeam = minecraftServer.playerManager.playerList
            .all { player -> BingoTeam.values().any { team -> player.isTeamPlayer(team.team()) } }

        if (!isEveryPlayerOnATeam && !player.isSneaking) {
            player.sendWarningMessage("Some players have not picked a team! Run /shuffleteams or sneak+click to ignore.")
            return@registerButton
        }

        BingoGame.start()
    }

    onUpdate {
        when (page) {
            1 -> {
                page1.spawn(it)
                page2.despawn()
            }
            2 -> {
                page1.despawn()
                page2.spawn(it)
            }
        }
    }

    onDespawn {
        page1.despawn()
        page2.despawn()
    }
}