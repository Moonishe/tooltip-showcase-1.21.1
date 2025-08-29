package com.tooltip.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;

public class HUDLayoutHelper {

    public static int computeBottomReservedHUD(MinecraftClient client) {
        int reserved = LayoutConstants.BASE_HOTBAR_OFFSET_Y; // 22

        PlayerEntity p = client.player;
        if (p == null) return reserved;

        int leftStack = 0;
        int playerHeartRows = computeHeartRows(p.getMaxHealth(), p.getAbsorptionAmount());
        if (playerHeartRows > 0) leftStack += playerHeartRows * LayoutConstants.HEART_ROW_HEIGHT;

        if (p.hasVehicle() && p.getVehicle() instanceof LivingEntity mount) {
            int mountRows = computeHeartRows(mount.getMaxHealth(), 0);
            if (mountRows > 0) leftStack += mountRows * LayoutConstants.HEART_ROW_HEIGHT;
        }
        if (leftStack > 0) leftStack += LayoutConstants.GAP_ABOVE_HOTBAR;

        int rightStack = 0;
        boolean hungerVisible = !p.getAbilities().creativeMode && !p.isSpectator();
        if (hungerVisible) rightStack += LayoutConstants.FOOD_ROW_HEIGHT;
        boolean airVisible = p.getAir() < p.getMaxAir() || p.isSubmergedInWater();
        if (airVisible) rightStack += LayoutConstants.FOOD_ROW_HEIGHT;
        if (rightStack > 0) rightStack += LayoutConstants.GAP_ABOVE_HOTBAR;

        int centerStack = 0;
        boolean hasMount = p.hasVehicle();
        if (hasMount) {
            centerStack = Math.max(centerStack, LayoutConstants.JUMP_BAR_HEIGHT + LayoutConstants.GAP_ABOVE_HOTBAR);
        } else {
            boolean xpVisible = !p.getAbilities().creativeMode && !p.isSpectator()
                    && (p.experienceLevel > 0 || p.experienceProgress > 0f);
            if (xpVisible) centerStack = Math.max(centerStack, LayoutConstants.XP_BAR_HEIGHT + LayoutConstants.GAP_ABOVE_HOTBAR);
        }

        return reserved + centerStack + Math.max(leftStack, rightStack);
    }

    private static int computeHeartRows(float maxHealth, float absorption) {
        int halfHearts = (int) Math.ceil((maxHealth + absorption) / 2.0f);
        if (halfHearts <= 0) return 0;
        return (halfHearts + 9) / 10;
    }
}