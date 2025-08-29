package com.tooltip.render;

public class AnimationUtils {

    public static float easeOutQuart(float t) {
        t = Math.max(0, Math.min(1, t));
        return 1 - (float) Math.pow(1 - t, 4);
    }

    public static float easeInOutCubic(float t) {
        t = Math.max(0, Math.min(1, t));
        return t < 0.5f ? 4f * t * t * t : 1f - (float)Math.pow(-2f * t + 2f, 3f) / 2f;
    }

    public static float easeOutExpo(float t) {
        t = Math.max(0, Math.min(1, t));
        return t == 1 ? 1 : 1 - (float)Math.pow(2, -10 * t);
    }

    public static float easeOutBack(float t) {
        t = Math.max(0, Math.min(1, t));
        float c1 = 1.70158f;
        float c3 = c1 + 1;
        return 1 + c3 * (float)Math.pow(t - 1, 3) + c1 * (float)Math.pow(t - 1, 2);
    }

    public static float easeOutElastic(float t) {
        t = Math.max(0, Math.min(1, t));
        if (t == 0) return 0;
        if (t == 1) return 1;
        double c4 = (2 * Math.PI) / 3;
        return (float)(Math.pow(2, -10 * t) * Math.sin((t * 10 - 0.75) * c4) + 1);
    }

    public static float smoothStep(float t) {
        t = Math.max(0, Math.min(1, t));
        return t * t * (3.0f - 2.0f * t);
    }

    // Новые плавные профили
    public static float easeInOutSine(float t) {
        t = Math.max(0, Math.min(1, t));
        return (float)(-(Math.cos(Math.PI * t) - 1) / 2.0);
    }

    public static float easeInOutQuint(float t) {
        t = Math.max(0, Math.min(1, t));
        return t < 0.5f
                ? 16f * t * t * t * t * t
                : 1f - (float)Math.pow(-2f * t + 2f, 5f) / 2f;
    }

    // «Айфоновая» плавность по умолчанию
    public static float easeIOS(float t) {
        return easeInOutCubic(t);
    }
}