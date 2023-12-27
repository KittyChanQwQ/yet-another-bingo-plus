package me.jfenn.bingo.mixin;

import me.jfenn.bingo.common.ConstantsKt;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.world.WorldListWidget;
import net.minecraft.world.level.storage.LevelStorage;
import net.minecraft.world.level.storage.LevelSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Deletes old BINGO worlds whenever the world list screen is opened
 */
@Mixin(WorldListWidget.class)
public abstract class WorldListWidgetMixin {

    @Unique
    private final Logger log = LoggerFactory.getLogger(WorldListWidgetMixin.class);

    @Inject(
            method = "loadLevels()Ljava/util/concurrent/CompletableFuture;",
            at = @At(
                value = "RETURN"
            ),
            cancellable = true
    )
    private void loadLevels(
            CallbackInfoReturnable<CompletableFuture<List<LevelSummary>>> ci
    ) {
        MinecraftClient client = MinecraftClient.getInstance();

        var future = ci.getReturnValue()
                .thenApply((list) -> {
                    var worldsToDelete = list.stream()
                            .filter((summary) -> summary.getDisplayName().startsWith(ConstantsKt.getBINGO_WORLD_PREFIX()))
                            .toList();

                    LevelStorage levelStorage = client.getLevelStorage();
                    for (LevelSummary summary : worldsToDelete) {
                        log.info("Deleting BINGO world: " + summary.getName());

                        try (var session = levelStorage.createSessionWithoutSymlinkCheck(summary.getName())) {
                            session.deleteSessionLock();
                        } catch (Exception e) {
                            log.error("Error deleting BINGO world:", e);
                        }
                    }

                    // Return the worlds that were not deleted
                    return list.stream()
                            .filter((summary) -> !worldsToDelete.contains(summary))
                            .toList();
                });

        ci.setReturnValue(future);
    }

}
