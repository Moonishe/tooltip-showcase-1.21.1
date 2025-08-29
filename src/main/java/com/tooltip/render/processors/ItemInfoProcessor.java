package com.tooltip.render.processors;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;

import java.util.Optional;

public class ItemInfoProcessor {

    public Optional<Text> processFoodInfo(ItemStack stack) {
        FoodComponent food = stack.get(DataComponentTypes.FOOD);
        if (food == null) return Optional.empty();

        return Optional.of(Text.literal("Food: +" + food.nutrition() + " | Sat: +" +
                        String.format("%.1f", food.saturation()))
                .formatted(Formatting.GOLD));
    }

    public Optional<Text> processArmorInfo(ItemStack stack) {
        if (!(stack.getItem() instanceof ArmorItem armorItem)) {
            return Optional.empty();
        }

        MutableText armorText = Text.literal("Defense: +" + armorItem.getProtection());

        if (armorItem.getToughness() > 0) {
            armorText.append(" | Tough: +" + armorItem.getToughness());
        }

        return Optional.of(armorText.formatted(Formatting.BLUE));
    }

    public Optional<Text> processItemId(ItemStack stack, boolean showId) {
        if (!showId) return Optional.empty();

        Identifier id = Registries.ITEM.getId(stack.getItem());
        return Optional.of(Text.literal("ID: " + id.toString()).formatted(Formatting.DARK_GRAY));
    }

    public Text getItemNameWithRarity(ItemStack stack) {
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
}