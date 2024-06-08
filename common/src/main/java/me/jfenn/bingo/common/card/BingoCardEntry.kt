package me.jfenn.bingo.common.card

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import me.jfenn.bingo.api.item.IItemStack
import me.jfenn.bingo.common.NBT_BINGO_IGNORE
import me.jfenn.bingo.common.config.TierLabel
import me.jfenn.bingo.common.team.BingoPlayerProfile
import me.jfenn.bingo.common.team.BingoTeam
import me.jfenn.bingo.common.team.BingoTeamKey
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import kotlin.time.Duration

@Serializable
sealed class BingoCardEntry {

    open val teams = HashSet<BingoTeamKey>()
    open val players = HashSet<BingoPlayerProfile>()

    private val teamsSeen = HashSet<BingoTeamKey>()

    abstract val visualItem: IItemStack?

    abstract val tier: TierLabel?

    open fun formatName(): Text? = null
    open fun formatLore(): List<Text>? = null

    fun visualItem(): IItemStack? {
        return visualItem?.copy()?.also { stack ->
            stack.setDisplay(
                name = formatName(),
                lore = (formatLore() ?: emptyList())
                    .plus(
                        tier?.let { listOf(Text.literal("$it-tier").formatted(it.formatting)) }
                            ?: emptyList()
                    )
                    .takeIf { it.isNotEmpty() },
            )
            stack.setHideFlags(255)
        }
    }

    open fun canSatisfy(stack: IItemStack): Boolean = false

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
        override val visualItem: IItemStack? = null

        @Transient
        override val tier: TierLabel? = null
        override fun hasAnyAchieved(): Boolean = true
        override fun hasAchieved(team: BingoTeam): Boolean = true
        override fun setAchieved(team: BingoTeam, value: Boolean): Boolean = false
        override fun isFlashing(team: BingoTeam?, duration: Duration): Boolean = false

        override fun formatName(): Text {
            return Text.literal("Free Space")
        }
    }

    @Serializable
    data class ItemEntry @Throws(IllegalArgumentException::class) constructor(
        val identifier: String,
        override val tier: TierLabel,
    ): BingoCardEntry() {

        // To be populated by [BingoCardController] once we have a server reference
        @Transient
        override var visualItem: IItemStack? = null

        /**
         * Returns true if the item can be counted to complete the bingo tile
         *
         * Returns false if any of:
         * - item does not match the card item
         * - item has a bingo card's map ID
         * - item has a "bingo_ignore:true" NBT tag
         */
        override fun canSatisfy(stack: IItemStack): Boolean {
            return stack.item == item()?.item && !stack.hasCustomTag(NBT_BINGO_IGNORE)
        }

        fun item(): IItemStack? {
            return visualItem
        }

    }

    @Serializable
    data class AdvancementEntry @Throws(IllegalArgumentException::class) constructor(
        val identifier: String,
        override val tier: TierLabel,
    ): BingoCardEntry() {

        // To be populated by [BingoCardController] once we have a server reference
        @Transient
        override var visualItem: IItemStack? = null

        @Transient
        var name: Text? = null
        @Transient
        var description: List<Text>? = null

        override fun formatName(): Text? = name
        override fun formatLore(): List<Text>? = description

    }

    @Serializable
    data class MultiEntry(
        val innerEntries: List<BingoCardEntry>,
        override val tier: TierLabel,
    ): BingoCardEntry() {
        override val visualItem: IItemStack? get() = innerEntries.firstOrNull()?.visualItem

        override fun formatName(): Text {
            return Text.literal("One of:").formatted(Formatting.GRAY)
        }

        override fun formatLore(): List<Text> {
            return innerEntries.mapNotNull { it.visualItem?.displayName }
        }

        override fun canSatisfy(stack: IItemStack): Boolean {
            return innerEntries.any { it.canSatisfy(stack) }
        }
    }
}
