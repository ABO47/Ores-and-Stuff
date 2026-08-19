package com.abo47.oresandstuff.client.theme.tokens;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

import com.abo47.oresandstuff.OresAndStuffMod;
import com.abo47.oresandstuff.data.config.ConfigAssets;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public final class OasColors {
    private OasColors() {
    }

    public static int SURFACE_BASE = 0xFF171C21;
    public static int SURFACE_PANEL = 0xFF202933;
    public static int SURFACE_PANEL_ALT = 0xFF2C3742;

    public static int BORDER_BASE = 0xFF546170;
    public static int BORDER_ACCENT = 0xFF65B7C8;

    public static int TEXT_PRIMARY = 0xFFEAF1F4;
    public static int TEXT_SECONDARY = 0xFFB8C7CE;
    public static int TEXT_MUTED = 0xFF88979F;

    public static int SUCCESS = 0xFF66D38D;
    public static int WARNING = 0xFFE5B44A;
    public static int ERROR = 0xFFE06F73;
    public static int INTERACTIVE = 0xFF64C3D2;
    public static int GLOW = 0xFF64C3D2;
    public static int SELECTION = 0xFF6BA8FF;
    public static int SCROLL_TRACK = BORDER_BASE;
    public static int SCROLL_THUMB = INTERACTIVE;

    public static int BLACK = 0xFF000000;
    public static int WHITE = 0xFFFFFFFF;
    public static int TRANSPARENT = 0x00000000;
    public static int DIM_OVERLAY = 0x60000000;

    public static int BG_0 = SURFACE_BASE;
    public static int BG_1 = SURFACE_PANEL;
    public static int BG_2 = SURFACE_PANEL_ALT;
    public static int BG_3 = 0xFF3F4953;
    public static int BORDER = BORDER_BASE;
    public static int BORDER_STRONG = BORDER_ACCENT;

    public static int ACCENT_PRIMARY = INTERACTIVE;
    public static int ACCENT_SOFT = 0xFF84CEDA;
    public static int ACCENT_MINT = SUCCESS;

    public static int VANILLA_BG = 0xFFC6C6C6;
    public static int VANILLA_PANEL = 0xFF8B8B8B;
    public static int VANILLA_PANEL_DARK = 0xFF747474;
    public static int VANILLA_BORDER_LIGHT = 0xFFEDEDED;
    public static int VANILLA_BORDER_DARK = 0xFF4A4A4A;
    public static int VANILLA_TEXT = 0xFF2D2D2D;

    private record ColorEntry(String key, int defaultValue, Consumer<Integer> setter) {
    }

    private static final Map<String, ColorEntry> REGISTRY = new LinkedHashMap<>();

    private static void register(String key, int defaultValue, Consumer<Integer> setter) {
        REGISTRY.put(key, new ColorEntry(key, defaultValue, setter));
    }

    static {
        register("SURFACE_BASE", SURFACE_BASE, v -> SURFACE_BASE = v);
        register("SURFACE_PANEL", SURFACE_PANEL, v -> SURFACE_PANEL = v);
        register("SURFACE_PANEL_ALT", SURFACE_PANEL_ALT, v -> SURFACE_PANEL_ALT = v);
        register("BORDER_BASE", BORDER_BASE, v -> BORDER_BASE = v);
        register("BORDER_ACCENT", BORDER_ACCENT, v -> BORDER_ACCENT = v);
        register("TEXT_PRIMARY", TEXT_PRIMARY, v -> TEXT_PRIMARY = v);
        register("TEXT_SECONDARY", TEXT_SECONDARY, v -> TEXT_SECONDARY = v);
        register("TEXT_MUTED", TEXT_MUTED, v -> TEXT_MUTED = v);
        register("SUCCESS", SUCCESS, v -> SUCCESS = v);
        register("WARNING", WARNING, v -> WARNING = v);
        register("ERROR", ERROR, v -> ERROR = v);
        register("INTERACTIVE", INTERACTIVE, v -> INTERACTIVE = v);
        register("GLOW", GLOW, v -> GLOW = v);
        register("SELECTION", SELECTION, v -> SELECTION = v);
        register("SCROLL_TRACK", SCROLL_TRACK, v -> SCROLL_TRACK = v);
        register("SCROLL_THUMB", SCROLL_THUMB, v -> SCROLL_THUMB = v);
        register("BLACK", BLACK, v -> BLACK = v);
        register("WHITE", WHITE, v -> WHITE = v);
        register("TRANSPARENT", TRANSPARENT, v -> TRANSPARENT = v);
        register("DIM_OVERLAY", DIM_OVERLAY, v -> DIM_OVERLAY = v);
        register("BG_0", BG_0, v -> BG_0 = v);
        register("BG_1", BG_1, v -> BG_1 = v);
        register("BG_2", BG_2, v -> BG_2 = v);
        register("BG_3", BG_3, v -> BG_3 = v);
        register("BORDER", BORDER, v -> BORDER = v);
        register("BORDER_STRONG", BORDER_STRONG, v -> BORDER_STRONG = v);
        register("ACCENT_PRIMARY", ACCENT_PRIMARY, v -> ACCENT_PRIMARY = v);
        register("ACCENT_SOFT", ACCENT_SOFT, v -> ACCENT_SOFT = v);
        register("ACCENT_MINT", ACCENT_MINT, v -> ACCENT_MINT = v);
        register("VANILLA_BG", VANILLA_BG, v -> VANILLA_BG = v);
        register("VANILLA_PANEL", VANILLA_PANEL, v -> VANILLA_PANEL = v);
        register("VANILLA_PANEL_DARK", VANILLA_PANEL_DARK, v -> VANILLA_PANEL_DARK = v);
        register("VANILLA_BORDER_LIGHT", VANILLA_BORDER_LIGHT, v -> VANILLA_BORDER_LIGHT = v);
        register("VANILLA_BORDER_DARK", VANILLA_BORDER_DARK, v -> VANILLA_BORDER_DARK = v);
        register("VANILLA_TEXT", VANILLA_TEXT, v -> VANILLA_TEXT = v);
    }

    /**
     * Writes the default {@code colors.json} into the mod config folder if it
     * does not exist yet, so players can override every UI color.
     */
    public static synchronized void ensureGenerated() {
        Path file = colorsFile();
        if (Files.isRegularFile(file)) {
            return;
        }
        JsonObject root = new JsonObject();
        for (ColorEntry e : REGISTRY.values()) {
            root.addProperty(e.key(), String.format("0x%08X", e.defaultValue()));
        }
        try {
            Path parent = file.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(file, ConfigAssets.pretty(root), StandardCharsets.UTF_8);
            OresAndStuffMod.LOGGER.info("Generated colors config {}", file);
        } catch (Exception e) {
            OresAndStuffMod.LOGGER.error("Failed to write colors config {}", file, e);
        }
    }

    /**
     * Reloads colors from the generated {@code colors.json}. Values that are
     * missing or malformed keep their current (default) value.
     */
    public static synchronized void reload() {
        ensureGenerated();
        try {
            Path file = colorsFile();
            JsonObject root = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
            for (ColorEntry e : REGISTRY.values()) {
                if (!root.has(e.key())) {
                    continue;
                }
                try {
                    e.setter().accept(parseColor(root.get(e.key()).getAsString(), e.defaultValue()));
                } catch (Exception ignored) {
                }
            }
        } catch (Exception e) {
            OresAndStuffMod.LOGGER.warn("Failed reading colors config, keeping defaults", e);
        }
    }

    private static int parseColor(String raw, int fallback) {
        if (raw == null) {
            return fallback;
        }
        String s = raw.trim();
        try {
            if (s.startsWith("0x") || s.startsWith("0X")) {
                return (int) Long.parseLong(s.substring(2), 16);
            }
            if (s.startsWith("#")) {
                return (int) Long.parseLong(s.substring(1), 16);
            }
            return (int) Long.parseLong(s);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static Path colorsFile() {
        return ConfigAssets.folder("").resolve("colors.json");
    }

    public static int elevatedSurface() {
        return mix(SURFACE_PANEL_ALT, TEXT_PRIMARY, 10);
    }

    public static int recessedSurface() {
        return mix(SURFACE_BASE, BLACK, 12);
    }

    public static int subtleBorder() {
        return mix(BORDER_BASE, SURFACE_BASE, 28);
    }

    public static int focusBorder() {
        return mix(BORDER_ACCENT, TEXT_PRIMARY, 10);
    }

    public static int hoverFill(int accent) {
        return withAlpha(accent, 46);
    }

    public static int pressedFill(int accent) {
        return withAlpha(accent, 76);
    }

    public static int scrollTrack(boolean active) {
        return withAlpha(SCROLL_TRACK, active ? 190 : 140);
    }

    public static int scrollThumb(boolean active) {
        return withAlpha(SCROLL_THUMB, active ? 255 : 220);
    }

    public static float rf(int argb) {
        return ((argb >> 16) & 0xFF) / 255f;
    }

    public static float gf(int argb) {
        return ((argb >> 8) & 0xFF) / 255f;
    }

    public static float bf(int argb) {
        return (argb & 0xFF) / 255f;
    }

    public static int withAlpha(int rgbOrArgb, int alpha) {
        return ((alpha & 0xFF) << 24) | (rgbOrArgb & 0x00FFFFFF);
    }

    private static int mix(int color, int other, int otherPercent) {
        int p = Math.max(0, Math.min(100, otherPercent));
        int inv = 100 - p;
        int a = (((color >>> 24) & 0xFF) * inv + ((other >>> 24) & 0xFF) * p) / 100;
        int r = (((color >>> 16) & 0xFF) * inv + ((other >>> 16) & 0xFF) * p) / 100;
        int g = (((color >>> 8) & 0xFF) * inv + ((other >>> 8) & 0xFF) * p) / 100;
        int b = ((color & 0xFF) * inv + (other & 0xFF) * p) / 100;
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}