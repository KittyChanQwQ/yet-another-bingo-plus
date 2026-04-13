package me.jfenn.bingo.platform.block

import org.joml.Vector3d

data class AABB(
    val min: Vector3d,
    val max: Vector3d,
)