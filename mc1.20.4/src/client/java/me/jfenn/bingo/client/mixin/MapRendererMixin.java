package me.jfenn.bingo.client.mixin;

import me.jfenn.bingo.common.map.MapItemService;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.MapRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.FilledMapItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.map.MapState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(MapRenderer.class)
public abstract class MapRendererMixin {

    @ModifyVariable(
            method = "draw",
            at = @At(value = "LOAD"),
            ordinal = 1,
            index = 5,
            name = "light",
            argsOnly = true
    )
    int getLight(int light, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int id, MapState state, boolean hidePlayerIcons) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null) {
            return light;
        }

        // Try to find the map item being drawn in the player's inventory
        PlayerInventory inventory = player.getInventory();
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.getStack(i);
            if (MapItemService.INSTANCE.isMapItem(stack)) {
                // If the map being drawn is a bingo map item...
                Integer mapId = FilledMapItem.getMapId(stack);
                if (mapId != null && mapId == id) {
                    // Render it at full brightness
                    return 0x00FF;
                }
            }
        }

        return light;
    }

}
