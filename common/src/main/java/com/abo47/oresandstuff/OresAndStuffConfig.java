package com.abo47.oresandstuff;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import com.abo47.oresandstuff.platform.Services;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public final class OresAndStuffConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static boolean loaded;
    private static final OresAndStuffConfigSections.Scanner SCANNER = new OresAndStuffConfigSections.Scanner();
    private static final OresAndStuffConfigSections.BioScan BIO_SCAN = new OresAndStuffConfigSections.BioScan();
    private static final OresAndStuffConfigSections.Miner MINER = new OresAndStuffConfigSections.Miner();
    private static final OresAndStuffConfigSections.Worldgen WORLDGEN = new OresAndStuffConfigSections.Worldgen();
    private static final OresAndStuffConfigSections.Debug DEBUG = new OresAndStuffConfigSections.Debug();

    private OresAndStuffConfig() {
    }

    public static synchronized void load() {
        if (loaded) {
            return;
        }
        loaded = true;
        Path file = configFile();
        if (Files.isRegularFile(file)) {
            try {
                JsonElement parsed = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8));
                if (parsed.isJsonObject()) {
                    read(parsed.getAsJsonObject());
                }
            } catch (Exception e) {
                OresAndStuffMod.LOGGER.warn("Failed reading Ores and Stuff config {}, keeping defaults", file, e);
            }
        }
        save();
    }

    public static synchronized void reload() {
        loaded = false;
        load();
    }

    public static OresAndStuffConfigSections.Scanner scanner() {
        load();
        return SCANNER;
    }

    public static OresAndStuffConfigSections.BioScan bioScan() {
        load();
        return BIO_SCAN;
    }

    public static OresAndStuffConfigSections.Miner miner() {
        load();
        return MINER;
    }

    public static OresAndStuffConfigSections.Worldgen worldgen() {
        load();
        return WORLDGEN;
    }

    public static OresAndStuffConfigSections.Debug debug() {
        load();
        return DEBUG;
    }

    public static synchronized void save() {
        JsonObject root = new JsonObject();
        root.add("scanner", SCANNER.write());
        root.add("bioScan", BIO_SCAN.write());
        root.add("miner", MINER.write());
        root.add("worldgen", WORLDGEN.write());
        root.add("debug", DEBUG.write());

        Path file = configFile();
        try {
            Path parent = file.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(file, GSON.toJson(root), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (Exception e) {
            OresAndStuffMod.LOGGER.warn("Failed writing Ores and Stuff config {}", file, e);
        }
    }

    private static void read(JsonObject root) {
        SCANNER.read(OresAndStuffConfigSections.object(root, "scanner"));
        BIO_SCAN.read(OresAndStuffConfigSections.object(root, "bioScan"));
        MINER.read(OresAndStuffConfigSections.object(root, "miner"));
        WORLDGEN.read(OresAndStuffConfigSections.object(root, "worldgen"));
        DEBUG.read(OresAndStuffConfigSections.object(root, "debug"));
    }

    private static Path configFile() {
        return Services.platform().configDir().resolve(OresAndStuffMod.MOD_ID).resolve("settings.json");
    }
}
