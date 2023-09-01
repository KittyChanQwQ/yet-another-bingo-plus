package me.jfenn.bingo.map

import org.apache.commons.imaging.color.ColorConversions
import kotlin.math.*

fun rgb(r: Int, g: Int, b: Int): Int {
    return -0x1000000 or (r shl 16 and 0x00FF0000) or (g shl 8 and 0x0000FF00) or (b and 0x000000FF)
}

data class Color(val r: Int, val g: Int, val b: Int, val a: Int = 255) {
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

    val asByte by lazy {
        toInt().closestColorByte()
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

    companion object {
        fun fromInt(i: Int): Color {
            return Color(
                (i ushr 16) and 0xFF,
                (i ushr 8) and 0xFF,
                i and 0xFF,
                (i ushr 24) and 0xFF,
            )
        }
    }
}

/**
 * Computes the visual "distance" between two color int values
 * using a low-cost approximation of visual color space:
 * https://stackoverflow.com/a/9085524
 */
fun Int.colorDistance(other: Int): Double {
    val (r, g, b, _) = Color.fromInt(this)
    val (r2, g2, b2, _) = Color.fromInt(other)
    val rMean = (r + r2) / 2
    val rDiff = r - r2
    val gDiff = g - g2
    val bDiff = b - b2
    return sqrt((((512 + rMean) * rDiff * rDiff) shr 8) + (4 * gDiff * gDiff) + (((767 - rMean) * bDiff * bDiff) shr 8).toDouble())
}

/**
 * Computes the visual "distance" between two color int values
 * using the CMC l:c algorithm:
 * https://en.wikipedia.org/wiki/Color_difference#CMC_l:c_(1984)
 */
fun Int.colorDistanceCMC1984(other: Int): Double {
    val color1Lab = ColorConversions.convertRGBtoXYZ(this)
        .let { ColorConversions.convertXYZtoCIELab(it) }
    val color1Lch = ColorConversions.convertCIELabtoCIELCH(color1Lab)

    val color2Lab = ColorConversions.convertRGBtoXYZ(other)
        .let { ColorConversions.convertXYZtoCIELab(it) }
    val color2Lch = ColorConversions.convertCIELabtoCIELCH(color2Lab)

    // Ratio of l:c (lightness/chroma) - using 1:1
    val l = 1
    val c = 2

    val T = when {
        (color1Lch.h + 360.0) % 360.0 in 165.0..345.0 -> 0.56 + abs(0.2 * cos(color1Lch.h + 168))
        else -> 0.36 + abs(0.4 * cos(color1Lch.h + 35))
    }

    val F = sqrt(color1Lch.C.pow(4) / (color1Lch.C.pow(4) + 1900))

    val S_C = (0.0638 * color1Lch.C) / (1 + 0.0131 * color1Lch.C)

    val S_L = when {
        color1Lch.L < 16 -> 0.511
        else -> (0.040975 * color1Lch.L) / (1 + 0.01765 * color1Lch.L)
    }

    val S_H = S_C * (F*T + 1 - F)

    val deltaH = sqrt(
        (color1Lab.a - color2Lab.a).pow(2)
        + (color1Lab.b - color2Lab.b).pow(2)
        - (color1Lch.C - color2Lch.C).pow(2)
    )

    val deltaE = sqrt(
        ((color2Lch.L - color1Lch.L) / (l * S_L)).pow(2)
            + ((color2Lch.C - color1Lch.C) / (c * S_C)).pow(2)
            + (deltaH / S_H).pow(2)
    )

    return deltaE
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

    Color.fromInt(baseColor).map { it * multiplier / 255 }.toInt()
}

fun Int.closestColorByte(): Byte {
    // colorDistanceCMC1984 fails on comparisons to #000... so we're doing this manually
    if (Color.fromInt(this).let { (r, g, b) -> r == 0 && g == 0 && b == 0 })
        return (29 * 4).toUByte().toByte()

    return (4 until MAP_COLORS.size)
        .minByOrNull { i ->
            this.colorDistance(MAP_COLORS[i]) + 0.5 * this.colorDistanceCMC1984(MAP_COLORS[i])
        }
        .let { it ?: (29 * 4) }
        .toUByte()
        .toByte()
}
