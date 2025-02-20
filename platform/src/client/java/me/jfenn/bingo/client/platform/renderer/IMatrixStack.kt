package me.jfenn.bingo.client.platform.renderer

import org.joml.Quaternionf

interface IMatrixStack {
    fun translate(x: Float, y: Float, z: Float)
    fun scale(x: Float, y: Float, z: Float)
    fun multiply(quaternion: Quaternionf)
    fun push()
    fun pop()
}