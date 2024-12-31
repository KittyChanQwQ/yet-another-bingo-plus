package me.jfenn.bingo.common.menu

import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.common.utils.plus
import me.jfenn.bingo.generated.StringKey
import net.minecraft.text.Text
import org.joml.Vector3d
import java.util.*
import kotlin.time.Duration.Companion.minutes

const val MAX_TIMER_MINUTES: Int = 360

fun MenuComponent.registerTimer(
    position: Vector3d,
    width: Double,
    text: TextProvider = koinScope.get(),
    state: BingoState = koinScope.get(),
) {
    registerTitlePanel(
        position = position + Vector3d(0.0, MENU_LINE_HEIGHT + MENU_LINE_PADDING, 0.0),
        width = width,
        title = text.string(StringKey.OptionsTimeLimit),
    )

    var timerMinutes by propertyRef(state.options::timeLimit)
    val timerProp = DelegatedProperty(
        getter = { timerMinutes?.inWholeMinutes?.toInt() ?: 0 },
        setter = {
            timerMinutes = if (it > 0) it.minutes else null
        },
    )

    registerNumberInput(
        position = position,
        width = width,
        height = MENU_LINE_HEIGHT,
        valueProp = timerProp,
        step = 15,
        minValueProp = ConstantProperty(0),
        maxValueProp = ConstantProperty(MAX_TIMER_MINUTES),
        format = { minutes ->
            minutes.takeIf { it > 0 }
                ?.let {
                    String.format(Locale.US, "%dh %02dm", minutes / 60, minutes % 60)
                }
                ?.let { Text.literal(it) }
                ?: text.string(StringKey.OptionsTimeLimitOff)
        }
    )
}
