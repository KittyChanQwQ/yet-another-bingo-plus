package me.jfenn.bingo.common.config

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames
import me.jfenn.bingo.common.spawn.SpawnItem
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.full.memberProperties
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

@Serializable
@OptIn(ExperimentalSerializationApi::class)
data class BingoOptions(
    var winCondition: WinCondition = WinCondition.THREE_LINES,
    var timeLimit: Duration? = 60.minutes,
    var itemDistribution: List<Int> = listOf(0, 5, 7, 7, 6),
    var tierList: String = "default",

    // game mode settings
    var isLockoutMode: Boolean = false,
    var isInventoryMode: Boolean = false,
    var isRankedMode: Boolean = false,
    @JsonNames("hiddenItems")
    var isHiddenItemsMode: Boolean = false,

    // game feature settings
    var isElytra: Boolean = false,
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
) {

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
        val features = buildList {
            if (isElytra) add("Elytra")
            if (isKeepInventory) add("KeepInv")
            if (isPvpEnabled) add("PvP")
        }

        return features.joinToString(", ").ifEmpty { "None" }
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