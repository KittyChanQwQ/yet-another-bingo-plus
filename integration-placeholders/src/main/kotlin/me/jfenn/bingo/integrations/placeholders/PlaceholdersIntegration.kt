package me.jfenn.bingo.integrations.placeholders

import eu.pb4.placeholders.api.PlaceholderContext
import eu.pb4.placeholders.api.PlaceholderResult
import eu.pb4.placeholders.api.Placeholders
import me.jfenn.bingo.api.BingoApi
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.*

class PlaceholdersIntegration {

    private val log = LoggerFactory.getLogger("bingo")

    private fun Duration.formatString(): String {
        val sign = if (isNegative) "-" else ""
        val value = if (isNegative) negated() else this
        val days = value.toDays()
        val hours = value.toHoursPart()
        val minutes = value.toMinutesPart()
        val seconds = value.toSecondsPart()

        return String.format(
            Locale.US,
            "%s"
                    + "%2\$dd ".takeIf { days > 0 }.orEmpty()
                    + "%3\$dh".takeIf { hours > 0 }.orEmpty()
                    + "%4$02dm %5$02ds",
            sign,
            days,
            hours,
            minutes,
            seconds,
        )
    }

    fun registerPlaceholders() {
        registerPlaceholder("time") { _, _ ->
            Text.literal(BingoApi.game?.time?.formatString().orEmpty())
        }

        registerPlaceholder("time_remaining") { _, _ ->
            Text.literal(BingoApi.game?.timeRemaining?.formatString().orEmpty())
        }
    }

    private fun registerPlaceholder(id: String, callback: (ctx: PlaceholderContext, arg: String?) -> Text) {
        Placeholders.register(Identifier.of("yet-another-minecraft-bingo", id)!!) { ctx, arg ->
            try {
                PlaceholderResult.value(callback(ctx, arg))
            } catch (e: Throwable) {
                log.error("Error obtaining placeholder $id:", e)
                PlaceholderResult.invalid(e.message)
            }
        }
    }
}