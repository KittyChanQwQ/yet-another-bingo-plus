package me.jfenn.bingo.common.config

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonNames
import me.jfenn.bingo.common.card.BingoCard
import me.jfenn.bingo.common.spawn.SpawnItem
import me.jfenn.bingo.common.utils.json
import me.jfenn.bingo.generated.StringKey
import java.security.MessageDigest
import java.util.*
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.full.memberProperties
import kotlin.time.Duration

@Serializable
@OptIn(ExperimentalSerializationApi::class)
data class BingoOptions(
    var winCondition: BingoWinCondition = BingoWinCondition.Cards(1),
    var cards: List<BingoCardOptions> = listOf(BingoCardOptions()),

    var skipCardWhenLost: Boolean = true,
    var endGameWhenStalemate: Boolean = true,
    var endGameWhenComplete: Boolean = true,

    var timeLimit: Duration? = null,
    var tierListPage: Int = 0,

    // game feature settings
    var isElytra: Boolean = false,
    var isNightVision: Boolean = false,
    var isUnlockRecipes: Boolean = true,
    var isPvpEnabled: Boolean = true,
    var isKeepInventory: Boolean = false,

    // team spawnpoint settings
    var spawnDimension: String = "minecraft:overworld",
    @JsonNames("spreadPlayersDistance")
    var spawnDistance: Int = 25,

    // score info settings
    var showRemainingTime: Boolean = true,
    var showCompletedItems: Boolean = true,
    var showCompletedLines: Boolean = true,
    var showLeadingTeam: Boolean = true,

    // bingo card settings
    var showPreviewCard: Boolean = true,
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
        return cards.all { it.isValid() }
    }

    fun formatGameMode(card: BingoCard): List<StringKey> {
        val gameMode = buildList {
            if (card.options.isLockoutMode) add(StringKey.OptionsModeLockout)
            if (card.options.isInventoryMode) add(StringKey.OptionsModeInventory)
            if (card.options.isHiddenItemsMode) add(StringKey.OptionsModeHiddenItems)
            if (winCondition is BingoWinCondition.Infinite) add(StringKey.OptionsWinConditionInfinite)
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

    /**
     * Generates a unique hash reflecting any game settings that might
     * have an effect on its difficulty
     * (excluding purely visual settings, such as showCompletedLines)
     */
    fun getShaHash(): String {
        val md = MessageDigest.getInstance("SHA-512")

        sequence {
            yield(json.encodeToString(winCondition))

            for (card in cards) {
                // card difficulty
                yield(json.encodeToString(card.goal))
                yield(json.encodeToString(card.itemDistribution))
                yield(card.itemFilter.toString())
                yield(json.encodeToString(card.tierList))
                // gamemodes
                yield(card.isLockoutMode.toString())
                yield(card.isInventoryMode.toString())
                yield(card.isHiddenItemsMode.toString())
                yield(",")
            }
            // features
            yield(isKeepInventory.toString())
            yield(isElytra.toString())
            yield((isPlayerKit || isTeamKit).toString()) // has starting items enabled
            // spawning
            yield(spawnDimension)
        }.forEach {
            md.update(it.toByteArray())
        }

        val digest = md.digest()
        return HexFormat.of().formatHex(digest)
    }

}