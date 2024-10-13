package me.jfenn.bingo.api.particle

import me.jfenn.bingo.api.IPlayerHandle
import me.jfenn.bingo.common.map.Color
import org.joml.Vector3d

interface IParticleFactory {
    fun createDustParticle(color: Color, scale: Float): IParticle
}

interface IParticle {
    fun spawn(player: IPlayerHandle, position: Vector3d, count: Int, delta: Vector3d, speed: Double)
}
