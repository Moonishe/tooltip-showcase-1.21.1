package com.tooltip.render.processors;

import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FireworksComponent;
import net.minecraft.component.type.JukeboxPlayableComponent;
import net.minecraft.component.type.WrittenBookContentComponent;
import net.minecraft.item.BannerPatternItem;
import net.minecraft.item.GoatHornItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class SpecialItemProcessor {
    private final MinecraftClient client;

    // Карта соответствия предметов музыкальных дисков их трекам
    private static final Map<net.minecraft.item.Item, String> MUSIC_DISC_TRACKS = new HashMap<>();

    static {
        MUSIC_DISC_TRACKS.put(Items.MUSIC_DISC_13, "C418 - 13");
        MUSIC_DISC_TRACKS.put(Items.MUSIC_DISC_CAT, "C418 - cat");
        MUSIC_DISC_TRACKS.put(Items.MUSIC_DISC_BLOCKS, "C418 - blocks");
        MUSIC_DISC_TRACKS.put(Items.MUSIC_DISC_CHIRP, "C418 - chirp");
        MUSIC_DISC_TRACKS.put(Items.MUSIC_DISC_FAR, "C418 - far");
        MUSIC_DISC_TRACKS.put(Items.MUSIC_DISC_MALL, "C418 - mall");
        MUSIC_DISC_TRACKS.put(Items.MUSIC_DISC_MELLOHI, "C418 - mellohi");
        MUSIC_DISC_TRACKS.put(Items.MUSIC_DISC_STAL, "C418 - stal");
        MUSIC_DISC_TRACKS.put(Items.MUSIC_DISC_STRAD, "C418 - strad");
        MUSIC_DISC_TRACKS.put(Items.MUSIC_DISC_WARD, "C418 - ward");
        MUSIC_DISC_TRACKS.put(Items.MUSIC_DISC_11, "C418 - 11");
        MUSIC_DISC_TRACKS.put(Items.MUSIC_DISC_WAIT, "C418 - wait");
        MUSIC_DISC_TRACKS.put(Items.MUSIC_DISC_OTHERSIDE, "Lena Raine - otherside");
        MUSIC_DISC_TRACKS.put(Items.MUSIC_DISC_5, "Samuel Åberg - 5");
        MUSIC_DISC_TRACKS.put(Items.MUSIC_DISC_PIGSTEP, "Lena Raine - Pigstep");
        MUSIC_DISC_TRACKS.put(Items.MUSIC_DISC_RELIC, "Aaron Cherof - Relic");
        MUSIC_DISC_TRACKS.put(Items.MUSIC_DISC_CREATOR, "Lena Raine - Creator");
        MUSIC_DISC_TRACKS.put(Items.MUSIC_DISC_CREATOR_MUSIC_BOX, "Lena Raine - Creator (Music Box)");
        MUSIC_DISC_TRACKS.put(Items.MUSIC_DISC_PRECIPICE, "Aaron Cherof - Precipice");
    }

    public SpecialItemProcessor(MinecraftClient client) {
        this.client = client;
    }

    // 1. Фейерверки
    public Optional<Text> processFireworks(ItemStack stack) {
        FireworksComponent fireworks = stack.get(DataComponentTypes.FIREWORKS);
        if (fireworks == null || fireworks.flightDuration() == 0) {
            return Optional.empty();
        }

        return Optional.of(
                Text.literal("Flight Duration: ")
                        .formatted(Formatting.GRAY)
                        .append(Text.literal(String.valueOf(fireworks.flightDuration()))
                                .formatted(Formatting.WHITE))
        );
    }

    // 2. Подписанные книги - ИСПРАВЛЕНО
    public List<Text> processWrittenBook(ItemStack stack) {
        List<Text> lines = new ArrayList<>();
        WrittenBookContentComponent content = stack.get(DataComponentTypes.WRITTEN_BOOK_CONTENT);
        if (content == null) {
            return lines;
        }

        // Автор - author() возвращает String, а не Text
        Text authorLine = Text.literal("Author: ")
                .formatted(Formatting.GRAY)
                .append(Text.literal(content.author()).formatted(Formatting.WHITE));
        lines.add(authorLine);

        // Поколение книги
        String generationText;
        Formatting generationColor;
        switch (content.generation()) {
            case 0:
                generationText = "Original";
                generationColor = Formatting.GREEN;
                break;
            case 1:
                generationText = "Copy of original";
                generationColor = Formatting.YELLOW;
                break;
            case 2:
                generationText = "Copy of copy";
                generationColor = Formatting.GOLD;
                break;
            default:
                generationText = "Tattered";
                generationColor = Formatting.RED;
                break;
        }
        lines.add(Text.literal(generationText).formatted(generationColor));

        return lines;
    }

    // 3. Козий рог
    public Optional<Text> processGoatHorn(ItemStack stack) {
        if (!(stack.getItem() instanceof GoatHornItem)) {
            return Optional.empty();
        }

        var instrument = stack.get(DataComponentTypes.INSTRUMENT);
        if (instrument == null) {
            return Optional.empty();
        }

        // Получаем ID инструмента и форматируем красиво
        String instrumentId = instrument.getIdAsString();
        if (instrumentId != null) {
            String cleanId = instrumentId.replace("minecraft:", "").replace("_", " ");
            // Делаем первую букву заглавной
            if (!cleanId.isEmpty()) {
                cleanId = cleanId.substring(0, 1).toUpperCase() + cleanId.substring(1);
            }

            return Optional.of(
                    Text.literal("Sound: ")
                            .formatted(Formatting.GRAY)
                            .append(Text.literal(cleanId)
                                    .formatted(Formatting.AQUA))
            );
        }

        return Optional.empty();
    }

    // 4. Музыкальные пластинки
    public Optional<Text> processMusicDisc(ItemStack stack) {
        // Получаем название трека из нашей карты
        String trackName = MUSIC_DISC_TRACKS.get(stack.getItem());

        if (trackName == null) {
            return Optional.empty();
        }

        // Форматируем как в шалкер боксах
        Text discInfo = Text.literal("♪ ")
                .formatted(Formatting.LIGHT_PURPLE)
                .append(Text.literal(trackName)
                        .formatted(Formatting.GRAY));

        return Optional.of(discInfo);
    }

    // 5. Узоры флага
    public Optional<Text> processBannerPattern(ItemStack stack) {
        if (!(stack.getItem() instanceof BannerPatternItem patternItem)) {
            return Optional.empty();
        }

        // Получаем название узора
        String patternKey = patternItem.getTranslationKey();
        String patternName = patternKey.substring(patternKey.lastIndexOf('.') + 1)
                .replace("_", " ");

        // Делаем первую букву заглавной
        if (!patternName.isEmpty()) {
            patternName = patternName.substring(0, 1).toUpperCase() + patternName.substring(1);
        }

        return Optional.of(
                Text.literal("Pattern: ")
                        .formatted(Formatting.GRAY)
                        .append(Text.literal(patternName)
                                .formatted(Formatting.YELLOW))
        );
    }

    // 6. Компас (лодстоун)
    public Optional<Text> processCompass(ItemStack stack) {
        if (stack.getItem() != Items.COMPASS) {
            return Optional.empty();
        }

        var lodestone = stack.get(DataComponentTypes.LODESTONE_TRACKER);
        if (lodestone != null && lodestone.tracked()) {
            return Optional.of(
                    Text.literal("Lodestone Linked")
                            .formatted(Formatting.AQUA)
            );
        }

        return Optional.empty();
    }

    // 7. Карты - ИСПРАВЛЕНО
    public Optional<Text> processMap(ItemStack stack) {
        if (stack.getItem() != Items.FILLED_MAP) {
            return Optional.empty();
        }

        var mapId = stack.get(DataComponentTypes.MAP_ID);
        if (mapId != null) {
            // mapId.id() возвращает int, не нужно вызывать toString() на int
            return Optional.of(
                    Text.literal("Map #")
                            .formatted(Formatting.GRAY)
                            .append(Text.literal(String.valueOf(mapId.id()))
                                    .formatted(Formatting.WHITE))
            );
        }

        return Optional.empty();
    }
}