package me.jfenn.bingo.utils.text

import me.jfenn.bingo.common.utils.text.TextColor
import me.jfenn.bingo.common.utils.text.TextInfo
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class TextInfoTest {

    @Test
    fun `Serializes text color values correctly`() {
        val text = TextInfo.literal("Hello world!")
            .formatted(TextColor.RED)

        assertEquals(
            """
                {"text":"Hello world!","color":"red"}
            """.trimIndent(),
            TextInfo.toJsonString(text),
        )
    }

}