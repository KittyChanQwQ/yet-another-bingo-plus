package me.jfenn.bingo.common.card

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import me.jfenn.bingo.common.NBT_BINGO_IGNORE
import me.jfenn.bingo.common.config.TierLabel
import me.jfenn.bingo.common.utils.text.TextColor
import me.jfenn.bingo.common.utils.text.TextInfo
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtList
import net.minecraft.nbt.NbtString
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier
import kotlin.time.Duration

@Serializable
sealed class BingoCardEntry {

    open val teams = HashSet<BingoTeamKey>()
    private val teamsSeen = HashSet<BingoTeamKey>()

    abstract val visualItem: ItemStack?

    abstract val tier: TierLabel?

    open fun formatName(): String? = null
    open fun formatLore(): List<String>? = null

    fun visualItem(): ItemStack? {
        return visualItem?.copy()?.also { stack ->
            val name = formatName()
            val lore = formatLore()
            stack.getOrCreateNbt().apply {
                put("display", NbtCompound().apply {
                    if (name != null)
                        putString("Name", name)

                    put("Lore", NbtList().apply {
                        lore?.forEach { add(NbtString.of(it)) }
                        tier?.let { tier ->
                            add(
                                NbtString.of(
                                TextInfo.toJsonString(TextInfo(
                                    text = "${tier.name}-tier",
                                    color = tier.formatting,
                                ))
                            ))
                        }
                    })
                })
                putInt("HideFlags", 255)
            }
        }
    }

    open fun canSatisfy(stack: ItemStack): Boolean = false

    open fun hasAchieved(team: BingoTeam): Boolean {
        return teams.contains(team.key)
    }

    /**
     * @return true if the tile state has changed
     */
    open fun setAchieved(team: BingoTeam, value: Boolean): Boolean {
        return when {
            value -> teams.add(team.key)
            else -> teams.remove(team.key)
        }.also { changed ->
            if (changed) markDirty(team)
        }
    }

    open fun hasAnyAchieved() = teams.isNotEmpty()
    fun hasNoneAchieved() = !hasAnyAchieved()

    /**
     * Checks if the [team] has seen the item on
     * the current tile (regardless of whether it can be scored)
     */
    open fun hasSeen(team: BingoTeam): Boolean {
        return teamsSeen.contains(team.key)
    }

    /**
     * Marks the tile as seen by the provided [team].
     */
    open fun markSeen(team: BingoTeam) {
        if (teamsSeen.add(team.key)) {
            markDirty(team)
        }
    }

    @Transient
    private val updatedAtMap = mutableMapOf<BingoTeamKey?, Instant>()

    /**
     * Determines the last time that the tile was changed at.
     * If [team] is not null, this returns the last changed time
     * for the specified team.
     */
    open fun updatedAt(team: BingoTeam?): Instant = updatedAtMap.getOrPut(team?.key) { Clock.System.now() }

    /**
     * Marks the tile as changed, indicating that it needs to be redrawn on
     * the bingo map
     */
    open fun markDirty(team: BingoTeam) {
        Clock.System.now().let {
            updatedAtMap[team.key] = it
            updatedAtMap[null] = it
        }
    }

    /**
     * Returns when the tile is flashing on the map, based on its update time
     * - i.e. if the item has been updated within [duration]
     */
    open fun isFlashing(team: BingoTeam?, duration: Duration): Boolean {
        return team != null &&
                hasAchieved(team) &&
                Clock.System.now() - updatedAt(team) < duration
    }

    @Serializable
    class FreeSpace : BingoCardEntry() {
        @Transient
        override val visualItem: ItemStack? = null

        @Transient
        override val tier: TierLabel? = null
        override fun hasAnyAchieved(): Boolean = true
        override fun hasAchieved(team: BingoTeam): Boolean = true
        override fun setAchieved(team: BingoTeam, value: Boolean): Boolean = false
        override fun isFlashing(team: BingoTeam?, duration: Duration): Boolean = false

        override fun formatName(): String {
            return TextInfo.toJsonString(
                TextInfo.literal("Free Space")
            )
        }
    }

    @Serializable
    data class ItemEntry @Throws(IllegalArgumentException::class) constructor(
        val identifier: String,
        override val tier: TierLabel,
    ): BingoCardEntry() {

        @Transient
        val identifierInstance = try {
            Identifier(identifier)
        } catch (e: Throwable) {
            throw IllegalArgumentException("Could not parse identifier '$identifier'")
        }

        @Transient
        override val visualItem: ItemStack = Registries.ITEM.get(identifierInstance)
            .takeIf { it != Items.AIR }
            ?.let { ItemStack(it, 1) }
            ?: throw IllegalArgumentException("Item identifier '$identifier' does not exist.")

        /**
         * Returns true if the item can be counted to complete the bingo tile
         *
         * Returns false if any of:
         * - item does not match the card item
         * - item has a bingo card's map ID
         * - item has a "bingo_ignore:true" NBT tag
         */
        override fun canSatisfy(stack: ItemStack): Boolean {
            return stack.item == item() && stack.nbt?.getBoolean(NBT_BINGO_IGNORE) != true
        }

        fun item(): Item {
            return visualItem.item
        }

    }

    @Serializable
    data class AdvancementEntry @Throws(IllegalArgumentException::class) constructor(
        val identifier: String,
        override val tier: TierLabel,
    ): BingoCardEntry() {

        @Transient
        val identifierInstance = try {
            Identifier(identifier)
        } catch (e: Throwable) {
            throw IllegalArgumentException("Could not parse identifier '$identifier'")
        }

        // To be populated by [BingoCardController] once we have a server reference
        @Transient
        override var visualItem: ItemStack? = null

        var name: String? = null
        var description: String? = null

        override fun formatName(): String? = name
        override fun formatLore(): List<String>? = description?.let { listOf(it) }

    }

    @Serializable
    data class MultiEntry(
        val innerEntries: List<BingoCardEntry>,
        override val tier: TierLabel,
    ): BingoCardEntry() {
        override val visualItem: ItemStack? get() = innerEntries.firstOrNull()?.visualItem

        override fun formatName(): String {
            return TextInfo.toJsonString(
                TextInfo.literal("One of:")
                    .formatted(TextColor.GRAY)
            )
        }

        override fun formatLore(): List<String> {
            return innerEntries
                .mapNotNull { it.visualItem?.translationKey }
                .map { "{\"translate\":\"$it\"}" }
        }

        override fun canSatisfy(stack: ItemStack): Boolean {
            return innerEntries.any { it.canSatisfy(stack) }
        }
    }
}
