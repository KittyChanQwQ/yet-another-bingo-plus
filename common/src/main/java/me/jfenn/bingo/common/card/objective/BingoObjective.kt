package me.jfenn.bingo.common.card.objective

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.encodeToString
import me.jfenn.bingo.api.IAdvancementHandle
import me.jfenn.bingo.api.IObjectiveHandle
import me.jfenn.bingo.api.IStatHandle
import me.jfenn.bingo.api.item.IItemStack
import me.jfenn.bingo.common.MOD_ID_BINGO
import me.jfenn.bingo.common.card.data.ObjectiveData
import me.jfenn.bingo.common.team.BingoPlayerProfile
import me.jfenn.bingo.common.team.BingoTeam
import me.jfenn.bingo.common.team.BingoTeamKey
import me.jfenn.bingo.common.utils.UuidAsString
import me.jfenn.bingo.common.utils.jsonUnpretty
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

    abstract val data: ObjectiveData?

    @Transient
    open var display: ObjectiveDisplay.Resolved = ObjectiveDisplay.Resolved.EMPTY

    open val dependsOnObjectives get() = emptySet<String>()

    open val conflictsWithObjectives get() = (data?.conflictsWith ?: emptySet()) + id

    open fun hasAchieved(player: BingoPlayerProfile): Boolean = players.containsKey(player.uuid)
    open fun hasAchieved(team: BingoTeamKey): Boolean = teamsAchieved.containsKey(team)

    open fun hasAnyAchieved() = teamsAchieved.isNotEmpty()
    fun hasNoneAchieved() = !hasAnyAchieved()

    open fun removePlayer(uuid: UUID) {
        playersHolding.remove(uuid)
        players.remove(uuid)
    }

    open fun removeTeam(team: BingoTeam) {
        val teamKey = team.key
        teamsSeen.remove(teamKey)
        teamsAchieved.remove(teamKey)
        teamsLost.remove(teamKey)
        teamsOnceAchieved.remove(teamKey)
        teamsProgress.remove(teamKey)

        for (player in team.players) {
            players.remove(player.uuid)
            playersHolding.remove(player.uuid)
        }
    }

    /**
     * Checks if the [team] has seen the item on
     * the current tile (regardless of whether it can be scored)
     */
    open fun hasSeen(team: BingoTeamKey): Boolean {
        return teamsSeen.contains(team) || teamsAchieved.containsKey(team)
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
        val achieved = achievedAt(team) ?: Instant.DISTANT_PAST
        val lost = lostAt(team) ?: Instant.DISTANT_PAST
        return maxOf(achieved, lost)
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

        @Transient
        override val data: ObjectiveData? = null

        override var display: ObjectiveDisplay.Resolved
            set(_) {}
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
        override val data: ObjectiveData.Item,
        var itemCount: Int = 1,
    ): BingoObjective() {

        override val conflictsWithObjectives: Set<String>
            get() = super.conflictsWithObjectives + itemId

        val itemId get() = data.item
        val itemNbt get() = data.nbt

        @Transient
        val itemComponents = data.components?.mapValues { (_, data) -> data?.let { jsonUnpretty.encodeToString(it) } }

        @Transient
        var itemStack: IItemStack? = null
    }

    @Serializable
    @SerialName("advancement")
    data class AdvancementEntry(
        override val id: String,
        override val data: ObjectiveData.Advancement,
        val advancementId: String,
    ): BingoObjective() {

        override val conflictsWithObjectives: Set<String>
            get() = super.conflictsWithObjectives + advancementId

        @Transient
        var advancement: IAdvancementHandle? = null
    }

    @Serializable
    @SerialName("one_of")
    data class OneOfEntry(
        override val id: String,
        override val data: ObjectiveData.OneOf,
        val oneOfObjectives: Set<String>,
    ): BingoObjective() {
        override val dependsOnObjectives: Set<String>
            get() = super.dependsOnObjectives + oneOfObjectives

        override val conflictsWithObjectives: Set<String>
            get() = super.conflictsWithObjectives + oneOfObjectives
    }

    @Serializable
    @SerialName("all_of")
    data class AllOfEntry(
        override val id: String,
        override val data: ObjectiveData.AllOf,
        val allOfObjectives: Set<String>,
    ): BingoObjective() {
        override val dependsOnObjectives: Set<String>
            get() = super.dependsOnObjectives + allOfObjectives

        override val conflictsWithObjectives: Set<String>
            get() = super.conflictsWithObjectives + allOfObjectives
    }

    @Serializable
    @SerialName("inverse")
    data class InverseEntry(
        override val id: String,
        override val data: ObjectiveData.Inverse,
        val inverseObjective: String,
    ): BingoObjective() {
        override val dependsOnObjectives: Set<String>
            get() = super.dependsOnObjectives + inverseObjective

        override val conflictsWithObjectives: Set<String>
            get() = super.conflictsWithObjectives + inverseObjective

        val permanent get() = data.permanent

        val playersOnceAchieved = mutableSetOf<UuidAsString>()

        override fun removePlayer(uuid: UUID) {
            super.removePlayer(uuid)
            playersOnceAchieved.remove(uuid)
        }

        override fun removeTeam(team: BingoTeam) {
            super.removeTeam(team)

            for (player in team.players) {
                playersOnceAchieved.remove(player.uuid)
            }
        }
    }

    @Serializable
    @SerialName("opponent")
    data class OpponentEntry(
        override val id: String,
        override val data: ObjectiveData.Opponent,
        val opponentObjective: String,
    ): BingoObjective() {
        override val dependsOnObjectives: Set<String>
            get() = super.dependsOnObjectives + opponentObjective

        override val conflictsWithObjectives: Set<String>
            get() = super.conflictsWithObjectives + opponentObjective

        val permanent get() = data.permanent
    }

    @Serializable
    @SerialName("stats")
    data class StatsEntry(
        override val id: String,
        override val data: ObjectiveData.Stats,
        val valueMin: Int,
        val valueMax: Int
    ): BingoObjective() {

        override val conflictsWithObjectives: Set<String>
            get() = super.conflictsWithObjectives + listOfNotNull(data.statName)

        val valueRange get() = valueMin..valueMax

        /**
         * Records the stats that each player has when first entering the game
         * (workaround to avoid resetting stats each time a game starts)
         */
        val baseStats = mutableMapOf<UuidAsString, Int>()

        @Transient
        var stat: IStatHandle? = null

        override fun removePlayer(uuid: UUID) {
            super.removePlayer(uuid)
            baseStats.remove(uuid)
        }

        override fun removeTeam(team: BingoTeam) {
            super.removeTeam(team)

            for (player in team.players) {
                baseStats.remove(player.uuid)
            }
        }
    }

    @Serializable
    @SerialName("scoreboard")
    data class ScoreboardEntry(
        override val id: String,
        override val data: ObjectiveData.Scoreboard,
        val valueMin: Int,
        val valueMax: Int
    ): BingoObjective() {

        val valueRange get() = valueMin..valueMax

        /**
         * Records the scores that each player has when first entering the game
         * (workaround to avoid resetting stats each time a game starts)
         */
        val baseScores = mutableMapOf<UuidAsString, Int>()

        @Transient
        var scoreboard: IObjectiveHandle? = null

        override fun removePlayer(uuid: UUID) {
            super.removePlayer(uuid)
            baseScores.remove(uuid)
        }

        override fun removeTeam(team: BingoTeam) {
            super.removeTeam(team)

            for (player in team.players) {
                baseScores.remove(player.uuid)
            }
        }
    }
}