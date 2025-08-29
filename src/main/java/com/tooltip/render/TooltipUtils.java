package com.tooltip.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Rarity;

public class TooltipUtils {

    public static Text getItemNameWithRarity(ItemStack stack) {
        Text name = stack.getName();
        Rarity rarity = stack.getRarity();

        switch (rarity) {
            case UNCOMMON:
                return Text.literal("").append(name).formatted(Formatting.YELLOW);
            case RARE:
                return Text.literal("").append(name).formatted(Formatting.AQUA);
            case EPIC:
                return Text.literal("").append(name).formatted(Formatting.LIGHT_PURPLE);
            default:
                return Text.literal("").append(name).formatted(Formatting.WHITE);
        }
    }

    public static String getRomanNumeral(int number) {
        String[] ROMAN_NUMERALS = {
                "", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"
        };

        if (number > 0 && number < ROMAN_NUMERALS.length) {
            return ROMAN_NUMERALS[number];
        }
        return String.valueOf(number);
    }

    public static float smoothStep(float x) {
        return x * x * (3.0f - 2.0f * x);
    }

    public static boolean shouldRender(MinecraftClient client) {
        return client.player != null
                && client.world != null
                && client.currentScreen == null
                && !client.options.hudHidden;
    }
}