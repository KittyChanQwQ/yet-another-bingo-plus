package me.jfenn.bingo.common.card.objective

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.JsonElement
import me.jfenn.bingo.api.IAdvancementHandle
import me.jfenn.bingo.api.item.IItemStack
import me.jfenn.bingo.common.MOD_ID_BINGO
import me.jfenn.bingo.common.card.tierlist.TierLabel
import me.jfenn.bingo.common.team.BingoPlayerProfile
import me.jfenn.bingo.common.team.BingoTeamKey
import me.jfenn.bingo.common.utils.UuidAsString
import me.jfenn.bingo.generated.StringKey
import net.minecraft.text.Text
import java.util.*
import kotlin.time.Duration

@Serializable
sealed class BingoObjective {

    abstract val id: String

    /** Includes teams that have observed or made partial progress on the objective */
    var teamsSeen = mutableSetOf<BingoTeamKey>()
    /** Includes teams that have achieved the objective */
    var teamsAchieved = mutableMapOf<BingoTeamKey, Instant>()
    /** Includes teams that have lost the objective */
    var teamsLost = mutableMapOf<BingoTeamKey, Instant>()
    /** Includes teams that have previously achieved and then lost the objective */
    val teamsOnceAchieved = mutableSetOf<BingoTeamKey>()
    /** Includes players that have achieved the objective */
    var players = mutableMapOf<UuidAsString, BingoObjectiveCapture>()
    /** Includes players that currently satisfy the criteria for the objective */
    var playersHolding = mutableMapOf<UuidAsString, BingoObjectiveCapture>()

    var teamsProgress = mutableMapOf<BingoTeamKey, Float>()

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
                    ?.plus(
                        tier?.let { listOf(it.text()) }
                            ?: emptyList()
                    )
                    ?.takeIf { it.isNotEmpty() },
            )
            stack.setHideFlags(255)
        }
    }

    open fun hasAchieved(player: BingoPlayerProfile): Boolean = players.containsKey(player.uuid)
    open fun hasAchieved(team: BingoTeamKey): Boolean = teamsAchieved.containsKey(team)

    open fun hasAnyAchieved() = teamsAchieved.isNotEmpty()
    fun hasNoneAchieved() = !hasAnyAchieved()

    open fun removePlayer(uuid: UUID) {
        playersHolding.remove(uuid)
        players.remove(uuid)
    }

    /**
     * Checks if the [team] has seen the item on
     * the current tile (regardless of whether it can be scored)
     */
    open fun hasSeen(team: BingoTeamKey): Boolean {
        return teamsSeen.contains(team)
    }

    fun getProgress(team: BingoTeamKey) = teamsProgress[team] ?: 0f

    fun achievedAt(team: BingoTeamKey?): Instant? {
        return when {
            team != null -> teamsAchieved[team]
            else -> teamsAchieved.values.maxOrNull()
        }
    }

    fun lostAt(team: BingoTeamKey?): Instant? {
        return when {
            team != null -> teamsLost[team]
            else -> teamsLost.values.maxOrNull()
        }
    }

    /**
     * Determines the last time that the tile was updated at.
     * If [team] is not null, this returns the last changed time
     * for the specified team.
     */
    fun updatedAt(team: BingoTeamKey?): Instant {
        return listOfNotNull(achievedAt(team), lostAt(team))
            .maxOrNull()
            ?: Instant.DISTANT_PAST
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

        val playersOnceAchieved = mutableSetOf<UuidAsString>()

        override fun removePlayer(uuid: UUID) {
            super.removePlayer(uuid)
            playersOnceAchieved.remove(uuid)
        }

        @Transient
        override var display: ObjectiveDisplay.Resolved = ObjectiveDisplay.Resolved.EMPTY
    }

    @Serializable
    @SerialName("opponent")
    data class OpponentEntry(
        override val id: String,
        val opponentObjective: String,
        val permanent: Boolean,
        val displayData: ObjectiveDisplay.Data?,
    ): BingoObjective() {
        init {
            dependsOnObjectives.add(opponentObjective)
            conflictsWithObjectives.add(opponentObjective)
        }

        @Transient
        override var display: ObjectiveDisplay.Resolved = ObjectiveDisplay.Resolved.EMPTY
    }
}