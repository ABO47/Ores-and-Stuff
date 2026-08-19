package com.abo47.oresandstuff;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public final class OresAndStuffConfigSections {
    private OresAndStuffConfigSections() {
    }

    public static final class Scanner {
        public int radiusCap = 512;
        public int maxResults = 6;
        public int cooldownTicks = 40;
        public int pulseDurationMs = 4200;
        public int pulseRangeBlocks = 224;
        public double pulseSpeedBlocksPerSec = 48.0;
        public int pulseWidthBlocks = 12;
        public int qualityPreset = 2;
        public int experimentalVisualMode = 1;
        public int holographicWidthBlocks = 100;
        public double holographicScanlineStrength = 0.0;
        public boolean scanSoundEnabled = true;
        public double scanSoundVolume = 0.85;
        public double scanSoundPitch = 1.08;
        public boolean pingSoundEnabled = true;
        public double pingSoundVolume = 0.35;
        public double pingSoundPitch = 1.32;

        void read(JsonObject root) {
            maxResults = clampInt(intValue(root, "scannerMaxResults", maxResults), 1, 24);
            cooldownTicks = clampInt(intValue(root, "scannerCooldownTicks", cooldownTicks), 0, 1200);
            pulseDurationMs = clampInt(intValue(root, "scannerPulseDurationMs", pulseDurationMs), 800, 20000);
            pulseRangeBlocks = clampInt(intValue(root, "scannerPulseRangeBlocks", pulseRangeBlocks), 32, 1024);
            pulseSpeedBlocksPerSec = clampDouble(doubleValue(root, "scannerPulseSpeedBlocksPerSec", pulseSpeedBlocksPerSec), 4.0, 512.0);
            pulseWidthBlocks = clampInt(intValue(root, "scannerPulseWidthBlocks", pulseWidthBlocks), 2, 64);
            qualityPreset = clampInt(intValue(root, "scannerQualityPreset", qualityPreset), 0, 3);
            experimentalVisualMode = clampInt(intValue(root, "experimentalScannerVisualMode", experimentalVisualMode), 0, 1);
            holographicWidthBlocks = clampInt(intValue(root, "scannerHolographicWidthBlocks", holographicWidthBlocks), 1, 256);
            holographicScanlineStrength = clampDouble(doubleValue(root, "scannerHolographicScanlineStrength", holographicScanlineStrength), 0.0, 2.0);
            scanSoundEnabled = bool(root, "scannerScanSoundEnabled", scanSoundEnabled);
            scanSoundVolume = clampDouble(doubleValue(root, "scannerScanSoundVolume", scanSoundVolume), 0.0, 2.0);
            scanSoundPitch = clampDouble(doubleValue(root, "scannerScanSoundPitch", scanSoundPitch), 0.1, 2.0);
            pingSoundEnabled = bool(root, "scannerPingSoundEnabled", pingSoundEnabled);
            pingSoundVolume = clampDouble(doubleValue(root, "scannerPingSoundVolume", pingSoundVolume), 0.0, 2.0);
            pingSoundPitch = clampDouble(doubleValue(root, "scannerPingSoundPitch", pingSoundPitch), 0.1, 2.0);
        }

        JsonObject write() {
            JsonObject root = new JsonObject();
            root.addProperty("scannerRadiusCap", radiusCap);
            root.addProperty("scannerMaxResults", maxResults);
            root.addProperty("scannerCooldownTicks", cooldownTicks);
            root.addProperty("scannerPulseRangeBlocks", pulseRangeBlocks);
            root.addProperty("scannerPulseSpeedBlocksPerSec", pulseSpeedBlocksPerSec);
            root.addProperty("scannerPulseWidthBlocks", pulseWidthBlocks);
            root.addProperty("scannerQualityPreset", qualityPreset);
            root.addProperty("experimentalScannerVisualMode", experimentalVisualMode);
            root.addProperty("scannerHolographicWidthBlocks", holographicWidthBlocks);
            root.addProperty("scannerHolographicScanlineStrength", holographicScanlineStrength);
            root.addProperty("scannerScanSoundEnabled", scanSoundEnabled);
            root.addProperty("scannerScanSoundVolume", scanSoundVolume);
            root.addProperty("scannerScanSoundPitch", scanSoundPitch);
            root.addProperty("scannerPingSoundEnabled", pingSoundEnabled);
            root.addProperty("scannerPingSoundVolume", pingSoundVolume);
            root.addProperty("scannerPingSoundPitch", pingSoundPitch);
            return root;
        }
    }

    public static final class BioScan {
        public int durationMs = 1000;
        public double drainMultiplier = 2.4;
        public int cooldownTicks = 40;
        public double rayStyle = 1.15;
        public boolean hideNonMobs = true;
        public List<String> hiddenEntities = new ArrayList<>();

        void read(JsonObject root) {
            durationMs = clampInt(intValue(root, "bioScanDurationMs", durationMs), 300, 15000);
            drainMultiplier = clampDouble(doubleValue(root, "bioScanDrainMultiplier", drainMultiplier), 0.5, 10.0);
            cooldownTicks = clampInt(intValue(root, "bioScanCooldownTicks", cooldownTicks), 0, 1200);
            rayStyle = clampDouble(doubleValue(root, "bioScanRayStyle", rayStyle), 0.5, 3.0);
            hideNonMobs = bool(root, "bioScanHideNonMobs", hideNonMobs);
            hiddenEntities = stringList(root, "bioScanHiddenEntities", hiddenEntities);
        }

        JsonObject write() {
            JsonObject root = new JsonObject();
            root.addProperty("bioScanDurationMs", durationMs);
            root.addProperty("bioScanDrainMultiplier", drainMultiplier);
            root.addProperty("bioScanCooldownTicks", cooldownTicks);
            root.addProperty("bioScanRayStyle", rayStyle);
            root.addProperty("bioScanHideNonMobs", hideNonMobs);
            JsonArray arr = new JsonArray();
            for (String s : hiddenEntities) {
                arr.add(s);
            }
            root.add("bioScanHiddenEntities", arr);
            return root;
        }
    }

    public static final class Worldgen {
        public boolean vanillaOresEnabled = false;
        public List<String> disabledNodeTypes = new ArrayList<>();

        void read(JsonObject root) {
            if (root != null && root.has("removeVanillaOres")) {
                vanillaOresEnabled = !bool(root, "removeVanillaOres", !vanillaOresEnabled);
            } else {
                vanillaOresEnabled = bool(root, "vanillaOresEnabled", vanillaOresEnabled);
            }
            disabledNodeTypes = stringList(root, "disabledNodeTypes", disabledNodeTypes);
        }

        JsonObject write() {
            JsonObject root = new JsonObject();
            root.addProperty("vanillaOresEnabled", vanillaOresEnabled);
            JsonArray arr = new JsonArray();
            for (String s : disabledNodeTypes) {
                arr.add(s);
            }
            root.add("disabledNodeTypes", arr);
            return root;
        }
    }

    public static final class Debug {
        public boolean debugLogging = false;

        void read(JsonObject root) {
            debugLogging = bool(root, "debugLogging", debugLogging);
        }

        JsonObject write() {
            JsonObject root = new JsonObject();
            root.addProperty("debugLogging", debugLogging);
            return root;
        }
    }

    static JsonObject object(JsonObject root, String key) {
        if (root != null && root.has(key) && root.get(key).isJsonObject()) {
            return root.getAsJsonObject(key);
        }
        return null;
    }

    private static boolean bool(JsonObject root, String key, boolean fallback) {
        if (root != null && root.has(key) && root.get(key).isJsonPrimitive()) {
            try {
                return root.get(key).getAsBoolean();
            } catch (Exception ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    private static int intValue(JsonObject root, String key, int fallback) {
        if (root != null && root.has(key) && root.get(key).isJsonPrimitive()) {
            try {
                return root.get(key).getAsInt();
            } catch (Exception ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    private static double doubleValue(JsonObject root, String key, double fallback) {
        if (root != null && root.has(key) && root.get(key).isJsonPrimitive()) {
            try {
                return root.get(key).getAsDouble();
            } catch (Exception ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    private static List<String> stringList(JsonObject root, String key, List<String> fallback) {
        if (root != null && root.has(key) && root.get(key).isJsonArray()) {
            List<String> out = new ArrayList<>();
            for (JsonElement e : root.getAsJsonArray(key)) {
                if (e.isJsonPrimitive()) {
                    out.add(e.getAsString());
                }
            }
            return out;
        }
        return new ArrayList<>(fallback);
    }

    private static int clampInt(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double clampDouble(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
