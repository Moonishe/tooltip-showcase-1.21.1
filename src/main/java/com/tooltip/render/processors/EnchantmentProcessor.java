package com.tooltip.render.processors;

import com.tooltip.config.TooltipConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items; // НОВЫЙ ИМПОРТ
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;

public class EnchantmentProcessor {
    private static final String[] ROMAN_NUMERALS = {
            "", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"
    };

    private final MinecraftClient client;

    public EnchantmentProcessor(MinecraftClient client) {
        this.client = client;
    }

    public List<Text> processEnchantments(ItemStack stack, TooltipConfig config) {
        List<Text> lines = new ArrayList<>();
        var enchComp = stack.getEnchantments();

        if (enchComp.isEmpty()) return lines;

        List<EnchantmentData> all = new ArrayList<>();
        for (RegistryEntry<Enchantment> entry : enchComp.getEnchantments()) {
            int level = enchComp.getLevel(entry);
            Enchantment ench = entry.value();
            int maxLevel = ench.getMaxLevel();
            Text nameText = ench.description();
            String levelText = getLevelText(level, config.useRomanNumerals);
            boolean isMax = maxLevel > 0 && level >= maxLevel;
            all.add(new EnchantmentData(nameText, levelText, isMax));
        }

        all.sort((a, b) -> {
            int wa = client.textRenderer.getWidth(a.name.getString() + " " + a.levelText);
            int wb = client.textRenderer.getWidth(b.name.getString() + " " + b.levelText);
            return Integer.compare(wb, wa);
        });

        int visibleCount = Math.min(config.maxVisibleEnchants, all.size());
        List<EnchantmentData> visible = new ArrayList<>(visibleCount);
        for (int i = 0; i < visibleCount; i++) visible.add(all.get(i));

        int endCol = config.maxEnchantColor & 0xFFFFFF;
        int startCol = deriveStartColorFromMax(endCol);
        int normalCount = 0;
        for (EnchantmentData e : visible) if (!e.isMaxLevel) normalCount++;

        int shown = 0;
        int normalIndex = 0;
        for (EnchantmentData e : visible) {
            MutableText namePart = Text.empty().append(e.name).formatted(Formatting.GRAY);
            MutableText levelPart = Text.literal(" " + e.levelText);

            // НОВОЕ: Проверяем, является ли предмет книгой
            boolean isBook = stack.isOf(Items.ENCHANTED_BOOK);

            if (e.isMaxLevel) {
                // НОВОЕ: Если это книга с макс. уровнем, используем стандартный золотой цвет
                if (isBook) {
                    levelPart.styled(s -> s.withColor(Formatting.GOLD));
                } else {
                    levelPart.styled(s -> s.withColor(TextColor.fromRgb(endCol)));
                }

                if (config.enableEnchantGlow) {
                    levelPart.styled(s -> s.withBold(true));
                }
            } else {
                if (config.enableEnchantGradient && normalCount > 0) {
                    float t = (normalCount == 1) ? 1f : (float) normalIndex / (normalCount - 1);
                    int col = lerpHSV(startCol, endCol, t);
                    levelPart.styled(s -> s.withColor(TextColor.fromRgb(col)));
                    normalIndex++;
                } else {
                    levelPart.formatted(Formatting.GRAY);
                }
            }

            lines.add(namePart.append(levelPart));
            shown++;
        }

        int hidden = all.size() - shown;
        if (hidden > 0) {
            lines.add(Text.literal("+" + hidden + " more").formatted(Formatting.DARK_GRAY));
        }

        return lines;
    }

    // --- Вспомогательные методы без изменений ---
    private String getLevelText(int level, boolean roman) {
        if (roman) {
            if (level > 0 && level < ROMAN_NUMERALS.length) return ROMAN_NUMERALS[level];
            return String.valueOf(level);
        }
        return String.valueOf(level);
    }
    private static int deriveStartColorFromMax(int endColor) {
        float[] hsv = rgbToHsv(endColor);
        float h = hsv[0], s = hsv[1], v = hsv[2];
        float s1 = clamp01(s * 0.25f);
        float v1 = clamp01(Math.max(0.60f, v * 0.90f));
        return hsvToRgb(h, s1, v1);
    }
    private static int lerpHSV(int c1, int c2, float t) {
        t = clamp01(t);
        float[] a = rgbToHsv(c1), b = rgbToHsv(c2);
        float h = lerpAngle(a[0], b[0], t);
        float s = a[1] + (b[1] - a[1]) * t;
        float v = a[2] + (b[2] - a[2]) * t;
        return hsvToRgb(h, clamp01(s), clamp01(v));
    }
    private static float lerpAngle(float a, float b, float t) {
        float delta = ((b - a + 1.5f) % 1.0f) - 0.5f;
        return (a + delta * t + 1.0f) % 1.0f;
    }
    private static float clamp01(float x) { return Math.max(0f, Math.min(1f, x)); }
    private static float[] rgbToHsv(int rgb) {
        float r = ((rgb >> 16) & 0xFF) / 255f, g = ((rgb >> 8) & 0xFF) / 255f, b = (rgb & 0xFF) / 255f;
        float max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b));
        float delta = max - min;
        float h = 0f;
        if (delta >= 1e-6f) {
            if (max == r) h = ((g - b) / delta) % 6f;
            else if (max == g) h = ((b - r) / delta) + 2f;
            else h = ((r - g) / delta) + 4f;
        }
        h /= 6f;
        if (h < 0f) h += 1f;
        float s = (max <= 1e-6f) ? 0f : (delta / max);
        return new float[]{h, s, max};
    }
    private static int hsvToRgb(float h, float s, float v) {
        float i = (float) Math.floor(h * 6f), f = h * 6f - i;
        float p = v * (1f - s), q = v * (1f - f * s), t = v * (1f - (1f - f) * s);
        float r, g, b;
        switch ((int) i % 6) {
            case 0: r = v; g = t; b = p; break;
            case 1: r = q; g = v; b = p; break;
            case 2: r = p; g = v; b = t; break;
            case 3: r = p; g = q; b = v; break;
            case 4: r = t; g = p; b = v; break;
            default: r = v; g = p; b = q; break;
        }
        return (Math.round(r * 255f) << 16) | (Math.round(g * 255f) << 8) | Math.round(b * 255f);
    }
    public static class EnchantmentData {
        public final Text name;
        public final String levelText;
        public final boolean isMaxLevel;
        public EnchantmentData(Text name, String levelText, boolean isMaxLevel) {
            this.name = name; this.levelText = levelText; this.isMaxLevel = isMaxLevel;
        }
    }
}