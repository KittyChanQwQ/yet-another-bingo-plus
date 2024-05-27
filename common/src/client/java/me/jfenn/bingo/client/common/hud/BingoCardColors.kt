package me.jfenn.bingo.client.common.hud

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import me.jfenn.bingo.common.team.BingoTeam
import me.jfenn.bingo.common.team.BingoTeamKey
import me.jfenn.bingo.common.map.Color
import kotlin.reflect.KProperty1

@Serializable
data class BingoCardColors(
    val default: Team = Team(),
    val teams: Map<String, Team> = emptyMap(),
) {

    companion object {
        private val TEAM_KEYS = BingoTeam.TEAM_BLOCKS.values.map { it.key } + BingoTeamKey("preview")
        val DEFAULTS = TeamColors()
    }

    @Serializable
    data class Team(
        val tileAchievedColor: String? = null,
        val tileFlashingColor: String? = null,
        val backgroundColor: String? = null,
        val textColor: String? = null,
        val outlineColor: String? = null,
    )

    class TeamColors (
        val tileAchievedColor: Color = Color.fromInt(0xff55ff55.toInt()),
        val tileFlashingColor: Color = Color.fromInt(0xffb24cd8.toInt()),
        val backgroundColor: Color = Color.BLACK,
        val textColor: Color = Color.BLACK,
        val outlineColor: Color = Color.TRANSPARENT,
    )

    @Transient
    private val teamsByKey = TEAM_KEYS.associateWith { teams[it.label] ?: default }

    private fun <V> getValue(team: Team?, property: KProperty1<Team, V>): V =
        team?.let { property.get(it) }
            ?: property.get(default)

    @Transient
    val teamColors = teamsByKey.mapValues { (_, team) ->
        TeamColors(
            tileAchievedColor = getValue(team, Team::tileAchievedColor)
                ?.let { Color.fromString(it) }
                ?: DEFAULTS.tileAchievedColor,
            tileFlashingColor = getValue(team, Team::tileFlashingColor)
                ?.let { Color.fromString(it) }
                ?: DEFAULTS.tileFlashingColor,
            backgroundColor = getValue(team, Team::backgroundColor)
                ?.let { Color.fromString(it) }
                ?: DEFAULTS.backgroundColor,
            textColor = getValue(team, Team::textColor)
                ?.let { Color.fromString(it) }
                ?: DEFAULTS.textColor,
            outlineColor = getValue(team, Team::outlineColor)
                ?.let { Color.fromString(it) }
                ?: DEFAULTS.outlineColor,
        )
    }

}