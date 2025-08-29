package com.tooltip.config;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;

public class ModMenuConfigScreen extends Screen {
    private final Screen parent;
    private final TooltipConfig config;
    private CustomSlider colorRedSlider;
    private CustomSlider colorGreenSlider;
    private CustomSlider colorBlueSlider;

    private enum ConfigCategory {
        GENERAL("General"),
        DURABILITY("Durability"),
        ENCHANTMENTS("Enchantments"),
        ANIMATIONS("Animations"),
        POSITION("Position"),
        VISUAL("Visual"),
        ADVANCED("Advanced"),  // Новая категория
        DEBUG("Debug");        // Новая категория

        final String name;
        ConfigCategory(String name) { this.name = name; }
    }

    private ConfigCategory currentCategory = ConfigCategory.GENERAL;

    public ModMenuConfigScreen(Screen parent) {
        super(Text.literal("Advanced Tooltip Config"));
        this.parent = parent;
        this.config = TooltipConfig.getInstance();
    }

    @Override
    protected void init() {
        int categoryY = 30;
        int categoryX = 10;
        int buttonWidth = 70;

        for (ConfigCategory category : ConfigCategory.values()) {
            this.addDrawableChild(ButtonWidget.builder(
                    Text.literal(category.name),
                    button -> {
                        currentCategory = category;
                        clearAndRebuild();
                    }
            ).dimensions(categoryX, categoryY, buttonWidth, 18).build());
            categoryX += buttonWidth + 5;

            // Переход на новую строку после 6 кнопок
            if (categoryX > this.width - buttonWidth - 10) {
                categoryX = 10;
                categoryY += 23;
            }
        }

        renderCategoryContent();
    }

    private void clearAndRebuild() {
        this.clearChildren();
        init();
    }

    private void renderCategoryContent() {
        int y = 80; // Увеличено из-за двух рядов категорий
        int spacing = 25;

        switch (currentCategory) {
            case GENERAL:
                renderGeneralSettings(y, spacing);
                break;
            case DURABILITY:
                renderDurabilitySettings(y, spacing);
                break;
            case ENCHANTMENTS:
                renderEnchantmentSettings(y, spacing);
                break;
            case ANIMATIONS:
                renderAnimationSettings(y, spacing);
                break;
            case POSITION:
                renderPositionSettings(y, spacing);
                break;
            case VISUAL:
                renderVisualSettings(y, spacing);
                break;
            case ADVANCED:
                renderAdvancedSettings(y, spacing);
                break;
            case DEBUG:
                renderDebugSettings(y, spacing);
                break;
        }

        this.addDrawableChild(ButtonWidget.builder(ScreenTexts.DONE, (button) -> {
            TooltipConfig.save();
            this.client.setScreen(parent);
        }).dimensions(this.width / 2 - 100, this.height - 30, 200, 20).build());
    }

    private void renderGeneralSettings(int startY, int spacing) {
        int y = startY;

        this.addDrawableChild(createCheckbox(20, y, "Draw Background",
                config.drawBackground, (value) -> config.drawBackground = value));
        y += spacing;

        this.addDrawableChild(createCheckbox(20, y, "Show Item ID",
                config.showItemId, (value) -> config.showItemId = value));
        y += spacing;

        this.addDrawableChild(createCheckbox(20, y, "Show Stack Size",
                config.showStackSize, (value) -> config.showStackSize = value));
        y += spacing;

        this.addDrawableChild(createCheckbox(20, y, "Show Repair Cost",
                config.showRepairCost, (value) -> config.showRepairCost = value));
        y += spacing;

        this.addDrawableChild(createCheckbox(20, y, "Show Unbreakable",
                config.showUnbreakable, (value) -> config.showUnbreakable = value));
        y += spacing;

        this.addDrawableChild(new SliderWidget(20, y, 200, 20,
                Text.literal("Max Enchants: "), config.maxVisibleEnchants / 20.0) {
            @Override
            protected void updateMessage() {
                int enchants = Math.max(1, Math.min(20, (int)(value * 20)));
                setMessage(Text.literal("Max Enchants: " + enchants));
            }

            @Override
            protected void applyValue() {
                config.maxVisibleEnchants = Math.max(1, Math.min(20, (int)(value * 20)));
            }
        });
        y += spacing;

        this.addDrawableChild(new SliderWidget(20, y, 200, 20,
                Text.literal("Max Shulker Items: "), config.maxShulkerItems / 27.0) {
            @Override
            protected void updateMessage() {
                int items = Math.max(1, Math.min(27, (int)(value * 27)));
                setMessage(Text.literal("Max Shulker Items: " + items));
            }

            @Override
            protected void applyValue() {
                config.maxShulkerItems = Math.max(1, Math.min(27, (int)(value * 27)));
            }
        });
    }

    private void renderDurabilitySettings(int startY, int spacing) {
        int y = startY;

        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Display: " + config.durabilityMode.name()),
                button -> {
                    TooltipConfig.DurabilityDisplayMode[] modes = TooltipConfig.DurabilityDisplayMode.values();
                    int currentIndex = config.durabilityMode.ordinal();
                    int nextIndex = (currentIndex + 1) % modes.length;
                    config.durabilityMode = modes[nextIndex];
                    button.setMessage(Text.literal("Display: " + config.durabilityMode.name()));
                }
        ).dimensions(20, y, 200, 20).build());
        y += spacing;

        this.addDrawableChild(createCheckbox(20, y, "Animate Durability Numbers",
                config.animateDurabilityNumbers, (value) -> config.animateDurabilityNumbers = value));
        y += spacing;

        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Number Animation: " + config.durabilityNumberAnimation.name()),
                button -> {
                    TooltipConfig.NumberAnimationStyle[] styles = TooltipConfig.NumberAnimationStyle.values();
                    int currentIndex = config.durabilityNumberAnimation.ordinal();
                    int nextIndex = (currentIndex + 1) % styles.length;
                    config.durabilityNumberAnimation = styles[nextIndex];
                    button.setMessage(Text.literal("Number Animation: " + config.durabilityNumberAnimation.name()));
                }
        ).dimensions(20, y, 200, 20).build());
        y += spacing;

        this.addDrawableChild(new SliderWidget(20, y, 200, 20,
                Text.literal("Animation Intensity: "), config.durabilityAnimationIntensity / 2.0) {
            @Override
            protected void updateMessage() {
                float intensity = (float)(value * 2.0);
                setMessage(Text.literal("Animation Intensity: " + String.format("%.1fx", intensity)));
            }

            @Override
            protected void applyValue() {
                config.durabilityAnimationIntensity = Math.max(0.1f, Math.min(2.0f, (float)(value * 2.0)));
            }
        });
        y += spacing;

        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Change Color: #" + String.format("%06X", config.durabilityChangeColor)),
                button -> {
                    int[] colors = {0xFFFF00, 0xFF0000, 0x00FF00, 0x00FFFF, 0xFF00FF, 0xFFFFFF};
                    int currentIndex = 0;
                    for (int i = 0; i < colors.length; i++) {
                        if (colors[i] == config.durabilityChangeColor) {
                            currentIndex = i;
                            break;
                        }
                    }
                    config.durabilityChangeColor = colors[(currentIndex + 1) % colors.length];
                    button.setMessage(Text.literal("Change Color: #" + String.format("%06X", config.durabilityChangeColor)));
                }
        ).dimensions(20, y, 200, 20).build());
        y += spacing;

        this.addDrawableChild(createCheckbox(20, y, "Durability Glow",
                config.durabilityGlow, (value) -> config.durabilityGlow = value));
    }

    private void renderEnchantmentSettings(int startY, int spacing) {
        int y = startY;

        this.addDrawableChild(createCheckbox(20, y, "Use Roman Numerals",
                config.useRomanNumerals, (value) -> config.useRomanNumerals = value));
        y += spacing;

        this.addDrawableChild(createCheckbox(20, y, "Enchantment Glow Effect",
                config.enableEnchantGlow, (value) -> config.enableEnchantGlow = value));
        y += spacing;

        this.addDrawableChild(createCheckbox(20, y, "Enchantment Gradient Effect",
                config.enableEnchantGradient, (value) -> config.enableEnchantGradient = value));
        y += spacing;

        int currentColor = config.maxEnchantColor;
        int r = (currentColor >> 16) & 0xFF;
        int g = (currentColor >> 8) & 0xFF;
        int b = currentColor & 0xFF;

        colorRedSlider = new CustomSlider(this, 20, y, 200, 20,
                Text.literal("Red: "), r / 255.0);
        this.addDrawableChild(colorRedSlider);
        y += spacing;

        colorGreenSlider = new CustomSlider(this, 20, y, 200, 20,
                Text.literal("Green: "), g / 255.0);
        this.addDrawableChild(colorGreenSlider);
        y += spacing;

        colorBlueSlider = new CustomSlider(this, 20, y, 200, 20,
                Text.literal("Blue: "), b / 255.0);
        this.addDrawableChild(colorBlueSlider);
    }

    private void renderAnimationSettings(int startY, int spacing) {
        int y = startY;

        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Tooltip: " + config.tooltipAnimation.name()),
                button -> {
                    TooltipConfig.AnimationStyle[] styles = TooltipConfig.AnimationStyle.values();
                    int currentIndex = config.tooltipAnimation.ordinal();
                    int nextIndex = (currentIndex + 1) % styles.length;
                    config.tooltipAnimation = styles[nextIndex];
                    button.setMessage(Text.literal("Tooltip: " + config.tooltipAnimation.name()));
                }
        ).dimensions(20, y, 200, 20).build());
        y += spacing;

        this.addDrawableChild(new SliderWidget(20, y, 200, 20,
                Text.literal("Speed: "), (config.animationSpeed - 0.1f) / 4.9f) {
            @Override
            protected void updateMessage() {
                float speed = 0.1f + (float)(value * 4.9f);
                setMessage(Text.literal("Speed: " + String.format("%.1fx", speed)));
            }

            @Override
            protected void applyValue() {
                config.animationSpeed = 0.1f + (float)(value * 4.9f);
            }
        });
        y += spacing;

        this.addDrawableChild(createCheckbox(20, y, "Animate Color Transitions",
                config.animateColorTransitions, (value) -> config.animateColorTransitions = value));
        y += spacing;

        this.addDrawableChild(createCheckbox(20, y, "Reduced Motion",
                config.reducedMotion, (value) -> config.reducedMotion = value));
    }

    private void renderPositionSettings(int startY, int spacing) {
        int y = startY;

        this.addDrawableChild(new SliderWidget(20, y, 200, 20,
                Text.literal("Tooltip Height: "), (config.tooltipVerticalOffset) / 200.0) {
            @Override
            protected void updateMessage() {
                int offset = (int)(value * 200);
                setMessage(Text.literal("Tooltip Height: " + offset));
            }

            @Override
            protected void applyValue() {
                config.tooltipVerticalOffset = (int)(value * 200);
            }
        });
        y += spacing;

        this.addDrawableChild(new SliderWidget(20, y, 200, 20,
                Text.literal("X Offset: "), (config.tooltipOffsetX + 100) / 200.0) {
            @Override
            protected void updateMessage() {
                int offset = (int)(value * 200) - 100;
                setMessage(Text.literal("X Offset: " + offset));
            }

            @Override
            protected void applyValue() {
                config.tooltipOffsetX = (int)(value * 200) - 100;
            }
        });
        y += spacing;

        this.addDrawableChild(new SliderWidget(20, y, 200, 20,
                Text.literal("Y Offset: "), (config.tooltipOffsetY + 100) / 200.0) {
            @Override
            protected void updateMessage() {
                int offset = (int)(value * 200) - 100;
                setMessage(Text.literal("Y Offset: " + offset));
            }

            @Override
            protected void applyValue() {
                config.tooltipOffsetY = (int)(value * 200) - 100;
            }
        });
    }

    private void renderVisualSettings(int startY, int spacing) {
        int y = startY;

        this.addDrawableChild(new SliderWidget(20, y, 200, 20,
                Text.literal("Background Opacity: "), config.backgroundOpacity) {
            @Override
            protected void updateMessage() {
                setMessage(Text.literal("Background Opacity: " + String.format("%.0f%%", value * 100)));
            }

            @Override
            protected void applyValue() {
                config.backgroundOpacity = Math.max(0.0f, Math.min(1.0f, (float) value));
            }
        });
        y += spacing;

        this.addDrawableChild(new SliderWidget(20, y, 200, 20,
                Text.literal("Tooltip Transparency: "), config.tooltipTransparency) {
            @Override
            protected void updateMessage() {
                setMessage(Text.literal("Tooltip Transparency: " + String.format("%.0f%%", value * 100)));
            }

            @Override
            protected void applyValue() {
                config.tooltipTransparency = Math.max(0.1f, Math.min(1.0f, (float) value));
            }
        });
        y += spacing;

        this.addDrawableChild(new SliderWidget(20, y, 200, 20,
                Text.literal("Text Scale: "), (config.textScale - 0.5f) / 1.5f) {
            @Override
            protected void updateMessage() {
                float scale = 0.5f + (float)(value * 1.5f);
                setMessage(Text.literal("Text Scale: " + String.format("%.0f%%", scale * 100)));
            }

            @Override
            protected void applyValue() {
                config.textScale = Math.max(0.5f, Math.min(2.0f, 0.5f + (float)(value * 1.5f)));
            }
        });
        y += spacing;

        this.addDrawableChild(createCheckbox(20, y, "Smooth Corners",
                config.enableSmoothCorners, (value) -> config.enableSmoothCorners = value));
        y += spacing;

        this.addDrawableChild(new SliderWidget(20, y, 200, 20,
                Text.literal("Corner Radius: "), config.cornerRadius / 20.0) {
            @Override
            protected void updateMessage() {
                int radius = (int)(value * 20);
                setMessage(Text.literal("Corner Radius: " + radius));
            }

            @Override
            protected void applyValue() {
                config.cornerRadius = Math.max(0, Math.min(20, (int)(value * 20)));
            }
        });
        y += spacing;

        this.addDrawableChild(createCheckbox(20, y, "Show Tooltip Shadow",
                config.showTooltipShadow, (value) -> config.showTooltipShadow = value));
        y += spacing;

        this.addDrawableChild(createCheckbox(20, y, "Enable Glow Effect",
                config.enableGlowEffect, (value) -> config.enableGlowEffect = value));
    }

    private void renderAdvancedSettings(int startY, int spacing) {
        int y = startY;

        // Разделители
        this.addDrawableChild(createCheckbox(20, y, "Use Separators",
                config.useSeparators, (value) -> config.useSeparators = value));
        y += spacing;

        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Separator Style: " + config.separatorStyle.name()),
                button -> {
                    TooltipConfig.SeparatorStyle[] styles = TooltipConfig.SeparatorStyle.values();
                    int currentIndex = config.separatorStyle.ordinal();
                    int nextIndex = (currentIndex + 1) % styles.length;
                    config.separatorStyle = styles[nextIndex];
                    button.setMessage(Text.literal("Separator Style: " + config.separatorStyle.name()));
                }
        ).dimensions(20, y, 200, 20).build());
        y += spacing;

        // Производительность
        this.addDrawableChild(createCheckbox(20, y, "Enable Caching",
                config.enableCaching, (value) -> config.enableCaching = value));
        y += spacing;

        this.addDrawableChild(new SliderWidget(20, y, 200, 20,
                Text.literal("Cache Duration: "), (config.cacheExpirationMs - 50) / 950.0) {
            @Override
            protected void updateMessage() {
                int ms = 50 + (int)(value * 950);
                setMessage(Text.literal("Cache Duration: " + ms + "ms"));
            }

            @Override
            protected void applyValue() {
                config.cacheExpirationMs = 50 + (int)(value * 950);
            }
        });
        y += spacing;

        // Дополнительная информация
        this.addDrawableChild(createCheckbox(20, y, "Show Attribute Modifiers",
                config.showAttributeModifiers, (value) -> config.showAttributeModifiers = value));
    }

    private void renderDebugSettings(int startY, int spacing) {
        int y = startY;

        this.addDrawableChild(createCheckbox(20, y, "Debug Mode",
                config.debugMode, (value) -> config.debugMode = value));
        y += spacing;

        this.addDrawableChild(createCheckbox(20, y, "Show NBT Size",
                config.showNBTSize, (value) -> config.showNBTSize = value));
        y += spacing;

        this.addDrawableChild(createCheckbox(20, y, "Show Component Count",
                config.showComponentCount, (value) -> config.showComponentCount = value));
        y += spacing;

        // Исправленная кнопка сброса настроек
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Reset to Defaults").styled(s -> s.withColor(0xFF5555)),
                button -> {
                    // Используем публичный метод вместо прямого доступа к INSTANCE
                    TooltipConfig.resetToDefaults();
                    this.client.setScreen(new ModMenuConfigScreen(parent));
                }
        ).dimensions(20, y + spacing * 2, 200, 20).build());
    }

    private void updateMaxEnchantColor() {
        if (colorRedSlider == null || colorGreenSlider == null || colorBlueSlider == null) {
            return;
        }

        int r = Math.max(0, Math.min(255, (int)(colorRedSlider.getValue() * 255)));
        int g = Math.max(0, Math.min(255, (int)(colorGreenSlider.getValue() * 255)));
        int b = Math.max(0, Math.min(255, (int)(colorBlueSlider.getValue() * 255)));
        config.maxEnchantColor = (r << 16) | (g << 8) | b;
    }

    private ClickableWidget createCheckbox(int x, int y, String label, boolean checked, CheckboxCallback callback) {
        return ButtonWidget.builder(
                Text.literal((checked ? "☑ " : "☐ ") + label),
                (button) -> {
                    boolean newValue = !button.getMessage().getString().startsWith("☑");
                    callback.onValueChange(newValue);
                    button.setMessage(Text.literal((newValue ? "☑ " : "☐ ") + label));
                }
        ).dimensions(x, y, 200, 20).build();
    }

    @FunctionalInterface
    private interface CheckboxCallback {
        void onValueChange(boolean value);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 10, 0xFFFFFF);

        // Информация о текущей категории
        context.drawTextWithShadow(textRenderer, "Category: " + currentCategory.name,
                20, height - 50, 0xFFFFFF);

        // Предпросмотр цвета для энчантов
        if (currentCategory == ConfigCategory.ENCHANTMENTS && colorRedSlider != null) {
            int previewX = 230;
            int previewY = 180;
            context.fill(previewX, previewY, previewX + 40, previewY + 40, 0xFF000000 | config.maxEnchantColor);
            context.drawText(textRenderer, "Preview", previewX, previewY - 10, 0xFFFFFF, false);
        }

        // Индикатор производительности
        if (currentCategory == ConfigCategory.DEBUG) {
            String perfMode = config.reducedMotion ? "Performance Mode: ON" : "Performance Mode: OFF";
            int color = config.reducedMotion ? 0x55FF55 : 0xFF5555;
            context.drawTextWithShadow(textRenderer, perfMode, width - 150, height - 50, color);
        }
    }

    @Override
    public void close() {
        TooltipConfig.save();
        this.client.setScreen(parent);
    }

    private class CustomSlider extends SliderWidget {
        private final ModMenuConfigScreen parent;

        public CustomSlider(ModMenuConfigScreen parent, int x, int y, int width, int height, Text text, double value) {
            super(x, y, width, height, text, value);
            this.parent = parent;
        }

        @Override
        protected void updateMessage() {
            String label = getMessage().getString().split(":")[0];
            int colorValue = Math.max(0, Math.min(255, (int)(value * 255)));
            setMessage(Text.literal(label + ": " + colorValue));
        }

        @Override
        protected void applyValue() {
            parent.updateMaxEnchantColor();
        }

        public double getValue() {
            return this.value;
        }

        public void setValue(double value) {
            this.value = Math.max(0.0, Math.min(1.0, value));
            updateMessage();
        }
    }
}