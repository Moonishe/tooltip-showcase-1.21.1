package com.tooltip.render.animation;

import com.tooltip.config.TooltipConfig;
import com.tooltip.render.AnimationConstants;
import com.tooltip.render.AnimationUtils;
import com.tooltip.render.motion.SpringFloat;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class TooltipAnimationManager {
    // Состояние тултипа
    private float tooltipAlpha = 0.0f;
    private float targetAlpha = 0.0f;
    private float tooltipSlideProgress = 0.0f;
    private float targetSlideProgress = 0.0f;
    private float tooltipScaleProgress = 0.0f;
    private float targetScaleProgress = 0.0f;
    private float tooltipRotationProgress = 0.0f;
    private float targetRotationProgress = 0.0f;
    private float tooltipBounceProgress = 0.0f;
    private float introTimer = 0f; // глобальный fade-in
    private float swingTime = 0f; // время с начала SWING
    private float swingPhase = 0f; // фаза swing
    private float tooltipWaveOffset = 0.0f;
    private int tooltipTypewriterChars = 0;
    private float typewriterTime = 0.0f;
    private boolean isTooltipFadingOut = false;
    private float tooltipFadeOutProgress = 0.0f;
    private float animationStartTime = -1;

    // Прочность
    private float durabilityChangeAnimation = 0.0f;
    private float durabilityPulse = 0.0f;
    private float enchantmentGlowTime = 0.0f;
    private long lastDurabilityChangeTime = 0;

    private final Map<String, ElementAnimation> elementAnimations = new HashMap<>();
    private final TooltipConfig config;

    private static final int MAX_ELEMENT_ANIMATIONS = 100;

    // Пружины (суперплавность)
    private SpringFloat alphaSpring;
    private SpringFloat slideSpring;
    private SpringFloat scaleSpring;
    private SpringFloat rotationSpring;


    public TooltipAnimationManager() {
        this.config = TooltipConfig.getInstance();
        initSprings();
        resetTooltipAnimations();
        introTimer = 0f;
        swingTime = 0f;
        swingPhase = 0f;
    }

    private void initSprings() {
        float speed = Math.max(0.1f, config.animationSpeed);
        float fUI = AnimationConstants.SPRING_FREQ_UI * speed;
        alphaSpring    = new SpringFloat(fUI, AnimationConstants.SPRING_ZETA_UI, AnimationConstants.SPRING_RESP_UI, 0f);
        slideSpring    = new SpringFloat(fUI, AnimationConstants.SPRING_ZETA_UI, AnimationConstants.SPRING_RESP_UI, 0f);
        scaleSpring    = new SpringFloat(fUI, AnimationConstants.SPRING_ZETA_UI, AnimationConstants.SPRING_RESP_UI, 0f);
        rotationSpring = new SpringFloat(fUI, AnimationConstants.SPRING_ZETA_UI, AnimationConstants.SPRING_RESP_UI, 0f);
    }

    private float clamp01(float v) { return Math.max(0f, Math.min(1f, v)); }

    public void updateTooltipAnimations(float dt) {
        dt = Math.max(0f, Math.min(dt, 0.05f));
        float speed = Math.max(0.1f, config.animationSpeed);
        float fUI = AnimationConstants.SPRING_FREQ_UI * speed;
        alphaSpring.setParams(fUI, AnimationConstants.SPRING_ZETA_UI, AnimationConstants.SPRING_RESP_UI);
        slideSpring.setParams(fUI, AnimationConstants.SPRING_ZETA_UI, AnimationConstants.SPRING_RESP_UI);
        scaleSpring.setParams(fUI, AnimationConstants.SPRING_ZETA_UI, AnimationConstants.SPRING_RESP_UI);
        rotationSpring.setParams(fUI, AnimationConstants.SPRING_ZETA_UI, AnimationConstants.SPRING_RESP_UI);

        float desiredAlpha;

        if (isTooltipFadingOut) {
            targetAlpha = 0.0f;
            targetSlideProgress = 0.0f;
            targetScaleProgress = 0.0f;
            targetRotationProgress = 0.0f;
            tooltipFadeOutProgress = Math.min(1.0f, tooltipFadeOutProgress + dt * AnimationConstants.FADE_OUT_SPEED * speed);
            desiredAlpha = 0.0f;
        } else {
            // общий fade‑in таймер
            introTimer += dt;

            targetAlpha = 1.0f;
            targetSlideProgress = 1.0f;
            targetScaleProgress = 1.0f;
            targetRotationProgress = 1.0f;

            switch (config.tooltipAnimation) {
                case TYPEWRITER:
                    typewriterTime += dt * AnimationConstants.TYPEWRITER_SPEED * speed;
                    tooltipTypewriterChars = (int) typewriterTime;
                    desiredAlpha = 1.0f;
                    break;
                case WAVE:
                    tooltipWaveOffset += dt * 1.0f * speed;
                    desiredAlpha = 1.0f;
                    break;
                case PULSE:
                    durabilityPulse += dt * 1.0f;
                    float pulse = (float) (Math.sin(durabilityPulse * 2.0f) * 0.5 + 0.5);
                    desiredAlpha = 0.4f + 0.6f * AnimationUtils.easeOutExpo(pulse);
                    break;
                default:
                    desiredAlpha = 1.0f;
                    break;
            }

            // Глобальный fade‑in для всех стилей (0.18c)
            float introFactor = AnimationUtils.easeIOS(Math.min(1.0f, introTimer / 0.18f));
            desiredAlpha *= introFactor;

            // Доп. fade‑in для BOUNCE по его прогрессу
            if (config.tooltipAnimation == TooltipConfig.AnimationStyle.BOUNCE) {
                float intro = AnimationUtils.easeIOS(tooltipBounceProgress);
                desiredAlpha = Math.min(desiredAlpha, intro);
            }

            // Короткий SWING: накапливаем время и фазу только для SWING
            if (config.tooltipAnimation == TooltipConfig.AnimationStyle.SWING) {
                swingTime  += dt;
                swingPhase += dt * 10.0f; // скорость колебаний
            } else {
                swingTime = 0f;
                swingPhase = 0f;
            }
        }

        tooltipAlpha            = clamp01(alphaSpring.update(dt, desiredAlpha));
        tooltipSlideProgress    = clamp01(slideSpring.update(dt, targetSlideProgress));
        tooltipScaleProgress    = clamp01(scaleSpring.update(dt, targetScaleProgress));
        tooltipRotationProgress = clamp01(rotationSpring.update(dt, targetRotationProgress));

        if (config.tooltipAnimation == TooltipConfig.AnimationStyle.BOUNCE) {
            tooltipBounceProgress = Math.min(1.0f, tooltipBounceProgress + dt * 2.5f * speed);
        }

        if (tooltipAlpha > 0) {
            durabilityPulse += dt * 0.5f;
            enchantmentGlowTime += dt * 0.4f;
        }

        if (durabilityChangeAnimation > 0) {
            float t = Math.min(1f, AnimationConstants.DEFAULT_INTERPOLATION_SPEED * dt * 2.0f);
            durabilityChangeAnimation = durabilityChangeAnimation * (1f - t);
            if (durabilityChangeAnimation < 0.01f) durabilityChangeAnimation = 0f;
        }
    }

    public void resetTooltipAnimations() {
        isTooltipFadingOut = false;
        tooltipFadeOutProgress = 0.0f;
        animationStartTime = System.nanoTime() / 1_000_000_000.0f;

        // Сбрасываем значения и пружины
        if (config.tooltipAnimation == TooltipConfig.AnimationStyle.TYPEWRITER) {
            tooltipTypewriterChars = 0;
            typewriterTime = 0.0f;
            targetAlpha = 1.0f;
            tooltipAlpha = 1.0f;
        } else {
            targetAlpha = 0.0f;
            tooltipAlpha = 0.0f;
        }

        targetSlideProgress = 0.0f;
        targetScaleProgress = 0.0f;
        targetRotationProgress = 0.0f;

        tooltipSlideProgress = 0.0f;
        tooltipScaleProgress = 0.0f;
        tooltipRotationProgress = 0.0f;

        tooltipBounceProgress = 0.0f;
        tooltipWaveOffset = 0.0f;

        // пружины на новые стартовые значения
        alphaSpring.snapTo(tooltipAlpha);
        slideSpring.snapTo(tooltipSlideProgress);
        scaleSpring.snapTo(tooltipScaleProgress);
        rotationSpring.snapTo(tooltipRotationProgress);
    }

    public void fadeOutTooltip(float dt) {
        if (!isTooltipFadingOut) {
            isTooltipFadingOut = true;
            tooltipFadeOutProgress = 0.0f;
        }
    }

    public void triggerDurabilityAnimation() {
        if (!config.animateDurability) return;

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastDurabilityChangeTime > 150) {
            durabilityChangeAnimation = 1.0f;
            lastDurabilityChangeTime = currentTime;
        } else {
            durabilityChangeAnimation = Math.min(1.0f, durabilityChangeAnimation + 0.5f);
        }
    }

    // Getters
    public float getAlpha() { return clamp01(tooltipAlpha); }

    public float getXOffset() {
        float progress = AnimationUtils.easeIOS(tooltipSlideProgress);
        switch (config.tooltipAnimation) {
            case SLIDE_LEFT:  return -AnimationConstants.SLIDE_OFFSET_X * (1 - progress);
            case SLIDE_RIGHT: return  AnimationConstants.SLIDE_OFFSET_X * (1 - progress);
            default:          return 0.0f;
        }
    }

    public float getYOffset() {
        switch (config.tooltipAnimation) {
            case SLIDE_UP: {
                float p = AnimationUtils.easeIOS(tooltipSlideProgress);
                return AnimationConstants.SLIDE_OFFSET_Y * (1 - p);
            }
            case SLIDE_DOWN: {
                float p = AnimationUtils.easeIOS(tooltipSlideProgress);
                return -AnimationConstants.SLIDE_OFFSET_Y * (1 - p);
            }
            case BOUNCE:
                return -AnimationConstants.BOUNCE_OFFSET * (1 - AnimationUtils.easeOutElastic(tooltipBounceProgress));
            default:
                return 0.0f;
        }
    }

    public float getTooltipScaleValue() {
        if (config.tooltipAnimation == TooltipConfig.AnimationStyle.SCALE ||
                config.tooltipAnimation == TooltipConfig.AnimationStyle.ZOOM) {
            float p = AnimationUtils.easeIOS(tooltipScaleProgress);
            return 0.5f + 0.5f * p;
        }
        return 1.0f;
    }

    public float getTooltipRotationAngle() {
        switch (config.tooltipAnimation) {
            case ROTATE:
// вместо 180° делаем аккуратный «spin-in» до ~12°
                return 12.0f * (1 - AnimationUtils.easeIOS(tooltipRotationProgress));
            case SWING:
// короткий, быстро затухающий свинг (half-life ~0.22c)
                float amp = 12.0f * (float) Math.pow(0.5, swingTime / 0.22f);
                float ang = (float) Math.sin(swingPhase) * amp;
                return ang * tooltipAlpha;
            case FLIP:
                return 90.0f * (1 - AnimationUtils.easeIOS(tooltipRotationProgress));
            default:
                return 0.0f;
        }
    }

    public float getTooltipWaveOffset() { return tooltipWaveOffset; }
    public int getTooltipTypewriterChars() { return tooltipTypewriterChars; }
    public float getDurabilityChangeAnimation() { return durabilityChangeAnimation; }
    public float getDurabilityPulse() { return durabilityPulse; }
    public float getEnchantmentGlowTime() { return enchantmentGlowTime; }
    public float getAnimationStartTime() { return animationStartTime; }

    public void updateAnimations(float dt) {
        updateTooltipAnimations(dt);
        updateElementAnimations(dt);
        cleanupFinishedAnimations();
    }

    // Element animations (оставлено как было)
    public void registerElementChange(String elementId, String oldValue, String newValue) {
        if (elementAnimations.size() >= MAX_ELEMENT_ANIMATIONS) {
            cleanupFinishedAnimations();
        }
        ElementAnimation anim = elementAnimations.computeIfAbsent(elementId, k -> new ElementAnimation());
        anim.triggerChange(oldValue, newValue);
    }

    public void updateElementAnimations(float dt) {
        dt = Math.max(0f, Math.min(dt, 0.05f));
        float speed = Math.max(0.1f, config.animationSpeed);
        for (ElementAnimation anim : elementAnimations.values()) {
            anim.update(dt * 2.0f * speed);
        }
    }

    private void cleanupFinishedAnimations() {
        Iterator<Map.Entry<String, ElementAnimation>> iterator = elementAnimations.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, ElementAnimation> entry = iterator.next();
            if (entry.getValue().isFinished()) iterator.remove();
        }
    }

    public ElementAnimation getElementAnimation(String elementId) { return elementAnimations.get(elementId); }

    public class ElementAnimation {
        private float progress = 0.0f;
        private float currentProgress = 0.0f;
        private boolean isAnimating = false;
        private String oldValue;
        private String newValue;

        public void triggerChange(String oldVal, String newVal) {
            this.oldValue = oldVal;
            this.newValue = newVal;
            this.progress = 0.0f;
            this.currentProgress = 0.0f;
            this.isAnimating = true;
        }

        public void update(float delta) {
            if (isAnimating) {
                progress = Math.min(1.0f, progress + delta);
                // лёгкая подстройка (оставим старый плавный lerp — достаточно)
                float t = Math.min(1f, AnimationConstants.DEFAULT_INTERPOLATION_SPEED * delta);
                currentProgress = currentProgress + (progress - currentProgress) * t;
                if (progress >= 1.0f) isAnimating = false;
            }
        }

        public boolean isFinished() { return !isAnimating && progress >= 1.0f; }
        public boolean isAnimating() { return isAnimating; }
        public float getProgress() { return currentProgress; }
        public float getEasedProgress() { return AnimationUtils.easeIOS(currentProgress); }
        public String getOldValue() { return oldValue; }
        public String getNewValue() { return newValue; }
    }
}