package me.jfenn.bingo.common.utils

import org.joml.Vector3d

operator fun Vector3d.plus(other: Vector3d): Vector3d {
    return add(other, Vector3d())
}

operator fun Vector3d.plusAssign(other: Vector3d) {
    add(other)
}
