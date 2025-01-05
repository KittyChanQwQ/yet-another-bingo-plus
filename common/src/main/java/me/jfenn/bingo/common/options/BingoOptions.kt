package me.jfenn.bingo.common.options

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonNames
import me.jfenn.bingo.common.card.BingoCard
import me.jfenn.bingo.common.menu.*
import me.jfenn.bingo.common.spawn.SpawnItem
import me.jfenn.bingo.common.utils.copyMemberProperties
import me.jfenn.bingo.common.utils.json
import me.jfenn.bingo.generated.StringKey
import java.security.MessageDigest
import java.util.*
import kotlin.time.Duration

@Serializable
@OptIn(ExperimentalSerializationApi::class)
data class BingoOptions(
    var winCondition: BingoWinCondition = BingoWinCondition.Cards(1),
    var cards: List<BingoCardOptions> = listOf(BingoCardOptions()),

    var skipCardWhenLost: Boolean = true,
    var endGameWhenStalemate: Boolean = true,
    var endGameWhen: EndWhen = EndWhen.FirstWin,

    var timeLimit: Duration? = null,

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
        SpawnItem(
            id = "minecraft:netherite_helmet",
            nbt = "{Enchantments:[{lvl:4,id:protection},{lvl:1,id:aqua_affinity},{lvl:3,id:respiration}],Unbreakable:1}",
            components = mapOf(
                "minecraft:enchantments" to """{"levels":{"protection":4,"aqua_affinity":1,"respiration":3}}""",
                "minecraft:unbreakable" to "{}",
            )
        ),
        SpawnItem(
            id = "minecraft:netherite_chestplate",
            nbt = "{Enchantments:[{lvl:4,id:protection}],Unbreakable:1}",
            components = mapOf(
                "minecraft:enchantments" to """{"levels":{"protection":4}}""",
                "minecraft:unbreakable" to "{}",
            )
        ),
        SpawnItem(
            id = "minecraft:netherite_leggings",
            nbt = "{Enchantments:[{lvl:4,id:protection},{lvl:3,id:swift_sneak}],Unbreakable:1}",
            components = mapOf(
                "minecraft:enchantments" to """{"levels":{"protection":4,"swift_sneak":3}}""",
                "minecraft:unbreakable" to "{}",
            )
        ),
        SpawnItem(
            id = "minecraft:netherite_boots",
            nbt = "{Enchantments:[{lvl:4,id:protection},{lvl:4,id:feather_falling},{lvl:3,id:depth_strider}],Unbreakable:1}",
            components = mapOf(
                "minecraft:enchantments" to """{"levels":{"protection":4,"feather_falling":4,"depth_strider":3}}""",
                "minecraft:unbreakable" to "{}",
            )
        ),
        SpawnItem(
            id = "minecraft:netherite_pickaxe",
            nbt = "{Enchantments:[{lvl:5,id:efficiency},{lvl:3,id:fortune}],Unbreakable:1}",
            components = mapOf(
                "minecraft:enchantments" to """{"levels":{"efficiency":5,"fortune":3}}""",
                "minecraft:unbreakable" to "{}",
            )
        ),
        SpawnItem(
            id = "minecraft:netherite_axe",
            nbt = "{Enchantments:[{lvl:5,id:efficiency},{lvl:5,id:sharpness}],Unbreakable:1}",
            components = mapOf(
                "minecraft:enchantments" to """{"levels":{"efficiency":5,"sharpness":5}}""",
                "minecraft:unbreakable" to "{}",
            )
        ),
        SpawnItem(
            id = "minecraft:netherite_sword",
            nbt = "{Enchantments:[{lvl:2,id:knockback},{lvl:3,id:looting},{lvl:5,id:sharpness}],Unbreakable:1}",
            components = mapOf(
                "minecraft:enchantments" to """{"levels":{"knockback":2,"looting":3,"sharpness":5}}""",
                "minecraft:unbreakable" to "{}",
            )
        ),
        SpawnItem(
            id = "minecraft:netherite_shovel",
            nbt = "{Enchantments:[{lvl:5,id:efficiency}],Unbreakable:1}",
            components = mapOf(
                "minecraft:enchantments" to """{"levels":{"efficiency":5}}""",
                "minecraft:unbreakable" to "{}",
            )
        ),
        SpawnItem(
            id = "minecraft:iron_pickaxe",
            nbt = "{Enchantments:[{lvl:1,id:silk_touch}],Unbreakable:1}",
            components = mapOf(
                "minecraft:enchantments" to """{"levels":{"silk_touch":1}}""",
                "minecraft:unbreakable" to "{}",
            )
        ),
        SpawnItem(
            id = "minecraft:shield",
            components = mapOf(
                "minecraft:unbreakable" to "{}",
            )
        ),
        SpawnItem("minecraft:torch", count = 64),
        SpawnItem("minecraft:golden_carrot", count = 64),
        SpawnItem("minecraft:oak_log", count = 16),
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
            if (card.options.isConsumeItemsMode) add(StringKey.OptionsModeConsumeItems)
            if (winCondition is BingoWinCondition.Infinite) add(StringKey.OptionsWinConditionInfinite)
        }

        return gameMode.ifEmpty { listOf(StringKey.OptionsModeStandard) }
    }

    fun formatGameModeIcons(card: BingoCard): List<String> {
        val gameMode = buildList {
            if (card.options.isLockoutMode) add(ICON_LOCKOUT)
            if (card.options.isInventoryMode) add(ICON_INVENTORY)
            if (card.options.isHiddenItemsMode) add(ICON_HIDDEN)
            if (card.options.isConsumeItemsMode) add(ICON_CONSUME)
            if (winCondition is BingoWinCondition.Infinite) add("∞")
        }

        return gameMode
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

    fun formatFeaturesIcons(): List<String> {
        val features = buildList {
            if (isElytra) add(ICON_ELYTRA)
            if (isNightVision) add(ICON_NIGHTVIS)
            if (isKeepInventory) add(ICON_KEEP_INVENTORY)
            if (isPvpEnabled) add(ICON_PVP)
        }

        return features
    }

    fun copyFrom(defaultOptions: BingoOptions) {
        copyMemberProperties(
            from = defaultOptions,
            to = this,
            excluding = setOf(BingoOptions::cards),
        )
        // since options are passed by reference, these should be copied from the default config
        // to avoid mutation
        cards = defaultOptions.cards.map { it.copy() }
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