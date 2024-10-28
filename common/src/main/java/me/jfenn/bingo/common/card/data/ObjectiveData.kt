package me.jfenn.bingo.common.card.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import me.jfenn.bingo.common.card.objective.ObjectiveDisplay

@Serializable
sealed class ObjectiveData {

    val display: ObjectiveDisplay.Data? = null
    val conflictsWith: Set<String> = emptySet()
    abstract val permanent: Boolean

    @Serializable
    @SerialName("item")
    data class Item(
        val item: String,
        val count: NumberProviderPolymorphic = NumberProvider.Constant(1),
        val nbt: String? = null,
        val components: Map<String, JsonElement?>? = null,
        override val permanent: Boolean = false,
    ) : ObjectiveData()

    @Serializable
    @SerialName("advancement")
    data class Advancement(
        val advancement: String,
        override val permanent: Boolean = false,
    ) : ObjectiveData()

    @Serializable
    @SerialName("one_of")
    data class OneOf(
        val objectives: Set<String>,
        override val permanent: Boolean = false,
    ) : ObjectiveData()

    @Serializable
    @SerialName("all_of")
    data class AllOf(
        val objectives: Set<String>,
        override val permanent: Boolean = false,
    ) : ObjectiveData()

    @Serializable
    @SerialName("inverse")
    data class Inverse(
        val objective: String,
        override val permanent: Boolean = true,
    ) : ObjectiveData()

    @Serializable
    @SerialName("opponent")
    data class Opponent(
        val objective: String,
        override val permanent: Boolean = true,
    ) : ObjectiveData()

    @Serializable
    @SerialName("stats")
    data class Stats(
        val statType: String,
        val statName: String? = null,
        val min: NumberProviderPolymorphic = NumberProvider.Constant(1),
        val max: NumberProviderPolymorphic = NumberProvider.Constant(Int.MAX_VALUE),
        val relative: Boolean = true,
        override val permanent: Boolean = false,
    ) : ObjectiveData()

    @Serializable
    @SerialName("scoreboard")
    data class Scoreboard(
        val scoreboardName: String,
        val min: NumberProviderPolymorphic = NumberProvider.Constant(1),
        val max: NumberProviderPolymorphic = NumberProvider.Constant(Int.MAX_VALUE),
        val relative: Boolean = false,
        override val permanent: Boolean = false,
    ) : ObjectiveData()

}