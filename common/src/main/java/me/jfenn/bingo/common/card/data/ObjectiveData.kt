package me.jfenn.bingo.common.card.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import me.jfenn.bingo.common.card.objective.ObjectiveDisplay

@Serializable
sealed class ObjectiveData {

    var isRoot: Boolean = true
    val display: ObjectiveDisplay.Data? = null
    val conflictsWith: Set<String> = emptySet()
    abstract val permanent: Boolean

    open val innerObjectives: List<ObjectiveDataReference> = emptyList()
    fun innerObjectiveEntries(id: String): Map<String, ObjectiveDataReference> = buildMap {
        for ((i, reference) in innerObjectives.withIndex()) {
            when (reference) {
                is ObjectiveDataReference.Id -> put(reference.id, reference)
                is ObjectiveDataReference.Inline -> {
                    val inlineId = "$id->$i"
                    put(inlineId, reference)
                    putAll(reference.data.innerObjectiveEntries(inlineId))
                }
            }
        }
    }

    fun expandReference(id: String, reference: ObjectiveDataReference) : String {
        return when (reference) {
            is ObjectiveDataReference.Id -> reference.id
            is ObjectiveDataReference.Inline -> innerObjectiveEntries(id)
                .entries
                .find { it.value == reference }
                ?.key
                ?: error("Reference $reference does not exist inside objective '$id'!")
        }
    }

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
        val objectives: Set<ObjectiveDataReference>,
        override val permanent: Boolean = false,
    ) : ObjectiveData() {
        override val innerObjectives: List<ObjectiveDataReference>
            get() = objectives.toList()
    }

    @Serializable
    @SerialName("all_of")
    data class AllOf(
        val objectives: Set<ObjectiveDataReference>,
        override val permanent: Boolean = false,
    ) : ObjectiveData() {
        override val innerObjectives: List<ObjectiveDataReference>
            get() = objectives.toList()
    }

    @Serializable
    @SerialName("inverse")
    data class Inverse(
        val objective: ObjectiveDataReference,
        override val permanent: Boolean = true,
    ) : ObjectiveData() {
        override val innerObjectives: List<ObjectiveDataReference>
            get() = listOf(objective)
    }

    @Serializable
    @SerialName("opponent")
    data class Opponent(
        val objective: ObjectiveDataReference,
        override val permanent: Boolean = true,
    ) : ObjectiveData() {
        override val innerObjectives: List<ObjectiveDataReference>
            get() = listOf(objective)
    }

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