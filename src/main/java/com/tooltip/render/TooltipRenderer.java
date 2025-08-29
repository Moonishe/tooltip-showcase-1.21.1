package com.tooltip.render;

import com.tooltip.config.TooltipConfig;
import com.tooltip.render.animation.TooltipAnimationManager;
import com.tooltip.render.content.TooltipContentBuilder;
import com.tooltip.render.drawing.TooltipDrawer;
import com.tooltip.render.state.TooltipStateManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

import java.util.List;
import java.util.Objects;

public class TooltipRenderer {
    public static TooltipRenderer INSTANCE; // важно для миксина

    private final MinecraftClient client;
    private final TooltipConfig config;
    private final TooltipAnimationManager animationManager;
    private final TooltipContentBuilder contentBuilder;
    private final TooltipDrawer drawer;
    private final TooltipStateManager stateManager;

    private boolean lastHasTooltip = false;
    private ItemStack lastItem = ItemStack.EMPTY;

    private boolean uiPaused = false;
    private boolean skipDtOnce = false;

    public TooltipRenderer(MinecraftClient client) {
        this.client = Objects.requireNonNull(client, "MinecraftClient cannot be null");
        this.config = TooltipConfig.getInstance();
        this.animationManager = new TooltipAnimationManager();
        this.contentBuilder = new TooltipContentBuilder(client);
        this.drawer = new TooltipDrawer(client, config);
        this.stateManager = new TooltipStateManager();
        INSTANCE = this;
    }

    // вызывает ChatHudMixin, чтобы нарисовать тултип ПОД чатом
    public void renderUnderChat(DrawContext drawContext) {
        RenderTickCounter tickCounter = client.getRenderTickCounter();
        onHudRender(drawContext, tickCounter);
    }

    public void onHudRender(DrawContext drawContext, RenderTickCounter tickCounter) {
        if (client.player == null || client.world == null) {
            return;
        }

        boolean chatOpen = isChatScreenOpen();

        float dt = tickCounter.getTickDelta(true) * 0.05f;
        if (skipDtOnce) { dt = 0f; skipDtOnce = false; }
        dt = Math.min(dt, 0.05f);

        // чат — не пауза: рендерим, но замораживаем время анимаций
        if (chatOpen) dt = 0f;

        if (isPausedByUI()) {
            if (!uiPaused) { uiPaused = true; skipDtOnce = true; }
            return;
        } else if (uiPaused) {
            uiPaused = false;
            skipDtOnce = true;
        }

        ClientPlayerEntity player = client.player;
        ItemStack currentStack = player.getInventory().getMainHandStack();
        boolean hasTooltipNow = !currentStack.isEmpty();

        if (hasTooltipNow && !lastHasTooltip) {
            startTooltipAppear(currentStack);
        } else if (!hasTooltipNow && lastHasTooltip) {
            animationManager.fadeOutTooltip(dt);
        } else if (hasTooltipNow) {
            boolean itemChanged = !ItemStack.areEqual(currentStack, lastItem);
            if (itemChanged) {
                startTooltipAppear(currentStack);
            } else if (currentStack.isDamageable()) {
                boolean durabilityChanged = stateManager.checkDurabilityChange(currentStack);
                if (durabilityChanged) {
                    List<Text> newLines = contentBuilder.buildTooltipContent(currentStack, config);
                    stateManager.updateCache(currentStack, newLines);
                }
            }
        }

        animationManager.updateAnimations(dt);

        List<Text> linesToRender = stateManager.getCachedLines();
        if (!linesToRender.isEmpty() && animationManager.getAlpha() > 0.001f) {
            drawer.render(drawContext, linesToRender, animationManager, currentStack, dt);
        }

        if (!hasTooltipNow && animationManager.getAlpha() <= 0.001f) {
            stateManager.clear();
            lastItem = ItemStack.EMPTY;
        }

        lastHasTooltip = hasTooltipNow;
        if (hasTooltipNow) lastItem = currentStack.copy();
    }

    private void startTooltipAppear(ItemStack currentStack) {
        List<Text> newLines = contentBuilder.buildTooltipContent(currentStack, config);
        stateManager.updateCache(currentStack, newLines);
        stateManager.initializeDurability(currentStack);
        animationManager.resetTooltipAnimations();
        drawer.resetAnimation();
        lastItem = currentStack.copy();
    }

    private boolean isChatScreenOpen() {
        var screen = client.currentScreen;
        if (screen == null) return false;
        String name = screen.getClass().getName();
        String simple = screen.getClass().getSimpleName();
        return name.contains("ChatScreen") || simple.equalsIgnoreCase("ChatScreen");
    }

    private boolean isPausedByUI() {
        if (client.options.hudHidden) return true;
        var screen = client.currentScreen;
        if (screen == null) return false;
        return !isChatScreenOpen();
    }
}