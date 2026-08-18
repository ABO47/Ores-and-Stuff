package com.abo47.oresandstuff.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;

import com.abo47.oresandstuff.OresAndStuffMod;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class EntityScanDataManager extends SimpleJsonResourceReloadListener {
    public static final EntityScanDataManager INSTANCE = new EntityScanDataManager();

    private static final String DIR = "entity_scans";

    private final Map<ResourceLocation, EntityScanEntry> entries = new HashMap<>();

    private EntityScanDataManager() {
        super(new GsonBuilder().create(), DIR);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> objects, ResourceManager resourceManager, ProfilerFiller profiler) {
        entries.clear();
        for (Map.Entry<ResourceLocation, JsonElement> e : objects.entrySet()) {
            try {
                JsonObject o = e.getValue().getAsJsonObject();
                EntityScanEntry entry = parse(o, e.getKey());
                entries.put(entry.entityId(), entry);
            } catch (Exception ex) {
                OresAndStuffMod.LOGGER.error("Failed to parse entity scan file {}", e.getKey(), ex);
            }
        }
        OresAndStuffMod.LOGGER.info("Loaded {} entity scan entries", entries.size());
    }

    private EntityScanEntry parse(JsonObject root, ResourceLocation fallback) {
        ResourceLocation entityId = new ResourceLocation(GsonHelper.getAsString(root, "entity", fallback.toString()));
        String title = GsonHelper.getAsString(root, "title", entityId.getPath());
        String category = GsonHelper.getAsString(root, "category", "Fauna");
        String summary = GsonHelper.getAsString(root, "summary", "No summary.");
        List<String> facts = new ArrayList<>();
        if (root.has("facts") && root.get("facts").isJsonArray()) {
            root.getAsJsonArray("facts").forEach(j -> facts.add(j.getAsString()));
        }
        return new EntityScanEntry(entityId, title, category, summary, facts);
    }

    public Optional<EntityScanEntry> get(ResourceLocation entityId) {
        return Optional.ofNullable(entries.get(entityId));
    }

    public Map<ResourceLocation, EntityScanEntry> all() {
        return Collections.unmodifiableMap(entries);
    }
}
