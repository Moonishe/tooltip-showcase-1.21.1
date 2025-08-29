package com.tooltip.render.content;

import com.tooltip.config.TooltipConfig;
import com.tooltip.render.processors.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class TooltipContentBuilder {
    private static final Logger LOGGER = LoggerFactory.getLogger("TooltipAnimated");

    private final MinecraftClient client;
    private final EnchantmentProcessor enchantmentProcessor;
    private final DurabilityProcessor durabilityProcessor;
    private final PotionEffectProcessor potionProcessor;
    private final ShulkerBoxProcessor shulkerProcessor;
    private final ItemInfoProcessor itemInfoProcessor;
    private final SpecialItemProcessor specialItemProcessor;
    private final TooltipConfig config; // ДОБАВЛЕНО: сохраняем ссылку на конфиг

    // Кэш для оптимизации
    private ItemStack lastProcessedStack = ItemStack.EMPTY;
    private List<Text> cachedContent = new ArrayList<>();
    private long lastProcessTime = 0;
    private static final long CACHE_DURATION_MS = 100;

    public TooltipContentBuilder(MinecraftClient client) {
        this.client = client;
        this.config = TooltipConfig.getInstance(); // ДОБАВЛЕНО: инициализация конфига
        this.enchantmentProcessor = new EnchantmentProcessor(client);
        this.durabilityProcessor = new DurabilityProcessor();
        this.potionProcessor = new PotionEffectProcessor(client);
        this.shulkerProcessor = new ShulkerBoxProcessor(client);
        this.itemInfoProcessor = new ItemInfoProcessor();
        this.specialItemProcessor = new SpecialItemProcessor(client);

        LOGGER.info("TooltipContentBuilder initialized");
    }

    public List<Text> buildTooltipContent(ItemStack stack, TooltipConfig config) {
        if (stack.isEmpty()) {
            return new ArrayList<>();
        }

        // Проверка кэша (кроме прочности, она меняется часто)
        if (config.enableCaching && isCacheValid(stack) && !stack.isDamageable()) {
            return new ArrayList<>(cachedContent);
        }

        List<Text> lines = new ArrayList<>();

        try {
            // === ОСНОВНАЯ ИНФОРМАЦИЯ ===

            // Название предмета с редкостью
            lines.add(itemInfoProcessor.getItemNameWithRarity(stack));

            // ID предмета (если включено)
            if (config.showItemId) {
                itemInfoProcessor.processItemId(stack, true).ifPresent(lines::add);
            }

            // === СПЕЦИАЛЬНЫЕ ПРЕДМЕТЫ ===
            int specialItemsAdded = addSpecialItemInfo(stack, lines, config);

            // Добавляем разделитель после специальных предметов, если они были добавлены
            if (specialItemsAdded > 0 && config.useSeparators) {
                addSeparator(lines, config);
            }

            // === КОНТЕЙНЕРЫ ===

            // Шалкер бокс
            List<Text> shulkerLines = shulkerProcessor.processShulkerContents(stack, config);
            if (!shulkerLines.isEmpty()) {
                if (shouldAddSeparator(lines, config)) {
                    addSeparator(lines, config);
                }
                lines.addAll(shulkerLines);
            }

            // === МОДИФИКАТОРЫ ===

            // Зачарования
            List<Text> enchantLines = enchantmentProcessor.processEnchantments(stack, config);
            if (!enchantLines.isEmpty()) {
                if (shouldAddSeparator(lines, config)) {
                    addSeparator(lines, config);
                }
                lines.addAll(enchantLines);
            }

            // Эффекты зелий
            List<Text> potionLines = potionProcessor.processPotionEffects(stack);
            if (!potionLines.isEmpty()) {
                if (shouldAddSeparator(lines, config)) {
                    addSeparator(lines, config);
                }
                lines.addAll(potionLines);
            }

            // === ХАРАКТЕРИСТИКИ ===

            boolean addedCharacteristics = false;

            // Еда
            var foodInfo = itemInfoProcessor.processFoodInfo(stack);
            if (foodInfo.isPresent()) {
                if (shouldAddSeparator(lines, config)) {
                    addSeparator(lines, config);
                }
                lines.add(foodInfo.get());
                addedCharacteristics = true;
            }

            // Броня
            var armorInfo = itemInfoProcessor.processArmorInfo(stack);
            if (armorInfo.isPresent()) {
                if (!addedCharacteristics && shouldAddSeparator(lines, config)) {
                    addSeparator(lines, config);
                }
                lines.add(armorInfo.get());
            }

            // === СОСТОЯНИЕ ===

            // Прочность (всегда последняя перед debug info)
            if (stack.isDamageable()) {
                Text durabilityText = durabilityProcessor.createDurabilityText(stack, config.durabilityMode);
                if (durabilityText != null && !durabilityText.getString().isEmpty()) {
                    if (shouldAddSeparator(lines, config)) {
                        addSeparator(lines, config);
                    }
                    lines.add(durabilityText);
                }
            }

            // === DEBUG INFO ===

            // Дополнительная информация для отладки (если включен режим разработчика)
            if (config.debugMode) {
                addDebugInfo(stack, lines, config); // ИЗМЕНЕНО: передаем config
            }

            // Обновляем кэш
            if (config.enableCaching) {
                updateCache(stack, lines);
            }

        } catch (Exception e) {
            LOGGER.error("Failed to build tooltip for {}", stack.getItem(), e);
            lines.clear();
            lines.add(stack.getName());
            lines.add(Text.literal("Error: " + e.getMessage()).formatted(Formatting.RED));

            if (config.debugMode) {
                lines.add(Text.literal("Stack trace in logs").formatted(Formatting.DARK_RED));
                e.printStackTrace();
            }
        }

        return lines;
    }

    /**
     * Добавляет информацию о специальных предметах
     * @return количество добавленных строк
     */
    private int addSpecialItemInfo(ItemStack stack, List<Text> lines, TooltipConfig config) {
        int initialSize = lines.size();

        // Порядок отображения специальных предметов
        // (от наиболее важных к менее важным)

        // 1. Музыкальные пластинки
        specialItemProcessor.processMusicDisc(stack).ifPresent(lines::add);

        // 2. Подписанные книги (могут добавить несколько строк)
        List<Text> bookLines = specialItemProcessor.processWrittenBook(stack);
        lines.addAll(bookLines);

        // 3. Фейерверки
        specialItemProcessor.processFireworks(stack).ifPresent(lines::add);

        // 4. Козий рог
        specialItemProcessor.processGoatHorn(stack).ifPresent(lines::add);

        // 5. Узоры флага
        specialItemProcessor.processBannerPattern(stack).ifPresent(lines::add);

        // 6. Компас с лодстоуном
        specialItemProcessor.processCompass(stack).ifPresent(lines::add);

        // 7. Карты
        specialItemProcessor.processMap(stack).ifPresent(lines::add);

        int addedLines = lines.size() - initialSize;

        if (config.debugMode && addedLines > 0) {
            LOGGER.debug("Added {} special item info lines for {}", addedLines, stack.getItem());
        }

        return addedLines;
    }

    /**
     * Проверяет, нужно ли добавить разделитель
     */
    private boolean shouldAddSeparator(List<Text> lines, TooltipConfig config) {
        if (!config.useSeparators || lines.isEmpty()) {
            return false;
        }

        // Не добавляем разделитель после заголовка или другого разделителя
        if (!lines.isEmpty()) {
            Text lastLine = lines.get(lines.size() - 1);
            String lastText = lastLine.getString();

            // Проверяем, не является ли последняя строка разделителем
            return !lastText.equals("─────────") &&
                    !lastText.equals("═════════") &&
                    !lastText.equals("·········") &&
                    !lastText.isEmpty();
        }

        return true;
    }

    /**
     * Добавляет разделитель в зависимости от настроек
     */
    private void addSeparator(List<Text> lines, TooltipConfig config) {
        switch (config.separatorStyle) {
            case LINE:
                lines.add(Text.literal("─────────").formatted(Formatting.DARK_GRAY));
                break;
            case DOUBLE_LINE:
                lines.add(Text.literal("═════════").formatted(Formatting.DARK_GRAY));
                break;
            case DOTS:
                lines.add(Text.literal("·········").formatted(Formatting.DARK_GRAY));
                break;
            case EMPTY_LINE:
                lines.add(Text.empty());
                break;
            case NONE:
            default:
                // Не добавляем разделитель
                break;
        }
    }

    /**
     * Добавляет отладочную информацию
     */
    private void addDebugInfo(ItemStack stack, List<Text> lines, TooltipConfig config) { // ИЗМЕНЕНО: добавлен параметр config
        // Разделитель перед debug секцией
        lines.add(Text.empty());
        lines.add(Text.literal("═══ DEBUG ═══").formatted(Formatting.DARK_PURPLE));

        // Компоненты данных
        var components = stack.getComponents();
        if (components != null && !components.isEmpty()) {
            if (config.showComponentCount) {
                lines.add(Text.literal("Components: " + components.size()).formatted(Formatting.DARK_GRAY));
            }

            if (config.showNBTSize) {
                int dataSize = components.toString().length();
                lines.add(Text.literal("Data Size: ~" + formatBytes(dataSize)).formatted(Formatting.DARK_GRAY));
            }
        }

        // Базовая информация
        lines.add(Text.literal("Stack: " + stack.getCount() + "/" + stack.getMaxCount())
                .formatted(Formatting.DARK_GRAY));

        // Прочность (для debug)
        if (stack.isDamageable()) {
            lines.add(Text.literal("Damage: " + stack.getDamage() + "/" + stack.getMaxDamage())
                    .formatted(Formatting.DARK_GRAY));
        }

        // Редкость
        lines.add(Text.literal("Rarity: " + stack.getRarity().name())
                .formatted(getRarityColor(stack.getRarity())));

        // Дополнительные флаги
        List<String> flags = new ArrayList<>();

        if (stack.hasGlint()) {
            flags.add("Glint");
        }

        var customName = stack.get(net.minecraft.component.DataComponentTypes.CUSTOM_NAME);
        if (customName != null) {
            flags.add("CustomName");
        }

        var repairCost = stack.get(net.minecraft.component.DataComponentTypes.REPAIR_COST);
        if (repairCost != null && repairCost > 0) {
            flags.add("RepairCost:" + repairCost);
        }

        if (!flags.isEmpty()) {
            lines.add(Text.literal("Flags: " + String.join(", ", flags))
                    .formatted(Formatting.AQUA));
        }

        // Хэш для технической отладки
        lines.add(Text.literal("Hash: 0x" + Integer.toHexString(stack.hashCode()).toUpperCase())
                .formatted(Formatting.DARK_GRAY));
    }

    /**
     * Форматирует размер в байтах в читаемый формат
     */
    private String formatBytes(int chars) {
        // Примерная оценка: 1 char = 2 bytes в Java
        int bytes = chars * 2;
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        } else {
            return String.format("%.1f MB", bytes / (1024.0 * 1024));
        }
    }

    /**
     * Возвращает цвет для редкости предмета
     */
    private Formatting getRarityColor(net.minecraft.util.Rarity rarity) {
        switch (rarity) {
            case UNCOMMON: return Formatting.YELLOW;
            case RARE: return Formatting.AQUA;
            case EPIC: return Formatting.LIGHT_PURPLE;
            default: return Formatting.WHITE;
        }
    }

    /**
     * Проверяет валидность кэша
     */
    private boolean isCacheValid(ItemStack stack) {
        if (lastProcessedStack.isEmpty() || stack.isEmpty()) {
            return false;
        }

        long currentTime = System.currentTimeMillis();
        // ИСПРАВЛЕНО: используем поле config или значение по умолчанию
        long cacheExpiration = config != null ? config.cacheExpirationMs : CACHE_DURATION_MS;
        if (currentTime - lastProcessTime > cacheExpiration) {
            return false;
        }

        // Проверяем идентичность стеков
        return ItemStack.areEqual(stack, lastProcessedStack);
    }

    /**
     * Обновляет кэш
     */
    private void updateCache(ItemStack stack, List<Text> lines) {
        lastProcessedStack = stack.copy();
        cachedContent = new ArrayList<>(lines);
        lastProcessTime = System.currentTimeMillis();
    }

    /**
     * Очищает кэш
     */
    public void clearCache() {
        lastProcessedStack = ItemStack.EMPTY;
        cachedContent.clear();
        lastProcessTime = 0;
    }

    /**
     * Принудительно обновляет кэш для текущего предмета
     */
    public void forceRefresh() {
        lastProcessTime = 0;
    }

    /**
     * Проверяет, закэширован ли указанный предмет
     */
    public boolean isCached(ItemStack stack) {
        long cacheExpiration = config != null ? config.cacheExpirationMs : CACHE_DURATION_MS;
        return !lastProcessedStack.isEmpty() &&
                ItemStack.areEqual(stack, lastProcessedStack) &&
                (System.currentTimeMillis() - lastProcessTime) <= cacheExpiration;
    }
}