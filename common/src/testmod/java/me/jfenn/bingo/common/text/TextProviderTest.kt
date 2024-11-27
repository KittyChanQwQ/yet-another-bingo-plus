@file:Suppress("unused")

package me.jfenn.bingo.common.text

import assertk.assertThat
import assertk.assertions.isEqualTo
import io.mockk.mockk
import me.jfenn.bingo.platform.ITextSerializer
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.common.test.BaseGameTest
import me.jfenn.bingo.generated.StringKey
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest
import net.minecraft.test.GameTest
import net.minecraft.test.TestContext
import org.slf4j.LoggerFactory

class TextProviderTest : BaseGameTest() {

    private val mockTextSerializer = mockk<ITextSerializer>()

    private val textProvider = TextProvider(
        LoggerFactory.getLogger(this::class.java),
        textSerializer = mockTextSerializer,
    )

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE)
    fun rawString(context: TestContext) {
        val text = textProvider.raw(StringKey.WorldNewGame)
        assertThat(text).isEqualTo("New Game")
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE)
    fun rawStringWithFormat(context: TestContext) {
        val text = textProvider.raw(StringKey.GameItemCount, 3)
        assertThat(text).isEqualTo("3 items")
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE)
    fun itemCountSingular(context: TestContext) {
        val text = textProvider.itemCount(1)
        assertThat(text.string).isEqualTo("1 item")
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE)
    fun itemCountPlural(context: TestContext) {
        val text = textProvider.itemCount(2)
        assertThat(text.string).isEqualTo("2 items")
    }
}