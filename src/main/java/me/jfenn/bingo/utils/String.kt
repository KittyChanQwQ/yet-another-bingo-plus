package me.jfenn.bingo.utils

fun String.formatTitle(): String {
    return this.replaceFirstChar { it.uppercase() }
        .replace(Regex("([a-z][A-Z])")) { it.value[0] + " " + it.value[1] }
}