package com.tooltip.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.MinecraftClient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class TooltipConfig {
    private static TooltipConfig INSTANCE;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = MinecraftClient.getInstance().runDirectory.toPath().resolve("config/advancedtooltip.json");

    private static final int CONFIG_VERSION = 3;
    private int version = CONFIG_VERSION;

    // ... все enum и поля остаются такими же ...

    public enum DurabilityDisplayMode {
        PERCENTAGE,
        NUMBERS,
        BAR
    }

    public enum AnimationStyle {
        NONE,
        FADE,
        SLIDE_UP,
        SLIDE_DOWN,
        SLIDE_LEFT,
        SLIDE_RIGHT,
        SCALE,
        BOUNCE,
        WAVE,
        PULSE,
        ROTATE,
        SWING,
        TYPEWRITER,
        ZOOM,
        FLIP
    }

    public enum NumberAnimationStyle {
        NONE,
        PULSE,
        BOUNCE,
        SHAKE,
        GLOW,
        SCALE_BOUNCE,
        ROLL,
        FLASH
    }

    public enum SeparatorStyle {
        NONE,
        EMPTY_LINE,
        LINE,
        DOTS,
        DOUBLE_LINE
    }

    // === ОСНОВНЫЕ НАСТРОЙКИ ===
    public boolean drawBackground = true;
    public int maxVisibleEnchants = 5;
    public int maxShulkerItems = 5;
    public boolean showItemId = false;

    // === НОВЫЕ НАСТРОЙКИ ===
    public boolean debugMode = false;
    public boolean showNBTSize = false;
    public boolean showComponentCount = false;

    public boolean useSeparators = false;
    public SeparatorStyle separatorStyle = SeparatorStyle.NONE;
    public int separatorColor = 0x555555;

    public boolean showTooltipShadow = true;
    public boolean animateColorTransitions = true;
    public float tooltipTransparency = 1.0f;
    public boolean enableGlowEffect = false;
    public int glowColor = 0xFFFFFF;
    public float glowIntensity = 0.5f;

    public boolean enableCaching = true;
    public int cacheExpirationMs = 100;
    public boolean reducedMotion = false;

    public boolean showStackSize = true;
    public boolean showRepairCost = false;
    public boolean showUnbreakable = true;
    public boolean showAttributeModifiers = false;

    // === СУЩЕСТВУЮЩИЕ НАСТРОЙКИ ===
    public boolean useRomanNumerals = true;
    public int maxEnchantColor = 0xFFD700;
    public boolean enableEnchantGlow = true;
    public boolean enableEnchantGradient = true;

    public DurabilityDisplayMode durabilityMode = DurabilityDisplayMode.NUMBERS;
    public boolean animateDurability = true;
    public boolean animateDurabilityNumbers = true;
    public NumberAnimationStyle durabilityNumberAnimation = NumberAnimationStyle.PULSE;
    public float durabilityAnimationIntensity = 1.0f;
    public int durabilityChangeColor = 0xFFFF00;
    public boolean durabilityGlow = true;
    public boolean enableBounceAnimation = true;
    public boolean ignoreChatOverlay = true;

    public AnimationStyle tooltipAnimation = AnimationStyle.FADE;
    public AnimationStyle lineAnimation = AnimationStyle.NONE;
    public float animationSpeed = 1.0f;
    public boolean enableScrollAnimation = true;

    public int tooltipOffsetX = 0;
    public int tooltipOffsetY = 0;
    public int tooltipVerticalOffset = 40;

    public float backgroundOpacity = 0.8f;
    public int backgroundColor = 0x000000;
    public float textScale = 1.0f;
    public boolean enableSmoothCorners = true;
    public int cornerRadius = 4;

    public static TooltipConfig getInstance() {
        if (INSTANCE == null) {
            load();
        }
        return INSTANCE;
    }

    public static void load() {
        if (Files.exists(CONFIG_PATH)) {
            try {
                String json = Files.readString(CONFIG_PATH);
                INSTANCE = GSON.fromJson(json, TooltipConfig.class);

                if (INSTANCE != null && INSTANCE.version < CONFIG_VERSION) {
                    INSTANCE.migrate();
                    save();
                }

                if (INSTANCE != null) {
                    INSTANCE.validate();
                }
            } catch (IOException e) {
                e.printStackTrace();
                INSTANCE = new TooltipConfig();
                save();
            }
        } else {
            INSTANCE = new TooltipConfig();
            save();
        }
    }

    // === НОВЫЙ МЕТОД ДЛЯ СБРОСА НАСТРОЕК ===
    public static void resetToDefaults() {
        INSTANCE = new TooltipConfig();
        save();
    }

    private void migrate() {
        if (this.version < 3) {
            this.debugMode = false;
            this.useSeparators = false;
            this.separatorStyle = SeparatorStyle.NONE;
            this.showTooltipShadow = true;
            this.animateColorTransitions = true;
            this.tooltipTransparency = 1.0f;
            this.enableCaching = true;
            this.cacheExpirationMs = 100;
            this.reducedMotion = false;
        }

        this.version = CONFIG_VERSION;

        if (animateDurability && !animateDurabilityNumbers) {
            animateDurabilityNumbers = true;
        }
    }

    private void validate() {
        animationSpeed = Math.max(0.1f, Math.min(5.0f, animationSpeed));
        durabilityAnimationIntensity = Math.max(0.1f, Math.min(2.0f, durabilityAnimationIntensity));
        backgroundOpacity = Math.max(0.0f, Math.min(1.0f, backgroundOpacity));
        textScale = Math.max(0.5f, Math.min(2.0f, textScale));
        maxVisibleEnchants = Math.max(1, Math.min(20, maxVisibleEnchants));
        maxShulkerItems = Math.max(1, Math.min(27, maxShulkerItems));
        cornerRadius = Math.max(0, Math.min(20, cornerRadius));
        tooltipVerticalOffset = Math.max(0, Math.min(200, tooltipVerticalOffset));

        tooltipTransparency = Math.max(0.1f, Math.min(1.0f, tooltipTransparency));
        cacheExpirationMs = Math.max(50, Math.min(1000, cacheExpirationMs));
        glowIntensity = Math.max(0.0f, Math.min(1.0f, glowIntensity));

        separatorColor = separatorColor & 0xFFFFFF;
        glowColor = glowColor & 0xFFFFFF;
    }

    public static void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            Files.writeString(CONFIG_PATH, GSON.toJson(INSTANCE));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean shouldShowDebugInfo() {
        return debugMode && (showNBTSize || showComponentCount);
    }

    public boolean isPerformanceModeEnabled() {
        return reducedMotion || !enableCaching;
    }
}