package me.jfenn.bingo.common.utils

import net.minecraft.nbt.NbtDouble
import net.minecraft.nbt.NbtFloat
import net.minecraft.nbt.NbtList
import net.minecraft.util.math.Vec3d

fun Vec3d.toNbt(): NbtList {
    return NbtList().apply {
        add(NbtDouble.of(x))
        add(NbtDouble.of(y))
        add(NbtDouble.of(z))
    }
}

fun List<Float>.toNbt(): NbtList {
    val list = NbtList()
    forEach { list.add(NbtFloat.of(it)) }
    return list
}
