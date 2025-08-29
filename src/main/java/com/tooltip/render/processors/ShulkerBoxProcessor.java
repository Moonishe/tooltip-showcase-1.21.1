package com.tooltip.render.processors;

import com.tooltip.config.TooltipConfig;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ShulkerBoxProcessor {
    private final MinecraftClient client;

    public ShulkerBoxProcessor(MinecraftClient client) {
        this.client = client;
    }

    public List<Text> processShulkerContents(ItemStack stack, TooltipConfig config) {
        List<Text> lines = new ArrayList<>();

        if (!(stack.getItem() instanceof BlockItem blockItem)) return lines;
        if (!(blockItem.getBlock() instanceof ShulkerBoxBlock)) return lines;

        ContainerComponent container = stack.get(DataComponentTypes.CONTAINER);
        if (container == null) return lines;

        Map<Item, Integer> contents = new HashMap<>();
        container.iterateNonEmpty().forEach(itemStack -> {
            contents.merge(itemStack.getItem(), itemStack.getCount(), Integer::sum);
        });

        List<Map.Entry<Item, Integer>> sortedContents = contents.entrySet().stream()
                .sorted((a, b) -> {
                    String textA = a.getKey().getName().getString() + " x" + a.getValue();
                    String textB = b.getKey().getName().getString() + " x" + b.getValue();
                    return Integer.compare(client.textRenderer.getWidth(textB),
                            client.textRenderer.getWidth(textA));
                })
                .collect(Collectors.toList());

        int shown = 0;
        for (Map.Entry<Item, Integer> entry : sortedContents) {
            if (shown >= config.maxShulkerItems) break;

            Text itemText = Text.literal("")
                    .append(entry.getKey().getName())
                    .append(" x" + entry.getValue())
                    .formatted(Formatting.GRAY);

            lines.add(itemText);
            shown++;
        }

        int totalItems = contents.size();
        int hidden = totalItems - shown;
        if (hidden > 0) {
            lines.add(Text.literal("+" + hidden + " more items").formatted(Formatting.DARK_GRAY));
        }

        return lines;
    }
}