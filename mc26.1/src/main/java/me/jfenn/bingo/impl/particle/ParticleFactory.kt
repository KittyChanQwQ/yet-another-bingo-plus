package me.jfenn.bingo.impl.particle

import me.jfenn.bingo.impl.PlayerHandle
import me.jfenn.bingo.platform.IPlayerHandle
import me.jfenn.bingo.platform.particle.IParticle
import me.jfenn.bingo.platform.particle.IParticleFactory
import net.minecraft.core.particles.DustParticleOptions
import net.minecraft.core.particles.ParticleOptions
import net.minecraft.server.level.ServerLevel
import org.joml.Vector3d

object ParticleFactory : IParticleFactory {
    override fun createDustParticle(color: Int, scale: Float): IParticle {
        val effect = DustParticleOptions(color, scale)
        return ParticleImpl(effect)
    }

    class ParticleImpl(
        private val effect: ParticleOptions
    ) : IParticle {
        override fun spawn(player: IPlayerHandle, position: Vector3d, count: Int, delta: Vector3d, speed: Double) {
            require(player is PlayerHandle)
            val serverWorld: ServerLevel = player.serverWorld
            serverWorld.sendParticles(
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
