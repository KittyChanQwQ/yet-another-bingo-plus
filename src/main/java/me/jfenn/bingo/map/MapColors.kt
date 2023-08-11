package me.jfenn.bingo.map

import kotlin.math.pow
import kotlin.math.sqrt

fun rgb(r: Int, g: Int, b: Int): Int {
    return -0x1000000 or (r shl 16 and 0x00FF0000) or (g shl 8 and 0x0000FF00) or (b and 0x000000FF)
}

data class Color(val r: Int, val g: Int, val b: Int, val a: Int) {
    fun map(mapper: (Int) -> Int): Color {
        return Color(
            mapper(this.r),
            mapper(this.g),
            mapper(this.b),
            this.a
        )
    }

    fun toInt(): Int {
        return rgb(r, g, b)
    }

    fun mix(other: Color): Color {
        val alpha: Float = other.a / 255f
        return Color(
            ((r * (1-alpha)) + (other.r * alpha)).toInt(),
            ((g * (1-alpha)) + (other.g * alpha)).toInt(),
            ((b * (1-alpha)) + (other.b * alpha)).toInt(),
            a,
        )
    }
}

fun Int.rgba(): Color {
    return Color(
        (this ushr 16) and 0xFF,
        (this ushr 8) and 0xFF,
        this and 0xFF,
        (this ushr 24) and 0xFF,
    )
}

fun Int.colorDistance(other: Int): Double {
    val (r, g, b, _) = rgba()
    val (r2, g2, b2, _) = other.rgba()
    val rMean = (r + r2) / 2
    val rDiff = r - r2;
    val gDiff = g - g2;
    val bDiff = b - b2;
    return sqrt((((512 + rMean) * rDiff * rDiff) shr 8) + (4 * gDiff * gDiff) + (((767 - rMean) * bDiff * bDiff) shr 8).toDouble())

    /*
    val weightR = (2 + rMean) / 256.0
    val weightG = 4.0
    val weightB = 2 + (255 - rMean) / 256.0
    return (
        weightR * (r - r2).toDouble().pow(2)
            + weightG * (g - g2).toDouble().pow(2)
            + weightB * (b - b2).toDouble().pow(2)
    )
     */
}

val MAP_COLORS_BASE = arrayOf<Int>(
    0,
    rgb(127, 178, 56),
    rgb(247, 233, 163),
    rgb(199, 199, 199),
    rgb(255, 0, 0),
    rgb(160, 160, 255),
    rgb(167, 167, 167),
    rgb(0, 124, 0),
    rgb(255, 255, 255),
    rgb(164, 168, 184),
    rgb(151, 109, 77),
    rgb(112, 112, 112),
    rgb(64, 64, 255),
    rgb(143, 119, 72),
    rgb(255, 252, 245),
    rgb(216, 127, 51),
    rgb(178, 76, 216),
    rgb(102, 153, 216),
    rgb(229, 229, 51),
    rgb(127, 204, 25),
    rgb(242, 127, 165),
    rgb(76, 76, 76),
    rgb(153, 153, 153),
    rgb(76, 127, 153),
    rgb(127, 63, 178),
    rgb(51, 76, 178),
    rgb(102, 76, 51),
    rgb(102, 127, 51),
    rgb(153, 51, 51),
    rgb(25, 25, 25),
    rgb(250, 238, 77),
    rgb(92, 219, 213),
    rgb(74, 128, 255),
    rgb(0, 217, 58),
    rgb(129, 86, 49),
    rgb(112, 2, 0),
    rgb(209, 177, 161),
    rgb(159, 82, 36),
    rgb(149, 87, 108),
    rgb(112, 108, 138),
    rgb(186, 133, 36),
    rgb(103, 117, 53),
    rgb(160, 77, 78),
    rgb(57, 41, 35),
    rgb(135, 107, 98),
    rgb(87, 92, 92),
    rgb(122, 73, 88),
    rgb(76, 62, 92),
    rgb(76, 50, 35),
    rgb(76, 82, 42),
    rgb(142, 60, 46),
    rgb(37, 22, 16),
    rgb(189, 48, 49),
    rgb(148, 63, 97),
    rgb(92, 25, 29),
    rgb(22, 126, 134),
    rgb(58, 142, 140),
    rgb(86, 44, 62),
    rgb(20, 180, 133),
    rgb(100, 100, 100),
    rgb(216, 175, 147),
    rgb(127, 167, 150),
)

val MAP_COLORS = Array<Int>(248) { index ->
    val baseIndex = index / 4
    val baseColor = MAP_COLORS_BASE[baseIndex]
    val multiplier = when (index % 4) {
        0 -> 180
        1 -> 220
        3 -> 135
        else -> 255
    }

    baseColor.rgba().map { it * multiplier / 255 }.toInt()
}

fun Int.closestColorByte(): Byte {
    var index = 0
    var best = Double.MAX_VALUE;

    for (i in 4 until MAP_COLORS.size) {
        val distance = this.colorDistance(MAP_COLORS[i])
        if (distance < best) {
            best = distance
            index = i
        }
    }

    return index.toUByte().toByte()
}
