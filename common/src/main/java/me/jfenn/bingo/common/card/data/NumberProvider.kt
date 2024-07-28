package me.jfenn.bingo.common.card.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import me.jfenn.bingo.common.card.CardGeneratorState

@Serializable
sealed class NumberProvider {
    @SerialName("random")
    class Random(
        val min: Int = 1,
        val max: Int = 64,
    ) : NumberProvider() {
        override fun resolve(state: CardGeneratorState): Int {
            return state.random.nextInt(min, max + 1)
        }
    }

    @SerialName("constant")
    class Constant(val value: Int) : NumberProvider() {
        override fun resolve(state: CardGeneratorState) = value
    }

    abstract fun resolve(state: CardGeneratorState): Int
}