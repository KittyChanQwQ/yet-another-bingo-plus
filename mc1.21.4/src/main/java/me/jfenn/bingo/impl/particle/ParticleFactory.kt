package me.jfenn.bingo.impl.particle

import me.jfenn.bingo.api.IPlayerHandle
import me.jfenn.bingo.api.particle.IParticle
import me.jfenn.bingo.api.particle.IParticleFactory
import me.jfenn.bingo.common.map.Color
import me.jfenn.bingo.impl.PlayerHandle
import net.minecraft.particle.DustParticleEffect
import net.minecraft.particle.ParticleEffect
import net.minecraft.server.world.ServerWorld
import org.joml.Vector3d

object ParticleFactory : IParticleFactory {
    override fun createDustParticle(color: Color, scale: Float): IParticle {
        val effect = DustParticleEffect(color.asInt, scale)
        return ParticleImpl(effect)
    }

    class ParticleImpl(
        private val effect: ParticleEffect
    ) : IParticle {
        override fun spawn(player: IPlayerHandle, position: Vector3d, count: Int, delta: Vector3d, speed: Double) {
            require(player is PlayerHandle)
            val serverWorld: ServerWorld = player.serverWorld
            serverWorld.spawnParticles(
                player.player,
                effect,
                false,
                true,
                position.x,
                position.y,
                position.z,
                count,
                delta.x,
                delta.y,
                delta.z,
                speed
            )
        }
    }
}
