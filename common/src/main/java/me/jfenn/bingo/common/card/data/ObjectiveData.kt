package me.jfenn.bingo.common.card.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import me.jfenn.bingo.common.card.objective.ObjectiveDisplay

@Serializable
sealed class ObjectiveData {

    val display: ObjectiveDisplay.Data? = null

    @Serializable
    @SerialName("item")
    data class Item(
        val item: String,
        val count: NumberProvider = NumberProvider.Constant(1),
    ) : ObjectiveData()

    @Serializable
    @SerialName("one_of")
    data class OneOf(
        val objectives: Set<String>,
    ) : ObjectiveData()

    @Serializable
    @SerialName("all_of")
    data class AllOf(
        val objectives: Set<String>,
    ) : ObjectiveData()

    @Serializable
    @SerialName("inverse")
    data class Inverse(
        val objective: String,
    ) : ObjectiveData()

}