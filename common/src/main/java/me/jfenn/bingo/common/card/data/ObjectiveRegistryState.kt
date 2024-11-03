package me.jfenn.bingo.common.card.data

class ObjectiveRegistryState {
    var objectives = mapOf<String, ObjectiveData>()

    val rootObjectives: Sequence<Map.Entry<String, ObjectiveData>>
        get() = objectives
            .asSequence()
            .filter { it.value.isRoot }
}