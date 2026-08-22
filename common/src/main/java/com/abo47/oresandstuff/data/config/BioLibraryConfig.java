package com.abo47.oresandstuff.data.config;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

import com.abo47.oresandstuff.OresAndStuffMod;
import com.abo47.oresandstuff.data.EntityScanEntry;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public final class BioLibraryConfig {
    private static final String FOLDER = "biolibrary";
    private static final Map<ResourceLocation, EntityScanEntry> ENTRIES = new HashMap<>();
    private static boolean loaded;

    private BioLibraryConfig() {
    }

    public static void ensureGenerated() {
        Map<String, String> defaults = new LinkedHashMap<>();
        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
            if (id == null) {
                continue;
            }
            defaults.put(fileName(id), defaultJson(id, type));
        }
        ConfigAssets.generateDefaults(FOLDER, defaults);
    }

    public static synchronized void reload() {
        loaded = true;
        ENTRIES.clear();
        for (Path file : ConfigAssets.listJson(FOLDER)) {
            try {
                JsonObject root = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
                ResourceLocation id = null;
                if (root.has("id")) {
                    id = ResourceLocation.tryParse(root.get("id").getAsString());
                }
                if (id == null) {
                    id = ResourceLocation.tryParse(file.getFileName().toString().replace(".json", "").replace('_', ':'));
                }
                String title = root.has("title") ? root.get("title").getAsString() : id.getPath();
                String category = root.has("category") ? root.get("category").getAsString() : "Misc";
                String summary = root.has("summary") ? root.get("summary").getAsString() : "No scan data.";
                List<String> facts = new ArrayList<>();
                if (root.has("facts") && root.get("facts").isJsonArray()) {
                    for (JsonElement fact : root.getAsJsonArray("facts")) {
                        facts.add(fact.getAsString());
                    }
                }
                ENTRIES.put(id, new EntityScanEntry(id, title, category, summary, facts));
            } catch (Exception e) {
                OresAndStuffMod.LOGGER.error("Failed to parse bio library config file {}", file, e);
            }
        }
    }

    public static synchronized EntityScanEntry get(ResourceLocation entityId) {
        ensureLoaded();
        return ENTRIES.get(entityId);
    }

    public static synchronized List<EntityScanEntry> entries() {
        ensureLoaded();
        return new ArrayList<>(ENTRIES.values());
    }

    public static synchronized EntityScanEntry entryFor(EntityType<?> type, ResourceLocation id) {
        EntityScanEntry entry = get(id);
        if (entry != null) {
            return entry;
        }
        return new EntityScanEntry(id, type.getDescription().getString(), categoryFor(type.getCategory()), "No scan data.", List.of());
    }

    private static void ensureLoaded() {
        if (!loaded) {
            reload();
        }
    }

    private static String defaultJson(ResourceLocation id, EntityType<?> type) {
        JsonObject root = new JsonObject();
        root.addProperty("id", id.toString());
        root.addProperty("title", type.getDescription().getString());
        root.addProperty("category", categoryFor(type.getCategory()));
        root.addProperty("summary", "No scan data.");
        JsonArray facts = new JsonArray();
        root.add("facts", facts);
        return ConfigAssets.pretty(root);
    }

    private static String categoryFor(MobCategory category) {
        if (category == null) {
            return "Misc";
        }
        return switch (category) {
            case MONSTER -> "Hostile";
            case CREATURE, AXOLOTLS -> "Fauna";
            case AMBIENT -> "Ambient";
            case UNDERGROUND_WATER_CREATURE, WATER_CREATURE, WATER_AMBIENT -> "Water";
            default -> "Misc";
        };
    }

    private static String fileName(ResourceLocation id) {
        return id.getNamespace() + "_" + id.getPath().replace('/', '_') + ".json";
    }
}