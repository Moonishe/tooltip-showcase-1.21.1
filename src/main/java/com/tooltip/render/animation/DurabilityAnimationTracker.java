package com.tooltip.render.animation;

import com.tooltip.config.TooltipConfig;
import com.tooltip.render.AnimationConstants;
import com.tooltip.render.AnimationUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.RotationAxis;

import java.util.HashMap;
import java.util.Map;

public class DurabilityAnimationTracker {
    private static class DigitAnimation {
        public final char oldChar;
        public final char newChar;
        public float progress;

        // целевые параметры (пересчёт каждый апдейт)
        public float tScale = 1f;
        public float tAlpha = 1f;
        public float tOffset = 0f;
        public float tRotation = 0f;

        // текущие, сглаженные (что реально рисуем)
        public float scale = 1f;
        public float alpha = 1f;
        public float offsetY = 0f;
        public float rotation = 0f;

        public boolean firstUpdate = true;

        public DigitAnimation(char oldChar, char newChar) {
            this.oldChar = oldChar;
            this.newChar = newChar;
            this.progress = 0.0f;
        }
    }

    private final Map<Integer, DigitAnimation> digitAnimations = new HashMap<>();
    private String lastDurabilityText = "";
    private final TooltipConfig config;
    private final MinecraftClient client;

    public DurabilityAnimationTracker(MinecraftClient client) {
        this.config = TooltipConfig.getInstance();
        this.client = client;
    }

    public void updateDurability(String newDurabilityText) {
        if (newDurabilityText.equals(lastDurabilityText)) return;

        digitAnimations.clear();

        int newLen = newDurabilityText.length();
        int oldLen = lastDurabilityText.length();

        for (int i = 0; i < Math.max(newLen, oldLen); i++) {
            int newPos = newLen - 1 - i;
            int oldPos = oldLen - 1 - i;

            char newChar = newPos >= 0 ? newDurabilityText.charAt(newPos) : ' ';
            char oldChar = oldPos >= 0 ? lastDurabilityText.charAt(oldPos) : ' ';

            if (newChar != oldChar && Character.isDigit(newChar)) {
                digitAnimations.put(newPos, new DigitAnimation(oldChar, newChar));
            }
        }

        lastDurabilityText = newDurabilityText;
    }

    private static float smoothTo(float current, float target, float dt, float halfLife) {
        // Нулевой overshoot, dt-зависимое сглаживание
        float k = (float) Math.pow(0.5, dt / Math.max(1e-4f, halfLife));
        return current * k + target * (1 - k);
    }

    private static float clamp(float v, float min, float max) {
        return v < min ? min : (v > max ? max : v);
    }

    public void update(float deltaTime) {
        float dt = Math.max(0.0001f, Math.min(deltaTime, 0.05f));
        float rate = 2.2f * Math.max(0.1f, config.animationSpeed); // медленнее для ещё большей плавности

        digitAnimations.entrySet().removeIf(entry -> {
            DigitAnimation anim = entry.getValue();
            anim.progress += dt * rate;

            if (anim.progress >= 1.0f) {
                return true; // анимация завершена — убираем
            }

            float eased = AnimationUtils.easeIOS(anim.progress);
            float introFade = AnimationUtils.easeInOutSine(Math.min(1f, anim.progress * 1.2f));

            // целевые значения (как раньше, но без пружин)
            anim.tScale = 1.0f;
            anim.tAlpha = introFade;
            anim.tOffset = 0.0f;
            anim.tRotation = 0.0f;

            switch (config.durabilityNumberAnimation) {
                case PULSE:
                    anim.tScale = 1.0f + 0.30f * (1.0f - eased) * config.durabilityAnimationIntensity;
                    anim.tAlpha = 1.0f * introFade;
                    break;

                case BOUNCE: {
                    float bounce = (float) Math.sin(eased * Math.PI) * 0.5f;
                    anim.tOffset = -bounce * 9.0f * config.durabilityAnimationIntensity;
                    anim.tAlpha = 1.0f * introFade;
                    break;
                }

                case SHAKE: {
                    float shake = (1.0f - eased) * config.durabilityAnimationIntensity;
                    anim.tOffset = (float) (Math.sin(anim.progress * 20.0f) * shake * 2.5f);
                    anim.tAlpha = 1.0f * introFade;
                    break;
                }

                case GLOW:
                    anim.tAlpha = (1.0f + (1.0f - eased) * 0.45f * config.durabilityAnimationIntensity) * introFade;
                    break;

                case SCALE_BOUNCE:
                    if (anim.progress < 0.5f) {
                        anim.tScale = 1.0f + anim.progress * 0.7f * config.durabilityAnimationIntensity;
                    } else {
                        anim.tScale = 1.0f + (1.0f - anim.progress) * 0.7f * config.durabilityAnimationIntensity;
                    }
                    anim.tAlpha = 1.0f * introFade;
                    break;

                case ROLL:
                    anim.tRotation = (1.0f - eased) * 330.0f * config.durabilityAnimationIntensity; // чуть меньше, чтобы не «вылетало»
                    anim.tAlpha = 1.0f * introFade;
                    break;

                case FLASH:
                    anim.tAlpha = (1.0f + (1.0f - eased) * 0.9f * config.durabilityAnimationIntensity) * introFade;
                    anim.tScale = 1.0f + (1.0f - eased) * 0.25f * config.durabilityAnimationIntensity;
                    break;

                case NONE:
                default:
                    anim.tAlpha = 1.0f * introFade;
                    break;
            }

            // Ограничим цели здравыми пределами, чтобы не «вылетало»
            anim.tScale = clamp(anim.tScale, 0.75f, 1.45f);
            anim.tOffset = clamp(anim.tOffset, -14f, 14f);
            anim.tRotation = clamp(anim.tRotation, -540f, 540f);
            anim.tAlpha = clamp(anim.tAlpha, 0f, 1.25f);

            // На первом апдейте — без лагов прилипнем к цели (чтобы не было «двойного» старта)
            if (anim.firstUpdate) {
                anim.scale = anim.tScale;
                anim.alpha = anim.tAlpha;
                anim.offsetY = anim.tOffset;
                anim.rotation = anim.tRotation;
                anim.firstUpdate = false;
            } else {
                // Мягкое сглаживание без overshoot
                anim.alpha   = clamp(smoothTo(anim.alpha,   anim.tAlpha,   dt, AnimationConstants.DIGIT_SMOOTH_HALF_ALPHA), 0f, 1.25f);
                anim.scale   = clamp(smoothTo(anim.scale,   anim.tScale,   dt, AnimationConstants.DIGIT_SMOOTH_HALF_SCALE), 0.6f, 1.6f);
                anim.offsetY = clamp(smoothTo(anim.offsetY, anim.tOffset,  dt, AnimationConstants.DIGIT_SMOOTH_HALF_OFFSET), -20f, 20f);
                anim.rotation= clamp(smoothTo(anim.rotation,anim.tRotation,dt, AnimationConstants.DIGIT_SMOOTH_HALF_ROT),   -720f, 720f);
            }

            return false;
        });
    }

    public boolean hasAnimationForPosition(int position) { return digitAnimations.containsKey(position); }
    public DigitAnimation getAnimation(int position) { return digitAnimations.get(position); }

    public void renderAnimatedText(DrawContext context, String text, float x, float y, int color) {
        float currentX = x;

        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            String charStr = String.valueOf(ch);

            if (hasAnimationForPosition(i) && Character.isDigit(ch)) {
                DigitAnimation anim = getAnimation(i);

                context.getMatrices().push();

                float charX = currentX;
                float charY = y + anim.offsetY;

                float charWidth = client.textRenderer.getWidth(charStr);
                float centerX = charX + charWidth / 2.0f;
                float centerY = charY + client.textRenderer.fontHeight / 2.0f;

                context.getMatrices().translate(centerX, centerY, 0);
                if (Math.abs(anim.rotation) > 0.001f) {
                    context.getMatrices().multiply(RotationAxis.POSITIVE_Z.rotationDegrees(anim.rotation));
                }
                if (Math.abs(anim.scale - 1.0f) > 0.001f) {
                    context.getMatrices().scale(anim.scale, anim.scale, 1.0f);
                }
                context.getMatrices().translate(-centerX, -centerY, 0);

                int animColor = config.durabilityChangeColor;
                if (anim.progress > 0.7f) {
                    float t = (anim.progress - 0.7f) / 0.3f;
                    t = clamp(t, 0f, 1f);
                    animColor = lerpColor(config.durabilityChangeColor, color, t);
                }

                int baseAlpha = (color >> 24) & 0xFF;
                int alpha = Math.min(255, (int)(baseAlpha * clamp(anim.alpha, 0f, 1f)));
                animColor = (alpha << 24) | (animColor & 0xFFFFFF);

                context.drawTextWithShadow(client.textRenderer, charStr, (int)charX, (int)charY, animColor);
                context.getMatrices().pop();
            } else {
                context.drawTextWithShadow(client.textRenderer, charStr, (int)currentX, (int)y, color);
            }

            currentX += client.textRenderer.getWidth(charStr);
        }
    }

    private static int lerpColor(int color1, int color2, float t) {
        t = clamp(t, 0f, 1f);
        int r1 = (color1 >> 16) & 0xFF, g1 = (color1 >> 8) & 0xFF, b1 = color1 & 0xFF;
        int r2 = (color2 >> 16) & 0xFF, g2 = (color2 >> 8) & 0xFF, b2 = color2 & 0xFF;
        int r = (int)(r1 + (r2 - r1) * t);
        int g = (int)(g1 + (g2 - g1) * t);
        int b = (int)(b1 + (b2 - b1) * t);
        return (r << 16) | (g << 8) | b;
    }

    public void reset() {
        digitAnimations.clear();
        lastDurabilityText = "";
    }
}