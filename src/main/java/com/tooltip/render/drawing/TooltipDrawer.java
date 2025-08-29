package com.tooltip.render.drawing;

import com.tooltip.config.TooltipConfig;
import com.tooltip.render.AnimationConstants;
import com.tooltip.render.HUDLayoutHelper;
import com.tooltip.render.animation.DurabilityAnimationTracker;
import com.tooltip.render.animation.TooltipAnimationManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.RotationAxis;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TooltipDrawer {
    private final MinecraftClient client;
    private final TooltipConfig config;
    private final DurabilityAnimationTracker durabilityTracker;
    private ItemStack currentStack;
    private String lastDurabilityText = "";
    private int lastScreenHeight = 0;

    // x/y
    private static final Pattern DURABILITY_PATTERN = Pattern.compile("(\\d+/\\d+)");
    // 78.8% или 78,8% (поддержка и точки, и запятой)
    private static final Pattern PERCENT_PATTERN = Pattern.compile("(\\d+(?:[\\.,]\\d+)?%)");

    public TooltipDrawer(MinecraftClient client, TooltipConfig config) {
        this.client = client;
        this.config = config;
        this.durabilityTracker = new DurabilityAnimationTracker(client);
    }

    public void render(DrawContext context, List<Text> lines, TooltipAnimationManager animations,
                       ItemStack stack, float tickDeltaSeconds) {
        if (animations.getAlpha() <= 0.01f || lines.isEmpty()) {
            return;
        }

        this.currentStack = stack;

        int currentScreenHeight = client.getWindow().getScaledHeight();
        if (lastScreenHeight != 0 && Math.abs(lastScreenHeight - currentScreenHeight) > 50) {
            // опционально: пересчёт позиции при резком ресайзе
        }
        lastScreenHeight = currentScreenHeight;

        // Обновляем трекер анимации цифр прочности (теперь учитывает и проценты)
        updateDurabilityTracker(lines);

        // Обновляем сами анимации цифр (dt в секундах)
        durabilityTracker.update(tickDeltaSeconds);

        context.getMatrices().push();
        try {
            context.getMatrices().translate(0, 0, AnimationConstants.TOOLTIP_Z_OFFSET);

            // Центр с учётом анимации по X
            float centerX = client.getWindow().getScaledWidth() / 2.0f + config.tooltipOffsetX + animations.getXOffset();
            float startY = calculateY(lines) + animations.getYOffset();

            // Габариты тултипа (для определения pivot по Y)
            int[] size = measureTooltipSize(lines); // [maxWidth, totalHeight]
            float pivotX = centerX;
            float pivotY = startY + size[1] / 2.0f;

            // ТОЛЬКО теперь применяем анимационные трансформации к canvas вокруг центра тултипа
            applyTooltipAnimation(context, animations, pivotX, pivotY);

            if (config.drawBackground) {
                renderBackground(context, centerX, startY, lines, animations.getAlpha());
            }

            renderLines(context, lines, centerX, startY, animations);
        } finally {
            context.getMatrices().pop();
        }
    }

    private void updateDurabilityTracker(List<Text> lines) {
        for (Text line : lines) {
            String text = line.getString();

            // Сначала пытаемся поймать проценты (Percentage mode)
            Matcher percent = PERCENT_PATTERN.matcher(text);
            if (percent.find()) {
                String numbers = percent.group(1);
                if (!numbers.equals(lastDurabilityText)) {
                    durabilityTracker.updateDurability(numbers);
                    lastDurabilityText = numbers;
                }
                continue; // важное: если нашли проценты — не проверяем x/y дальше
            }

            // Потом — x/y (Numbers mode)
            Matcher ratio = DURABILITY_PATTERN.matcher(text);
            if (ratio.find()) {
                String numbers = ratio.group(1);
                if (!numbers.equals(lastDurabilityText)) {
                    durabilityTracker.updateDurability(numbers);
                    lastDurabilityText = numbers;
                }
            }
        }
    }

    public void resetAnimation() {
        durabilityTracker.reset();
        lastDurabilityText = "";
        lastScreenHeight = 0;
    }

    // TYPEWRITER (опционально)
    private void renderLinesTypewriter(DrawContext context, List<Text> lines, float centerX, float startY,
                                       TooltipAnimationManager animations) {
        float y = startY;
        int remaining = animations.getTooltipTypewriterChars();
        if (remaining <= 0) return;

        for (Text line : lines) {
            String s = line.getString();
            if (s.equals("DURABILITY_BAR")) {
                if (remaining <= 0) break;
                renderDurabilityBar(context, centerX, y, animations);
                y += 22;
                continue;
            }

            int color = getColorFromFormatting(line);
            int drawCount = Math.min(remaining, s.length());
            String part = s.substring(0, Math.max(0, drawCount));
            int textWidth = client.textRenderer.getWidth(s);
            float textX = centerX - textWidth / 2.0f;

            context.drawTextWithShadow(client.textRenderer,
                    part, (int) textX, (int) y,
                    (int) (animations.getAlpha() * 255) << 24 | (color & 0xFFFFFF));

            remaining -= drawCount;
            y += client.textRenderer.fontHeight + 2;

            if (remaining <= 0) break;
        }
    }

    // WAVE (опционально)
    private void drawTextWave(DrawContext context, String text, float startX, float y, int color, float waveOffset) {
        float x = startX;
        for (int i = 0; i < text.length(); i++) {
            String ch = text.substring(i, i + 1);
            float dy = (float) Math.sin((i * 0.35f) + waveOffset * Math.PI * 2) * AnimationConstants.WAVE_AMPLITUDE;
            context.drawTextWithShadow(client.textRenderer, ch, (int) x, (int) (y + dy), color);
            x += client.textRenderer.getWidth(ch);
        }
    }

    private void renderLinesWave(DrawContext context, List<Text> lines, float centerX, float startY,
                                 TooltipAnimationManager animations) {
        float y = startY;
        int baseAlpha = (int)(animations.getAlpha() * 255);
        if (baseAlpha < 10) return;

        float waveOffset = animations.getTooltipWaveOffset();

        for (Text line : lines) {
            String lineText = line.getString();
            if (lineText.equals("DURABILITY_BAR")) {
                renderDurabilityBar(context, centerX, y, animations);
                y += 22;
                continue;
            }

            int color = getColorFromFormatting(line) | (baseAlpha << 24);
            int textWidth = client.textRenderer.getWidth(lineText);
            float textX = centerX - textWidth / 2.0f;

            drawTextWave(context, lineText, textX, y, color, waveOffset);
            y += client.textRenderer.fontHeight + 2;
        }
    }

    private void renderLines(DrawContext context, List<Text> lines, float centerX, float startY,
                             TooltipAnimationManager animations) {
        int baseAlpha = (int)(animations.getAlpha() * 255);
        if (baseAlpha < 10) return;

        if (config.tooltipAnimation == TooltipConfig.AnimationStyle.TYPEWRITER) {
            renderLinesTypewriter(context, lines, centerX, startY, animations);
            return;
        }

        if (config.tooltipAnimation == TooltipConfig.AnimationStyle.WAVE) {
            renderLinesWave(context, lines, centerX, startY, animations);
            return;
        }

        float y = startY;

        for (Text line : lines) {
            String lineText = line.getString();

            if (lineText.equals("DURABILITY_BAR")) {
                renderDurabilityBar(context, centerX, y, animations);
                y += 22;
                continue;
            }

            // Проверяем проценты или x/y
            boolean isDurabilityLine = false;
            String durabilityNumbers = "";
            Formatting durabilityColor = Formatting.WHITE;

            Matcher percent = PERCENT_PATTERN.matcher(lineText);
            if (percent.find()) {
                isDurabilityLine = true;
                durabilityNumbers = percent.group(1);
                // Цвет — по фактическому процентажу предмета (как раньше)
                if (currentStack != null && currentStack.isDamageable()) {
                    int maxDamage = currentStack.getMaxDamage();
                    int currentDurability = maxDamage - currentStack.getDamage();
                    float percentage = (float) currentDurability / maxDamage;
                    durabilityColor = getDurabilityColor(percentage);
                }
            } else {
                Matcher ratio = DURABILITY_PATTERN.matcher(lineText);
                if (ratio.find()) {
                    isDurabilityLine = true;
                    durabilityNumbers = ratio.group(1);

                    if (currentStack != null && currentStack.isDamageable()) {
                        int maxDamage = currentStack.getMaxDamage();
                        int currentDurability = maxDamage - currentStack.getDamage();
                        float percentage = (float) currentDurability / maxDamage;
                        durabilityColor = getDurabilityColor(percentage);
                    }
                }
            }

            if (isDurabilityLine && config.animateDurabilityNumbers && !durabilityNumbers.isEmpty()) {
                renderDurabilityLineWithAnimation(context, lineText, durabilityNumbers,
                        centerX, y, baseAlpha, durabilityColor);
            } else {
                int textWidth = client.textRenderer.getWidth(line);
                float textX = centerX - textWidth / 2.0f;

                int color = getColorFromFormatting(line);

                context.drawTextWithShadow(client.textRenderer, line,
                        (int)textX, (int)y,
                        color | (baseAlpha << 24));
            }

            y += client.textRenderer.fontHeight + 2;
        }
    }

    private int getColorFromFormatting(Text text) {
        var style = text.getStyle();
        if (style != null && style.getColor() != null) {
            return style.getColor().getRgb() & 0xFFFFFF;
        }
        return 0xFFFFFF;
    }

    private Formatting getDurabilityColor(float percentage) {
        if (percentage > 0.6f) return Formatting.GREEN;
        else if (percentage > 0.3f) return Formatting.YELLOW;
        else if (percentage > 0.1f) return Formatting.GOLD;
        else return Formatting.RED;
    }

    private int getDurabilityColorInt(float percentage) {
        if (percentage > 0.6f) return 0x55FF55;
        else if (percentage > 0.3f) return 0xFFFF55;
        else if (percentage > 0.1f) return 0xFFAA00;
        else return 0xFF5555;
    }

    private void renderDurabilityLineWithAnimation(DrawContext context, String fullText,
                                                   String numbers, float centerX, float y,
                                                   int alpha, Formatting colorFormatting) {
        int numbersStart = fullText.indexOf(numbers);
        String prefix = numbersStart > 0 ? fullText.substring(0, numbersStart) : "";
        String suffix = numbersStart + numbers.length() < fullText.length() ?
                fullText.substring(numbersStart + numbers.length()) : "";

        int baseColor = 0xFFFFFF;
        switch (colorFormatting) {
            case GREEN -> baseColor = 0x55FF55;
            case YELLOW -> baseColor = 0xFFFF55;
            case GOLD -> baseColor = 0xFFAA00;
            case RED -> baseColor = 0xFF5555;
            case DARK_RED -> baseColor = 0xAA0000;
        }

        float totalWidth = client.textRenderer.getWidth(fullText);
        float startX = centerX - totalWidth / 2.0f;

        if (!prefix.isEmpty()) {
            context.drawTextWithShadow(client.textRenderer, prefix,
                    (int)startX, (int)y,
                    baseColor | (alpha << 24));
            startX += client.textRenderer.getWidth(prefix);
        }

        // Рендерим числа с анимацией (цифры внутри "78.8%" анимируются, знак % и . — статичны)
        durabilityTracker.renderAnimatedText(context, numbers, startX, y, baseColor | (alpha << 24));

        if (!suffix.isEmpty()) {
            startX += client.textRenderer.getWidth(numbers);
            context.drawTextWithShadow(client.textRenderer, suffix,
                    (int)startX, (int)y,
                    baseColor | (alpha << 24));
        }
    }

    private void renderDurabilityBar(DrawContext context, float centerX, float y,
                                     TooltipAnimationManager animations) {
        if (currentStack == null || currentStack.isEmpty() || !currentStack.isDamageable()) {
            return;
        }

        int maxDamage = currentStack.getMaxDamage();
        int currentDurability = maxDamage - currentStack.getDamage();
        float percentage = (float) currentDurability / maxDamage;

        float barWidth = 60.0f;
        float barHeight = 4.0f;
        float barX = centerX - barWidth / 2.0f;

        int alpha = (int)(animations.getAlpha() * 255);

        String durabilityText = currentDurability + "/" + maxDamage;
        int textColor = getDurabilityColorInt(percentage);

        if (config.animateDurabilityNumbers) {
            if (!durabilityText.equals(lastDurabilityText)) {
                durabilityTracker.updateDurability(durabilityText);
                lastDurabilityText = durabilityText;
            }
            float textWidth = client.textRenderer.getWidth(durabilityText);
            durabilityTracker.renderAnimatedText(context, durabilityText,
                    centerX - textWidth / 2.0f, y,
                    textColor | (alpha << 24));
        } else {
            int textWidth = client.textRenderer.getWidth(durabilityText);
            context.drawTextWithShadow(client.textRenderer, durabilityText,
                    (int)(centerX - textWidth / 2.0f), (int)y,
                    textColor | (alpha << 24));
        }

        y += 10;

        // фон бара
        context.fill((int)barX - 1, (int)y - 1,
                (int)(barX + barWidth + 1), (int)(y + barHeight + 1),
                (alpha / 2 << 24) | 0x000000);

        if (percentage > 0) {
            float fillWidth = barWidth * percentage;
            int barColor = getDurabilityColorInt(percentage);

            if (percentage < 0.2f && config.durabilityGlow) {
                float pulse = (float)(Math.sin(animations.getDurabilityPulse() * 4) * 0.2f + 0.8f);
                int r = (barColor >> 16) & 0xFF;
                int g = (barColor >> 8) & 0xFF;
                int b = barColor & 0xFF;
                r = (int)(r * pulse);
                g = (int)(g * pulse);
                b = (int)(b * pulse);
                barColor = (r << 16) | (g << 8) | b;
            }

            context.fill((int)barX, (int)y,
                    (int)(barX + fillWidth), (int)(y + barHeight),
                    (alpha << 24) | barColor);
        }
    }

    private void applyTooltipAnimation(DrawContext context, TooltipAnimationManager animations, float pivotX, float pivotY) {
        switch (config.tooltipAnimation) {
            case SCALE:
            case ZOOM: {
                float scale = animations.getTooltipScaleValue();
                if (Math.abs(scale - 1.0f) > 0.001f) {
                    context.getMatrices().translate(pivotX, pivotY, 0);
                    context.getMatrices().scale(scale, scale, 1.0f);
                    context.getMatrices().translate(-pivotX, -pivotY, 0);
                }
                break;
            }
            case FLIP: {
                float rotationY = animations.getTooltipRotationAngle();
                if (Math.abs(rotationY) > 0.001f) {
                    context.getMatrices().translate(pivotX, pivotY, 0);
                    context.getMatrices().multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rotationY));
                    context.getMatrices().translate(-pivotX, -pivotY, 0);
                }
                break;
            }
            case ROTATE:
            case SWING: {
                float rotationZ = animations.getTooltipRotationAngle();
                if (Math.abs(rotationZ) > 0.001f) {
                    context.getMatrices().translate(pivotX, pivotY, 0);
                    context.getMatrices().multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rotationZ));
                    context.getMatrices().translate(-pivotX, -pivotY, 0);
                }
                break;
            }
            default:
                // SLIDE/BOUNCE/WAVE already accounted for in centerX/startY
                break;
        }
    }

    private void renderBackground(DrawContext context, float centerX, float startY,
                                  List<Text> lines, float alpha) {
        int padding = AnimationConstants.TOOLTIP_PADDING;
        int maxWidth = AnimationConstants.MIN_TOOLTIP_WIDTH;

        for (Text line : lines) {
            if (!"DURABILITY_BAR".equals(line.getString())) {
                maxWidth = Math.max(maxWidth, client.textRenderer.getWidth(line));
            }
        }

        int totalHeight = 0;
        for (Text line : lines) {
            if ("DURABILITY_BAR".equals(line.getString())) {
                totalHeight += 22;
            } else {
                totalHeight += client.textRenderer.fontHeight + 2;
            }
        }

        int bgX = (int) (centerX - maxWidth / 2.0f - padding);
        int bgY = (int) (startY - padding);
        int bgWidth = maxWidth + padding * 2;
        int bgHeight = totalHeight + padding * 2;

        int bgAlpha = (int) (alpha * config.backgroundOpacity * 255);
        int bgColor = (bgAlpha << 24) | (config.backgroundColor & 0xFFFFFF);

        if (config.enableSmoothCorners && config.cornerRadius > 0) {
            renderRoundedRectangle(context, bgX, bgY, bgWidth, bgHeight, config.cornerRadius, bgColor);
        } else {
            context.fill(bgX, bgY, bgX + bgWidth, bgY + bgHeight, bgColor);
        }
    }

    private void renderRoundedRectangle(DrawContext context, int x, int y, int width, int height,
                                        int radius, int color) {
        radius = Math.min(radius, Math.min(width, height) / 2);

        context.fill(x + radius, y, x + width - radius, y + height, color);
        context.fill(x, y + radius, x + width, y + height - radius, color);

        for (int i = 0; i < radius; i++) {
            for (int j = 0; j < radius; j++) {
                if (i * i + j * j <= radius * radius) {
                    context.fill(x + radius - i - 1, y + radius - j - 1,
                            x + radius - i, y + radius - j, color);
                    context.fill(x + width - radius + i, y + radius - j - 1,
                            x + width - radius + i + 1, y + radius - j, color);
                    context.fill(x + radius - i - 1, y + height - radius + j,
                            x + radius - i, y + height - radius + j + 1, color);
                    context.fill(x + width - radius + i, y + height - radius + j,
                            x + width - radius + i + 1, y + height - radius + j + 1, color);
                }
            }
        }
    }

    private float calculateY(List<Text> lines) {
        int screenHeight = client.getWindow().getScaledHeight();
        int screenWidth = client.getWindow().getScaledWidth();
        if (screenHeight < 100 || screenWidth < 100) return 50;

        int tooltipHeight = 0;
        for (Text line : lines) {
            if ("DURABILITY_BAR".equals(line.getString())) tooltipHeight += 22;
            else tooltipHeight += client.textRenderer.fontHeight + 2;
        }

        int bottomHUD = HUDLayoutHelper.computeBottomReservedHUD(client);

        int safeVerticalOffset = Math.max(0, Math.min(config.tooltipVerticalOffset, screenHeight / 3));

        float y = screenHeight - bottomHUD - tooltipHeight;
        y += config.tooltipOffsetY;
        y -= safeVerticalOffset;

        float minY = 10;
        float maxY = screenHeight - tooltipHeight - 10;
        y = Math.max(minY, Math.min(maxY, y));
        return y;
    }

    private int[] measureTooltipSize(List<Text> lines) {
        int maxWidth = AnimationConstants.MIN_TOOLTIP_WIDTH;
        for (Text line : lines) {
            if (!"DURABILITY_BAR".equals(line.getString())) {
                maxWidth = Math.max(maxWidth, client.textRenderer.getWidth(line));
            }
        }
        int totalHeight = 0;
        for (Text line : lines) {
            if ("DURABILITY_BAR".equals(line.getString())) totalHeight += 22;
            else totalHeight += client.textRenderer.fontHeight + 2;
        }
        return new int[]{ maxWidth, totalHeight };
    }
}