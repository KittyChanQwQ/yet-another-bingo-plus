package me.jfenn.bingo.common.map

import me.jfenn.bingo.common.team.BingoPlayerProfile
import me.jfenn.bingo.common.team.BingoTeamKey
import net.minecraft.text.Text
import net.minecraft.util.Formatting

data class CardView(
    val teamKey: BingoTeamKey?,
    val teamColor: Formatting? = null,
    val teamName: Text = Text.empty(),
    val tiles: MutableList<CardTile>,
    val players: List<BingoPlayerProfile>
) {

    fun tile(x: Int, y: Int): CardTile? {
        assert(x in 0 until 5) { "x=$x is not within the required 0..4 range" }
        assert(y in 0 until 5) { "y=$y is not within the required 0..4 range" }

        return tiles.getOrNull(x + y * 5)
    }

}