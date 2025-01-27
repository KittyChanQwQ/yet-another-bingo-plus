package me.jfenn.bingo.common.text

import assertk.assertThat
import assertk.assertions.isEqualTo
import io.mockk.mockk
import me.jfenn.bingo.generated.StringKey
import me.jfenn.bingo.platform.ITextSerializer
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory

class TextProviderTest {

    private val mockTextSerializer = mockk<ITextSerializer>()

    private val textProvider = TextProvider(
        LoggerFactory.getLogger(this::class.java),
        textSerializer = mockTextSerializer,
    )

    @Test
    fun rawString() {
        val text = textProvider.raw(StringKey.WorldNewGame)
        assertThat(text).isEqualTo("New Game")
    }

    @Test
    fun rawStringWithFormat() {
        val text = textProvider.raw(StringKey.GameItemCount, 3)
        assertThat(text).isEqualTo("3 items")
    }

    @Test
    fun itemCountSingular() {
        val text = textProvider.itemCount(1)
        assertThat(text.string).isEqualTo("1 item")
    }

    @Test
    fun itemCountPlural() {
        val text = textProvider.itemCount(2)
        assertThat(text.string).isEqualTo("2 items")
    }
}