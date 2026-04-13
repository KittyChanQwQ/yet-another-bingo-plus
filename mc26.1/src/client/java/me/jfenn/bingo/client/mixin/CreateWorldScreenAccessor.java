package me.jfenn.bingo.client.mixin;

import com.mojang.datafixers.util.Pair;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.world.level.WorldDataConfiguration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.nio.file.Path;
import java.util.function.Consumer;

@Mixin(CreateWorldScreen.class)
public interface CreateWorldScreenAccessor {
    @Invoker("getOrCreateTempDataPackDir")
    Path invokeGetDataPackTempDir();
    @Invoker("getDataPackSelectionSettings")
    Pair<Path, PackRepository> invokeGetScannedPack(WorldDataConfiguration dataConfiguration);
    @Invoker("tryApplyNewDataPacks")
    void invokeApplyDataPacks(PackRepository dataPackManager, boolean fromPackScreen, Consumer<WorldDataConfiguration> configurationSetter);
    @Invoker("applyNewPackConfig")
    void invokeValidateDataPacks(PackRepository dataPackManager, WorldDataConfiguration dataConfiguration, Consumer<WorldDataConfiguration> configurationSetter);
}
