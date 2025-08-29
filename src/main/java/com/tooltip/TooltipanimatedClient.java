package com.tooltip;

import com.tooltip.config.TooltipConfig;
import com.tooltip.render.TooltipRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;

public class TooltipanimatedClient implements ClientModInitializer {

    private TooltipRenderer tooltipRenderer;

    @Override
    public void onInitializeClient() {
        // Load configuration
        TooltipConfig.load();

        // Create renderer instance
        this.tooltipRenderer = new TooltipRenderer(MinecraftClient.getInstance());

        // Register HUD render callback
        HudRenderCallback.EVENT.register((drawContext, tickCounter) -> {
            tooltipRenderer.onHudRender(drawContext, tickCounter);
        });
    }
}