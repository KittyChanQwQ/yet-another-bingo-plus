package me.jfenn.bingo.common

import assertk.Assert
import assertk.assertions.isEqualTo

fun Assert<String>.isEqualToFile(path: String) = isEqualTo(
    this::class.java.getResource(path)!!.readText()
)
