package me.jfenn.bingo.common.config

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames
import me.jfenn.bingo.common.spawn.SpawnItem
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

@Serializable
data class BingoConfig(
    var overwrite: Boolean = true,
    var winCondition: WinCondition = WinCondition.THREE_LINES,
    var timeLimit: Duration? = 60.minutes,
    var itemDistribution: List<Int> = listOf(2, 6, 9, 6, 2),
    var tierLists: List<String> = listOf("overworld", "nether"),

    // game mode settings
    var isLockoutMode: Boolean = false,
    var isInventoryMode: Boolean = false,
    var isRankedMode: Boolean = false,

    // game feature settings
    var isElytra: Boolean = false,
    var isUnlockRecipes: Boolean = true,
    var isPvpEnabled: Boolean = true,
    var isKeepInventory: Boolean = false,

    // team spawnpoint settings
    var spawnDimension: String = "minecraft:overworld",
    @OptIn(ExperimentalSerializationApi::class)
    @JsonNames("spreadPlayersDistance")
    var spawnDistance: Int = 100,

    // score info settings
    var showRemainingTime: Boolean = true,
    var showCompletedItems: Boolean = true,
    var showCompletedLines: Boolean = true,
    var showLeadingTeam: Boolean = true,

    // bingo card settings
    @OptIn(ExperimentalSerializationApi::class)
    @JsonNames("drawOtherTeamCompletions")
    var showOtherTeamCompletions: Boolean = false,
    var hiddenItems: Boolean = false,
    var showPreviewCard: Boolean = false,

    // adds BINGO_IGNORE to spawn kit items, preventing them from being scored
    var preventScoringSpawnKitItems: Boolean = false,
    // excludes the active spawn kit items from item tiers when generating a card
    var excludeSpawnKitItemsFromCards: Boolean = true,
    var refillPlayerKitOnRespawn: Boolean = true,
    var isPlayerKit: Boolean = false,
    var playerKitItems: List<SpawnItem> = listOf(
        SpawnItem("minecraft:iron_chestplate"),
        SpawnItem("minecraft:iron_pickaxe"),
        SpawnItem("minecraft:iron_axe"),
        SpawnItem("minecraft:shield"),
        SpawnItem("minecraft:torch", count = 64),
    ),

    var isTeamKit: Boolean = false,
    var teamKitItems: List<SpawnItem> = listOf(
        SpawnItem("minecraft:iron_ingot", count = 64),
        SpawnItem("minecraft:oak_log", count = 64),
        SpawnItem("minecraft:torch", count = 64),
    ),

    var autoShutDownInPostgame: Boolean = true,
    var unsafeSkipWorldClose: Boolean = false,
) {

    fun isValid(): Boolean {
        return itemDistribution.sum() <= 25
    }

    fun formatGameMode(): String {
        val gameMode = buildList {
            if (isLockoutMode) add("Lockout")
            if (isInventoryMode) add("Inventory")
            if (isRankedMode) add("Ranked")
        }

        return gameMode.joinToString(", ").ifEmpty { "Standard" }
    }

    fun formatFeatures(): String {
        val features = buildList {
            if (isElytra) add("Elytra")
            if (isKeepInventory) add("KeepInv")
            if (isPvpEnabled) add("PvP")
        }

        return features.joinToString(", ").ifEmpty { "None" }
    }

}