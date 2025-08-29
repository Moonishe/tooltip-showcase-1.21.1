package com.tooltip.render.processors;

import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffectUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.item.LingeringPotionItem;
import net.minecraft.item.PotionItem;
import net.minecraft.item.TippedArrowItem;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;

public class PotionEffectProcessor {
    private final MinecraftClient client;

    public PotionEffectProcessor(MinecraftClient client) {
        this.client = client;
    }

    public List<Text> processPotionEffects(ItemStack stack) {
        List<Text> lines = new ArrayList<>();

        if (!(stack.getItem() instanceof PotionItem) &&
                !(stack.getItem() instanceof TippedArrowItem) &&
                !(stack.getItem() instanceof LingeringPotionItem)) {
            return lines;
        }

        PotionContentsComponent potionContents = stack.get(DataComponentTypes.POTION_CONTENTS);
        if (potionContents == null) return lines;

        for (StatusEffectInstance effect : potionContents.getEffects()) {
            MutableText effectText = Text.translatable(effect.getEffectType().value().getTranslationKey());

            if (effect.getAmplifier() > 0) {
                effectText.append(" ").append(Text.translatable("potion.potency." + effect.getAmplifier()));
            }

            if (!effect.isDurationBelow(20)) {
                String duration = StatusEffectUtil.getDurationText(effect, 1.0f,
                        client.world.getTickManager().getTickRate()).getString();
                effectText.append(" (").append(duration).append(")");
            }

            effectText = effectText.formatted(effect.getEffectType().value().isBeneficial() ?
                    Formatting.GREEN : Formatting.RED);
            lines.add(effectText);
        }

        return lines;
    }
}