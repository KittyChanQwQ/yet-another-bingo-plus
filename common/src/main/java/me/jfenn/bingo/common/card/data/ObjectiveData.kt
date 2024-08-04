package me.jfenn.bingo.common.card.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import me.jfenn.bingo.common.card.objective.ObjectiveDisplay

@Serializable
sealed class ObjectiveData {

    val display: ObjectiveDisplay.Data? = null
    val conflictsWith: Set<String> = emptySet()

    @Serializable
    @SerialName("item")
    data class Item(
        val item: String,
        val count: NumberProviderPolymorphic = NumberProvider.Constant(1),
        val nbt: String? = null,
        val components: Map<String, JsonElement?>? = null,
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
        val permanent: Boolean = true,
    ) : ObjectiveData()

    @Serializable
    @SerialName("opponent")
    data class Opponent(
        val objective: String,
        val permanent: Boolean = true,
    ) : ObjectiveData()

    @Serializable
    @SerialName("stats")
    data class Stats(
        val statType: String,
        val statName: String? = null,
        val min: NumberProviderPolymorphic = NumberProvider.Constant(1),
        val max: NumberProviderPolymorphic = NumberProvider.Constant(Int.MAX_VALUE),
    ) : ObjectiveData()

}