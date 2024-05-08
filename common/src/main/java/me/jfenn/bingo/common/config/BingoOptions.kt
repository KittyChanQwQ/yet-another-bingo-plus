package me.jfenn.bingo.common.config

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames
import me.jfenn.bingo.common.spawn.SpawnItem
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.full.memberProperties
import kotlin.time.Duration

@Serializable
@OptIn(ExperimentalSerializationApi::class)
data class BingoOptions(
    @JsonNames("winCondition")
    var goal: WinCondition = WinCondition.THREE_LINES,
    var timeLimit: Duration? = null,
    var itemDistribution: List<Int> = listOf(0, 5, 7, 7, 6),
    var tierList: String = "all_items",
    var tierListPage: Int = 0,

    // game mode settings
    var isLockoutMode: Boolean = false,
    var isInventoryMode: Boolean = false,
    var isRankedMode: Boolean = false,
    @JsonNames("hiddenItems")
    var isHiddenItemsMode: Boolean = false,

    // game feature settings
    var isElytra: Boolean = false,
    var isNightVision: Boolean = false,
    var isUnlockRecipes: Boolean = true,
    var isPvpEnabled: Boolean = true,
    var isKeepInventory: Boolean = false,

    // team spawnpoint settings
    var spawnDimension: String = "minecraft:overworld",
    @JsonNames("spreadPlayersDistance")
    var spawnDistance: Int = 100,

    // score info settings
    var showRemainingTime: Boolean = true,
    var showCompletedItems: Boolean = true,
    var showCompletedLines: Boolean = true,
    var showLeadingTeam: Boolean = true,

    // bingo card settings
    @JsonNames("drawOtherTeamCompletions")
    var showOtherTeamCompletions: Boolean = false,
    var showPreviewCard: Boolean = false,
    var showHiddenTiers: Boolean = false,

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

    // amount of time to wait before starting the game
    var startCountdownSeconds: Int = 8,
) {

    init {
        // Verify that the chosen tier list actually exists
        val tierLists = TierListService.readTierLists()
        if (this.tierList !in tierLists)
            tierLists.firstOrNull()?.let { this.tierList = it }
    }

    fun isValid(): Boolean {
        return itemDistribution.sum() <= 25
    }

    fun formatGameMode(): String {
        val gameMode = buildList {
            if (isLockoutMode) add("Lockout")
            if (isInventoryMode) add("Inventory")
            if (isRankedMode) add("Ranked")
            if (isHiddenItemsMode) add("Hidden Items")
        }

        return gameMode.joinToString(", ").ifEmpty { "Standard" }
    }

    fun formatFeatures(): String {
        return buildString {
            if (isElytra) append("Elytra, ")
            if (isNightVision) append("NightVis, ")
            if (isKeepInventory) append("KeepInv, ")
            if (isPvpEnabled) append("PvP, ")
        }
            .removeSuffix(", ")
            .ifEmpty { "None" }
    }

    fun copyFrom(options: BingoOptions) {
        for (p in BingoOptions::class.memberProperties) {
            @Suppress("UNCHECKED_CAST")
            val property = p as? KMutableProperty1<BingoOptions, Any?>
                ?: continue

            val value = property.getValue(options, property)
            property.setValue(this, property, value)
        }
    }

}