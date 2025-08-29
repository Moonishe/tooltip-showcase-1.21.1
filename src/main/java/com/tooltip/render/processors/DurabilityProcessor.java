package com.tooltip.render.processors;

import com.tooltip.config.TooltipConfig;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class DurabilityProcessor {

    public Text createDurabilityText(ItemStack stack, TooltipConfig.DurabilityDisplayMode mode) {
        if (!stack.isDamageable()) {
            return null;
        }

        int maxDamage = stack.getMaxDamage();
        int currentDurability = maxDamage - stack.getDamage();
        float percentage = (float) currentDurability / maxDamage;

        switch (mode) {
            case PERCENTAGE:
                Formatting color = getDurabilityColor(percentage);
                return Text.literal("Durability: " + String.format("%.1f%%", percentage * 100))
                        .formatted(color);

            case NUMBERS:
                Formatting numberColor = getDurabilityColor(percentage);
                return Text.literal("Durability: " + currentDurability + "/" + maxDamage)
                        .formatted(numberColor);

            case BAR:
                // Специальный маркер для отрисовки бара
                return Text.literal("DURABILITY_BAR");

            default:
                return Text.empty();
        }
    }

    public Formatting getDurabilityColor(float percentage) {
        if (percentage > 0.6f) return Formatting.GREEN;
        else if (percentage > 0.3f) return Formatting.YELLOW;
        else if (percentage > 0.1f) return Formatting.GOLD;
        else return Formatting.RED;
    }
}