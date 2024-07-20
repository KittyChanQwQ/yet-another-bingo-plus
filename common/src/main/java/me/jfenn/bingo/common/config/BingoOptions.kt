package me.jfenn.bingo.common.config

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames
import me.jfenn.bingo.common.spawn.SpawnItem
import me.jfenn.bingo.common.utils.joinText
import me.jfenn.bingo.generated.StringKey
import net.minecraft.text.MutableText
import net.minecraft.text.Text
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.full.memberProperties
import kotlin.time.Duration

@Serializable
@OptIn(ExperimentalSerializationApi::class)
data class BingoOptions(
    var cardGoal: BingoGoal = BingoGoal.Lines(3),
    var timeLimit: Duration? = null,
    var itemDistribution: List<Int> = listOf(0, 5, 7, 7, 6),
    var tierLists: Map<String, Int> = mapOf("all_items" to 1),
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

    fun isValid(): Boolean {
        return itemDistribution.sum() <= 25 && tierLists.isNotEmpty()
    }

    fun formatTierLists(state: TierListState) : MutableText {
        val lists = tierLists
            .filter { (name, _) -> state.tierLists.containsKey(name) }
            .map { (name, weight) -> name to weight.coerceAtLeast(1) }
            .toMap()

        val showPercentage = lists.size > 1
        val total = lists.values.sum()

        return lists.entries
            .map { (name, weight) ->
                when {
                    showPercentage -> Text.empty()
                        .append(state.title(name))
                        .append(String.format(" (%.0f%%)", weight * 100f / total))
                    else -> state.title(name)
                }
            }
            .joinText()
    }

    fun formatGameMode(): List<StringKey> {
        val gameMode = buildList {
            if (isLockoutMode) add(StringKey.OptionsModeLockout)
            if (isInventoryMode) add(StringKey.OptionsModeInventory)
            if (isRankedMode) add(StringKey.OptionsModeRanked)
            if (isHiddenItemsMode) add(StringKey.OptionsModeHiddenItems)
        }

        return gameMode.ifEmpty { listOf(StringKey.OptionsModeStandard) }
    }

    fun formatFeatures(): List<StringKey> {
        val features = buildList {
            if (isElytra) add(StringKey.OptionsElytra)
            if (isNightVision) add(StringKey.OptionsNightVisShort)
            if (isKeepInventory) add(StringKey.OptionsKeepInventoryShort)
            if (isPvpEnabled) add(StringKey.OptionsAllowPvpShort)
        }

        return features.ifEmpty { listOf(StringKey.OptionsFeaturesNone) }
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