@file:Suppress("unused")

package me.jfenn.bingo.common.utils

import assertk.assertThat
import assertk.assertions.isEqualTo
import me.jfenn.bingo.common.test.BaseGameTest
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest
import net.minecraft.test.GameTest
import net.minecraft.test.TestContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class DurationTest : BaseGameTest() {

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE)
    fun formatString_hours(context: TestContext) {
        val duration = 3.hours + 10.minutes + 1.seconds
        assertThat(duration.formatString()).isEqualTo("3h 10m 01s")
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE)
    fun formatString_seconds(context: TestContext) {
        val duration = 1.seconds + 123.milliseconds
        assertThat(duration.formatString()).isEqualTo("00m 01s")
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE)
    fun formatString_millis(context: TestContext) {
        val duration = 123.milliseconds
        assertThat(duration.formatString()).isEqualTo("00m 00s")
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE)
    fun formatString_negative(context: TestContext) {
        val duration = Duration.ZERO - 3.minutes
        assertThat(duration.formatString()).isEqualTo("-03m 00s")
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE)
    fun formatStringSmall_hours(context: TestContext) {
        val duration = 3.hours + 10.minutes + 1.seconds
        assertThat(duration.formatStringSmall()).isEqualTo("3h")
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE)
    fun formatStringSmall_seconds(context: TestContext) {
        val duration = 1.seconds + 123.milliseconds
        assertThat(duration.formatStringSmall()).isEqualTo("1s")
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE)
    fun formatStringSmall_millis(context: TestContext) {
        val duration = 123.milliseconds
        assertThat(duration.formatStringSmall()).isEqualTo("0.123s")
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE)
    fun formatStringSmall_negative(context: TestContext) {
        val duration = Duration.ZERO - 3.minutes
        assertThat(duration.formatStringSmall()).isEqualTo("-3m")
    }

}