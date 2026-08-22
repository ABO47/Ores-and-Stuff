package com.abo47.oresandstuff.data.config;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.Tier;

import com.abo47.oresandstuff.OresAndStuffMod;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Asset-driven manual node extraction tools. Every item in the game can be
 * turned into a node extractor by listing its id in a JSON file inside the
 * "pickaxes" config folder, e.g.:
 *
 * <pre>
 * {
 *   "tools": [
 *     {
 *       "item": "minecraft:iron_pickaxe",
 *       "extract_amount": 1,
 *       "cooldown_ticks": 70,
 *       "durability_cost": 1
 *     }
 *   ]
 * }
 * </pre>
 *
 * The node's quality percentage is the output multiplier: a 34% node yields
 * 0.34x per extraction and cools down proportionally faster, a 200% node
 * yields 2x. Any pickaxe not listed explicitly still works using tier-derived
 * fallback values, so modded pickaxes function out of the box.
 */
public final class PickaxeToolConfig {
    private static final String FOLDER = "pickaxes";
    private static final Map<ResourceLocation, PickaxeSpec> SPECS = new HashMap<>();
    private static boolean loaded;

    private PickaxeToolConfig() {
    }

    public static synchronized void ensureLoaded() {
        if (loaded) {
            return;
        }
        loaded = true;
        generateDefaults();

        Map<ResourceLocation, PickaxeSpec> parsed = new HashMap<>();
        for (Path file : ConfigAssets.listJson(FOLDER)) {
            try {
                JsonObject root = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
                if (!root.has("tools") || !root.get("tools").isJsonArray()) {
                    continue;
                }
                for (JsonElement element : root.getAsJsonArray("tools")) {
                    if (!element.isJsonObject()) {
                        continue;
                    }
                    PickaxeSpec spec = parseSpec(element.getAsJsonObject());
                    if (spec != null) {
                        parsed.put(spec.itemId(), spec);
                    }
                }
            } catch (Exception e) {
                OresAndStuffMod.LOGGER.error("Failed to parse pickaxe config file {}", file, e);
            }
        }
        SPECS.clear();
        SPECS.putAll(parsed);
        OresAndStuffMod.LOGGER.info("Loaded {} configured extraction tool(s)", SPECS.size());
    }

    /**
     * Whether the item can extract ore nodes. True for items listed in the
     * config and for any pickaxe (tier-derived fallback).
     */
    public static boolean isExtractor(Item item) {
        return specFor(item).isPresent();
    }

    /**
     * Resolves the extraction profile for an item: explicit config entry
     * first, tier-derived fallback for pickaxes second.
     */
    public static Optional<PickaxeSpec> specFor(Item item) {
        if (item == null) {
            return Optional.empty();
        }
        ensureLoaded();
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        PickaxeSpec spec = SPECS.get(id);
        if (spec != null) {
            return Optional.of(spec);
        }
        if (item instanceof PickaxeItem pickaxe) {
            return Optional.of(fallbackFor(pickaxe));
        }
        return Optional.empty();
    }

    private static PickaxeSpec fallbackFor(PickaxeItem pickaxe) {
        int level = Math.max(0, Math.min(4, tierLevel(pickaxe)));
        int cooldown = 120 - level * 18;
        int durability = Math.max(1, 2 - level / 2);
        return new PickaxeSpec(BuiltInRegistries.ITEM.getKey(pickaxe),
                1, cooldown, durability);
    }

    private static int tierLevel(PickaxeItem pickaxe) {
        try {
            Tier tier = pickaxe.getTier();
            if (tier == null) {
                return 0;
            }
            return tier.getLevel();
        } catch (Exception ignored) {
            return 0;
        }
    }

    private static PickaxeSpec parseSpec(JsonObject root) {
        if (!root.has("item")) {
            return null;
        }
        ResourceLocation itemId = ResourceLocation.tryParse(root.get("item").getAsString());
        if (itemId == null) {
            return null;
        }
        int extractAmount = Math.max(1, intValue(root, "extract_amount", 1));
        int cooldownTicks = Math.max(1, intValue(root, "cooldown_ticks", 60));
        int durabilityCost = Math.max(0, intValue(root, "durability_cost", 1));
        return new PickaxeSpec(itemId, extractAmount, cooldownTicks, durabilityCost);
    }

    private static void generateDefaults() {
        Path folder = ConfigAssets.createFolder(FOLDER);
        Path file = folder.resolve("vanilla.json");
        if (Files.isRegularFile(file)) {
            return;
        }
        try {
            Files.writeString(file, defaultJson(), StandardCharsets.UTF_8);
            OresAndStuffMod.LOGGER.info("Generated default pickaxe config {}", file);
        } catch (Exception e) {
            OresAndStuffMod.LOGGER.error("Failed to write default pickaxe config {}", file, e);
        }
    }

    private static String defaultJson() {
        JsonObject root = new JsonObject();
        JsonArray tools = new JsonArray();
        tools.add(defaultTool("minecraft:wooden_pickaxe", 1, 60, 2));
        tools.add(defaultTool("minecraft:stone_pickaxe", 1, 50, 2));
        tools.add(defaultTool("minecraft:iron_pickaxe", 1, 40, 1));
        tools.add(defaultTool("minecraft:golden_pickaxe", 1, 30, 3));
        tools.add(defaultTool("minecraft:diamond_pickaxe", 1, 20, 1));
        tools.add(defaultTool("minecraft:netherite_pickaxe", 1, 10, 1));
        root.add("tools", tools);
        return ConfigAssets.pretty(root);
    }

    private static JsonObject defaultTool(String item, int extractAmount, int cooldownTicks, int durabilityCost) {
        JsonObject tool = new JsonObject();
        tool.addProperty("item", item);
        tool.addProperty("extract_amount", extractAmount);
        tool.addProperty("cooldown_ticks", cooldownTicks);
        tool.addProperty("durability_cost", durabilityCost);
        return tool;
    }

    private static int intValue(JsonObject root, String key, int fallback) {
        try {
            return root.has(key) ? root.get(key).getAsInt() : fallback;
        } catch (Exception e) {
            return fallback;
        }
    }

    public record PickaxeSpec(ResourceLocation itemId,
                              int extractAmount,
                              int cooldownTicks,
                              int durabilityCost) {

        /** The quality percentage is the output multiplier: 34% = 0.34x. */
        public double amountMultiplierFor(double qualityPercent) {
            return Math.max(0.01, qualityPercent / 100.0);
        }

        /** Higher quality mines faster - cooldown scales inversely with quality. */
        public int cooldownFor(double qualityPercent) {
            double mult = Math.max(0.1, qualityPercent / 100.0);
            return Math.max(1, (int) Math.round(cooldownTicks / mult));
        }
    }

    public static List<PickaxeSpec> configuredSpecs() {
        ensureLoaded();
        return List.copyOf(SPECS.values());
    }

    public static List<String> configuredItemIds() {
        ensureLoaded();
        List<String> ids = new ArrayList<>();
        for (ResourceLocation id : SPECS.keySet()) {
            ids.add(id.toString());
        }
        ids.sort(String::compareTo);
        return ids;
    }
}
