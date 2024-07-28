package me.jfenn.bingo.common.card.objective

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import me.jfenn.bingo.api.IAdvancementHandle
import me.jfenn.bingo.api.item.IItemStack
import me.jfenn.bingo.common.MOD_ID_BINGO
import me.jfenn.bingo.common.card.tierlist.TierLabel
import me.jfenn.bingo.common.team.BingoPlayerProfile
import me.jfenn.bingo.common.team.BingoTeamKey
import me.jfenn.bingo.common.utils.UuidAsString
import me.jfenn.bingo.generated.StringKey
import net.minecraft.text.Text
import java.util.UUID
import kotlin.time.Duration

@Serializable
sealed class BingoObjective {

    abstract val id: String

    /** Includes teams that have observed or made partial progress on the objective */
    val teamsSeen = HashSet<BingoTeamKey>()
    /** Includes teams that have currently achieved the objective */
    val teams = HashSet<BingoTeamKey>()
    /** Includes teams that have previously achieved and then lost the objective */
    val teamsOnceAchieved = HashSet<BingoTeamKey>()
    /** Includes players that have currently achieved the objective */
    val players = HashMap<UuidAsString, Instant>()

    @Transient
    open val display: ObjectiveDisplay.Resolved = ObjectiveDisplay.Resolved.EMPTY

    @Transient
    val dependsOnObjectives = mutableSetOf<String>()
    @Transient
    val conflictsWithObjectives = mutableSetOf<String>()

    fun visualItem(tier: TierLabel?): IItemStack? {
        return this.display.item?.copy()?.also { stack ->
            stack.setDisplay(
                name = this.display.name
                    ?.takeIf { it != stack.displayName },
                lore = this.display.lore
                    .plus(
                        tier?.let { listOf(it.text()) }
                            ?: emptyList()
                    )
                    .takeIf { it.isNotEmpty() },
            )
            stack.setHideFlags(255)
        }
    }

    open fun hasAchieved(player: BingoPlayerProfile): Boolean = players.containsKey(player.uuid)
    open fun hasAchieved(team: BingoTeamKey): Boolean = teams.contains(team)

    /**
     * @return true if the tile state has changed
     */
    open fun setAchieved(team: BingoTeamKey, value: Boolean): Boolean {
        return when {
            value -> teams.add(team)
            else -> {
                val changed = teams.remove(team)
                if (changed) {
                    // when first lost, add the team to teamsOnceAchieved
                    teamsOnceAchieved.add(team)
                }
                changed
            }
        }.also { changed ->
            if (changed) markDirty(team)
        }
    }

    open fun hasAnyAchieved() = teams.isNotEmpty()
    fun hasNoneAchieved() = !hasAnyAchieved()

    fun addPlayer(player: UUID, instant: Instant) {
        players[player] = instant
    }

    fun addPlayer(player: BingoPlayerProfile, instant: Instant) {
        addPlayer(player.uuid, instant)
    }

    fun removePlayers(iterable: Iterable<BingoPlayerProfile>) {
        for (player in iterable) {
            players.remove(player.uuid)
        }
    }

    /**
     * Checks if the [team] has seen the item on
     * the current tile (regardless of whether it can be scored)
     */
    open fun hasSeen(team: BingoTeamKey): Boolean {
        return teamsSeen.contains(team)
    }

    /**
     * Marks the tile as seen by the provided [team].
     */
    open fun markSeen(team: BingoTeamKey) {
        teamsSeen.add(team)
    }

    @Transient
    protected val updatedAtMap = mutableMapOf<BingoTeamKey?, Instant>()

    /**
     * Determines the last time that the tile was changed at.
     * If [team] is not null, this returns the last changed time
     * for the specified team.
     */
    open fun updatedAt(team: BingoTeamKey?): Instant = updatedAtMap.getOrPut(team) { Clock.System.now() }

    /**
     * Marks the tile as changed, indicating that it needs to be redrawn on
     * the bingo map
     */
    open fun markDirty(team: BingoTeamKey) {
        Clock.System.now().let {
            updatedAtMap[team] = it
            updatedAtMap[null] = it
        }
    }

    /**
     * Returns when the tile is flashing on the map, based on its update time
     * - i.e. if the item has been updated within [duration]
     */
    open fun isFlashing(team: BingoTeamKey?, duration: Duration): Boolean {
        return team != null &&
                hasAchieved(team) &&
                Clock.System.now() - updatedAt(team) < duration
    }

    @Serializable
    @SerialName("free_space")
    class FreeSpace : BingoObjective() {
        override val id: String get() = "$MOD_ID_BINGO:free_space"

        override val display: ObjectiveDisplay.Resolved
            get() = ObjectiveDisplay.Resolved(
                name = Text.translatableWithFallback(StringKey.CardFreeSpace.key, "Free Space"),
                lore = emptyList(),
                item = null,
            )

        override fun hasAnyAchieved(): Boolean = true
        override fun hasAchieved(player: BingoPlayerProfile): Boolean = true
        override fun hasAchieved(team: BingoTeamKey): Boolean = true
        override fun setAchieved(team: BingoTeamKey, value: Boolean): Boolean = false
        override fun isFlashing(team: BingoTeamKey?, duration: Duration): Boolean = false
    }

    @Serializable
    @SerialName("item")
    data class ItemEntry(
        override val id: String,
        val itemId: String,
        val itemCount: Int = 1,
        val itemNbt: String? = null,
        val itemComponents: Map<String, String?>? = null,
        val displayData: ObjectiveDisplay.Data? = null,
    ): BingoObjective() {
        init {
            conflictsWithObjectives.add(itemId)
        }

        @Transient
        override var display: ObjectiveDisplay.Resolved = ObjectiveDisplay.Resolved.EMPTY

        @Transient
        var itemStack: IItemStack? = null
    }

    @Serializable
    @SerialName("advancement")
    data class AdvancementEntry(
        override val id: String,
        val advancementId: String,
    ): BingoObjective() {
        init {
            conflictsWithObjectives.add(advancementId)
        }

        @Transient
        override var display: ObjectiveDisplay.Resolved = ObjectiveDisplay.Resolved.EMPTY

        @Transient
        var advancement: IAdvancementHandle? = null
    }

    @Serializable
    @SerialName("one_of")
    data class OneOfEntry(
        override val id: String,
        val oneOfObjectives: Set<String>,
        val displayData: ObjectiveDisplay.Data?,
    ): BingoObjective() {
        init {
            dependsOnObjectives.addAll(oneOfObjectives)
            conflictsWithObjectives.addAll(oneOfObjectives)
        }

        @Transient
        override var display: ObjectiveDisplay.Resolved = ObjectiveDisplay.Resolved.EMPTY
    }

    @Serializable
    @SerialName("all_of")
    data class AllOfEntry(
        override val id: String,
        val allOfObjectives: Set<String>,
        val displayData: ObjectiveDisplay.Data?,
    ): BingoObjective() {
        init {
            dependsOnObjectives.addAll(allOfObjectives)
            conflictsWithObjectives.addAll(allOfObjectives)
        }

        @Transient
        override var display: ObjectiveDisplay.Resolved = ObjectiveDisplay.Resolved.EMPTY
    }

    @Serializable
    @SerialName("inverse")
    data class InverseEntry(
        override val id: String,
        val inverseObjective: String,
        val permanent: Boolean,
        val displayData: ObjectiveDisplay.Data?,
    ): BingoObjective() {
        init {
            dependsOnObjectives.add(inverseObjective)
            conflictsWithObjectives.add(inverseObjective)
        }

        @Transient
        override var display: ObjectiveDisplay.Resolved = ObjectiveDisplay.Resolved.EMPTY
    }
}