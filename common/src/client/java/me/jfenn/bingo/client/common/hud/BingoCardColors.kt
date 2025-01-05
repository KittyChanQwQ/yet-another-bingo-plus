package me.jfenn.bingo.client.common.hud

import kotlinx.serialization.Serializable
import me.jfenn.bingo.common.map.Color
import me.jfenn.bingo.common.team.BingoTeamKey
import net.minecraft.util.Formatting
import net.minecraft.util.Identifier
import kotlin.reflect.KProperty1

@Serializable
internal data class BingoCardColors(
    val default: Team = Team(),
    val teams: Map<String, Team> = emptyMap(),
) {

    companion object {
        val DEFAULTS = TeamColors()
    }

    @Serializable
    data class Team(
        val tileAchievedColor: String? = null,
        val tileFlashingColor: String? = null,
        val tileProgressColor: String? = null,
        val backgroundColor: String? = null,
        val textColor: String? = null,
        val outlineColor: String? = null,
        val cardTexture: String? = null,
        val cardTextureGui: String? = null,
        val cardTextureOutline: String? = null,
    )

    class TeamColors(
        val tileAchievedColor: Color = Color.fromInt(0xff55ff55.toInt()),
        val tileFlashingColor: Color = Color.fromInt(0xffb24cd8.toInt()),
        val tileProgressColor: Color = Color.fromInt(0xff309f30.toInt()),
        val backgroundColor: Color = Color.BLACK,
        val textColor: Color = Color.BLACK,
        val outlineColor: Color = Color.TRANSPARENT,
        val cardTexture: Identifier = Identifier.of("minecraft", "bingo/card_preview")!!,
        val cardTextureGui: Identifier = Identifier.of("minecraft", "bingo/card_preview")!!,
        val cardTextureOutline: Identifier = Identifier.of("minecraft", "bingo/card_outline")!!,
    )

    private fun <V> getValue(team: Team?, property: KProperty1<Team, V>): V =
        team?.let { property.get(it) }
            ?: property.get(default)

    private fun String.parseIdentifier(): Identifier =
        Identifier.of(substringBefore(':'), substringAfter(':'))!!

    fun getTeamColors(
        teamKey: BingoTeamKey,
        formatting: Formatting?,
    ): TeamColors {
        val colors = teams[teamKey.label]
            ?: formatting?.name?.lowercase()?.let { teams[it] }
            ?: default

        return TeamColors(
            tileAchievedColor = getValue(colors, Team::tileAchievedColor)
                ?.let { Color.fromString(it) }
                ?: DEFAULTS.tileAchievedColor,
            tileFlashingColor = getValue(colors, Team::tileFlashingColor)
                ?.let { Color.fromString(it) }
                ?: DEFAULTS.tileFlashingColor,
            tileProgressColor = getValue(colors, Team::tileProgressColor)
                ?.let { Color.fromString(it) }
                ?: DEFAULTS.tileProgressColor,
            backgroundColor = getValue(colors, Team::backgroundColor)
                ?.let { Color.fromString(it) }
                ?: DEFAULTS.backgroundColor,
            textColor = getValue(colors, Team::textColor)
                ?.let { Color.fromString(it) }
                ?: DEFAULTS.textColor,
            outlineColor = getValue(colors, Team::outlineColor)
                ?.let { Color.fromString(it) }
                ?: formatting?.colorValue?.let { Color.fromInt(it) }?.copy(a = 255)
                ?: DEFAULTS.outlineColor,
            cardTexture = getValue(colors, Team::cardTexture)
                ?.parseIdentifier()
                ?: DEFAULTS.cardTexture,
            cardTextureGui = getValue(colors, Team::cardTextureGui)
                ?.parseIdentifier()
                ?: getValue(colors, Team::cardTexture)?.parseIdentifier()
                ?: DEFAULTS.cardTextureGui,
            cardTextureOutline = getValue(colors, Team::cardTextureOutline)
                ?.parseIdentifier()
                ?: DEFAULTS.cardTextureOutline,
        )
    }

}