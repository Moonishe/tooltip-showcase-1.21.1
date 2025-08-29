package com.tooltip.render;

public class AnimationConstants {
    // Пер-секундные скорости
    public static final float DEFAULT_INTERPOLATION_SPEED = 10.0f;
    public static final float FADE_OUT_SPEED = 6.0f;
    public static final float TYPEWRITER_SPEED = 20.0f;

    // Не используем где лишнее, но оставляем для совместимости
    public static final float DURABILITY_ANIMATION_SPEED = 1.0f;

    // Смещения/размеры
    public static final float SLIDE_OFFSET_X = 60.0f;
    public static final float SLIDE_OFFSET_Y = 50.0f;
    public static final float BOUNCE_OFFSET = 30.0f;
    public static final float WAVE_AMPLITUDE = 3.0f;

    // Тайминги для построчных анимаций (если используешь)
    public static final float LINE_ANIMATION_DURATION = 0.3f;
    public static final float LINE_ANIMATION_SPACING = 0.05f;
    public static final int TYPEWRITER_CHARS_PER_LINE = 15;

    // Z-слой
    public static final int TOOLTIP_Z_OFFSET = 500;

    // Размеры
    public static final float DURABILITY_BAR_WIDTH = 80.0f;
    public static final float DURABILITY_BAR_HEIGHT = 6.0f;
    public static final int TOOLTIP_PADDING = 6;
    public static final int MIN_TOOLTIP_WIDTH = 80;

    // Параметры пружины (суперплавность)
// Чем меньше частота — тем плавнее. zeta ~1.05–1.15 даёт «масляную» плавность.
// UI (весь тултип)
    public static final float SPRING_FREQ_UI   = 5.5f;   // ГЛАВНЫЙ «рычаг» плавности UI (можно 3.8–6.5)
    public static final float SPRING_ZETA_UI   = 1.08f;  // демпфирование (1.0–1.2)
    public static final float SPRING_RESP_UI   = 0.35f;  // влияние скорости входа (0..1)

    // Цифры прочности
    public static final float SPRING_FREQ_DIGIT = 7.5f;  // можно 6.5–8.5
    public static final float SPRING_ZETA_DIGIT = 1.06f;
    public static final float SPRING_RESP_DIGIT = 0.40f;

    public static final float DIGIT_SMOOTH_HALF_ALPHA = 0.12f; // сек, чем больше — плавнее
    public static final float DIGIT_SMOOTH_HALF_SCALE = 0.07f;
    public static final float DIGIT_SMOOTH_HALF_OFFSET= 0.07f;
    public static final float DIGIT_SMOOTH_HALF_ROT = 0.07f;
}