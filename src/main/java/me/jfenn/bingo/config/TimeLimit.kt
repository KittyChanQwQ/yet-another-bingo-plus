package me.jfenn.bingo.config

enum class TimeLimit(val minutes: Int) {
    `30_MINUTES`(30),
    ONE_HOUR(60),
    TWO_HOURS(120);

    val seconds: Int get() = minutes * 60
}
