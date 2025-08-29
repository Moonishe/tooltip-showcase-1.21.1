package com.tooltip.render.state;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class TooltipStateManager {
    private ItemStack lastStack = ItemStack.EMPTY;
    private Item lastItem = null;
    private int lastDurability = -1;
    private List<Text> cachedLines = new ArrayList<>();
    private int cachedTotalHeight = 0;

    public boolean isItemChanged(ItemStack currentStack) {
        if (currentStack.isEmpty() && lastStack.isEmpty()) {
            return false;
        }
        if (currentStack.isEmpty() != lastStack.isEmpty()) {
            return true;
        }
        // Проверяем, что предметы идентичны с учетом NBT
        return !ItemStack.areEqual(currentStack, lastStack);
    }

    public boolean checkDurabilityChange(ItemStack stack) {
        if (!stack.isDamageable()) return false;

        int maxDamage = stack.getMaxDamage();
        int currentDurability = maxDamage - stack.getDamage();

        if (lastDurability == -1) {
            lastDurability = currentDurability;
            return false;
        }

        if (lastDurability != currentDurability) {
            lastDurability = currentDurability;
            return true;
        }

        return false;
    }

    public void initializeDurability(ItemStack stack) {
        if (stack.isDamageable()) {
            int maxDamage = stack.getMaxDamage();
            lastDurability = maxDamage - stack.getDamage();
        } else {
            lastDurability = -1;
        }
    }

    public void updateCache(ItemStack stack, List<Text> lines) {
        this.lastStack = stack.copy();
        this.lastItem = stack.getItem();
        this.cachedLines = new ArrayList<>(lines);
    }

    public void clear() {
        lastStack = ItemStack.EMPTY;
        lastItem = null;
        lastDurability = -1;
        cachedLines.clear();
        cachedTotalHeight = 0;
    }

    // Геттеры
    public ItemStack getLastStack() { return lastStack; }
    public List<Text> getCachedLines() { return cachedLines; }
    public int getCachedTotalHeight() { return cachedTotalHeight; }
    public void setCachedTotalHeight(int height) { this.cachedTotalHeight = height; }
}