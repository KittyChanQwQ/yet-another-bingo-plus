package me.jfenn.bingo.mixin;

import net.minecraft.world.level.levelgen.WorldGenSettings;
import net.minecraft.world.level.levelgen.WorldOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(WorldGenSettings.class)
public interface WorldGenSettingsAccessor {
    @Accessor("options")
    WorldOptions getWorldOptions();
    @Mutable @Accessor("options")
    void setWorldOptions(WorldOptions generatorOptions);
}
