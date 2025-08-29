package com.tooltip.utils;

import net.minecraft.text.MutableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.Rarity;

public class ColorUtils {

    public static Formatting getDurabilityColor(float percentage) {
        if (percentage > 0.5f) return Formatting.GREEN;
        if (percentage > 0.25f) return Formatting.YELLOW;
        if (percentage > 0.1f) return Formatting.GOLD;
        return Formatting.RED;
    }

    public static int getRarityColor(Rarity rarity) {
        return switch (rarity) {
            case COMMON -> 0xFFFFFF;
            case UNCOMMON -> 0xFFFF55;
            case RARE -> 0x55FFFF;
            case EPIC -> 0xFF55FF;
        };
    }

    public static MutableText applyRarityFormatting(MutableText text, Rarity rarity, boolean enchanted) {
        return switch (rarity) {
            case COMMON -> enchanted ? text.formatted(Formatting.AQUA) : text.formatted(Formatting.WHITE);
            case UNCOMMON -> text.formatted(Formatting.YELLOW);
            case RARE -> text.formatted(Formatting.AQUA, Formatting.BOLD);
            case EPIC -> text.formatted(Formatting.LIGHT_PURPLE, Formatting.BOLD);
        };
    }
}