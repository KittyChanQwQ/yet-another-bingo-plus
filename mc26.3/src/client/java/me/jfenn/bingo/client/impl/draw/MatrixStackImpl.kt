package me.jfenn.bingo.client.impl.draw

import me.jfenn.bingo.client.platform.renderer.IMatrixStack
import org.joml.Matrix3x2fStack

class MatrixStackImpl(
    private val matrices: Matrix3x2fStack,
) : IMatrixStack {
    override fun translate(x: Float, y: Float, z: Float) {
        matrices.translate(x, y)
    }
    override fun scale(x: Float, y: Float, z: Float) {
        matrices.scale(x, y)
    }
    override fun rotate(angle: Float) {
        matrices.rotate(angle)
    }
    override fun push() {
        matrices.pushMatrix()
    }
    override fun pop() {
        matrices.popMatrix()
    }
}