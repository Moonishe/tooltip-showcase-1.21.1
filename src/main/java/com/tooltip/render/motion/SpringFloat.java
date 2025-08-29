package com.tooltip.render.motion;

/**
 * Second-order dynamics (Freya Holmer) для суперплавного сглаживания.
 * Параметры:
 *  - f (Hz): "частота" реакции (выше — быстрее, ниже — плавнее)
 *  - z (damping ratio): демпфирование (1.0–1.2 даёт «iOS»-плавность, >1 мягче)
 *  - r (response): реагирование на скорость входа (0..1), 0.3–0.5 — мягкий «подхват»
 */
public final class SpringFloat {
    private float y;     // текущее значение
    private float yd;    // текущая скорость
    private float xp;    // предыдущее целевое (для оценки производной)
    private float k1, k2, k3; // предвычисленные константы

    public SpringFloat(float f, float z, float r, float initial) {
        setParams(f, z, r);
        this.y = initial;
        this.yd = 0f;
        this.xp = initial;
    }

    public void setParams(float f, float z, float r) {
        // защищаем от нуля
        f = Math.max(0.01f, f);
        z = Math.max(0.01f, z);
        float w = (float) (2.0 * Math.PI * f);
        this.k1 = z / (float) (Math.PI * f);
        this.k2 = 1.0f / (w * w);
        this.k3 = r * z / w;
    }

    public float update(float dt, float x) {
        dt = Math.max(0f, Math.min(dt, 0.05f)); // защита от фризов
        // производная входа
        float xd = (x - xp) / Math.max(1e-4f, dt);
        xp = x;

        // интегрируем
        y += dt * yd;
        float a = x + k3 * xd - y;
        float ydd = (a - k1 * yd) / k2;
        yd += dt * ydd;

        return y;
    }

    public float getValue() { return y; }
    public void snapTo(float x) { y = x; yd = 0f; xp = x; }
}