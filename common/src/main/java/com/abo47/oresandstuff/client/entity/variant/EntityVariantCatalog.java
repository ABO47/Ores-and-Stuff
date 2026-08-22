package com.abo47.oresandstuff.client.entity.variant;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.DyeColor;

import com.abo47.oresandstuff.client.ui.controls.SearchFilter;

public final class EntityVariantCatalog {
    private static final List<VariantEntry> CATS = List.of(
            entry("tabby", "Tabby cat"),
            entry("black", "Black cat"),
            entry("red", "Red cat"),
            entry("siamese", "Siamese cat"),
            entry("british_shorthair", "British shorthair cat"),
            entry("calico", "Calico cat"),
            entry("persian", "Persian cat"),
            entry("ragdoll", "Ragdoll cat"),
            entry("white", "White cat"),
            entry("jellie", "Jellie cat"),
            entry("all_black", "All black cat")
    );
    private static final List<VariantEntry> FROGS = List.of(
            entry("temperate", "Temperate frog"),
            entry("warm", "Warm frog"),
            entry("cold", "Cold frog")
    );
    private static final List<VariantEntry> AXOLOTLS = List.of(
            entry("lucy", "Lucy axolotl"),
            entry("wild", "Wild axolotl"),
            entry("gold", "Gold axolotl"),
            entry("cyan", "Cyan axolotl"),
            entry("blue", "Blue axolotl")
    );
    private static final List<VariantEntry> RABBITS = List.of(
            entry("brown", "Brown rabbit"),
            entry("white", "White rabbit"),
            entry("black", "Black rabbit"),
            entry("white_splotched", "White splotched rabbit"),
            entry("gold", "Gold rabbit"),
            entry("salt", "Salt rabbit"),
            entry("evil", "Killer bunny"),
            entry("toast", "Toast rabbit")
    );
    private static final List<VariantEntry> FOXES = List.of(
            entry("red", "Red fox"),
            entry("snow", "Snow fox")
    );
    private static final List<VariantEntry> PARROTS = List.of(
            entry("red_blue", "Red and blue parrot"),
            entry("blue", "Blue parrot"),
            entry("green", "Green parrot"),
            entry("yellow_blue", "Yellow and blue parrot"),
            entry("gray", "Gray parrot")
    );
    private static final List<VariantEntry> HORSES = horseVariants();
    private static final List<VariantEntry> LLAMAS = List.of(
            entry("creamy", "Creamy llama"),
            entry("white", "White llama"),
            entry("brown", "Brown llama"),
            entry("gray", "Gray llama")
    );
    private static final List<VariantEntry> MOOSHROOMS = List.of(
            entry("red", "Red mooshroom"),
            entry("brown", "Brown mooshroom")
    );
    private static final List<VariantEntry> PANDAS = List.of(
            entry("normal", "Normal panda"),
            entry("lazy", "Lazy panda"),
            entry("worried", "Worried panda"),
            entry("playful", "Playful panda"),
            entry("brown", "Brown panda"),
            entry("weak", "Weak panda"),
            entry("aggressive", "Aggressive panda")
    );
    private static final List<VariantEntry> SHEEP = sheepVariants();
    private static final List<VariantEntry> TROPICAL_FISH = List.of(
            fish("stripey", DyeColor.ORANGE, DyeColor.GRAY),
            fish("flopper", DyeColor.GRAY, DyeColor.GRAY),
            fish("flopper", DyeColor.GRAY, DyeColor.BLUE),
            fish("clayfish", DyeColor.WHITE, DyeColor.GRAY),
            fish("sunstreak", DyeColor.BLUE, DyeColor.GRAY),
            fish("kob", DyeColor.ORANGE, DyeColor.WHITE),
            fish("spotty", DyeColor.PINK, DyeColor.LIGHT_BLUE),
            fish("blockfish", DyeColor.PURPLE, DyeColor.YELLOW),
            fish("clayfish", DyeColor.WHITE, DyeColor.RED),
            fish("spotty", DyeColor.WHITE, DyeColor.YELLOW),
            fish("glitter", DyeColor.WHITE, DyeColor.GRAY),
            fish("clayfish", DyeColor.WHITE, DyeColor.ORANGE),
            fish("dasher", DyeColor.CYAN, DyeColor.PINK),
            fish("brinely", DyeColor.LIME, DyeColor.LIGHT_BLUE),
            fish("betty", DyeColor.RED, DyeColor.WHITE),
            fish("snooper", DyeColor.GRAY, DyeColor.RED),
            fish("blockfish", DyeColor.RED, DyeColor.WHITE),
            fish("flopper", DyeColor.WHITE, DyeColor.YELLOW),
            fish("kob", DyeColor.RED, DyeColor.WHITE),
            fish("sunstreak", DyeColor.GRAY, DyeColor.WHITE),
            fish("dasher", DyeColor.CYAN, DyeColor.YELLOW),
            fish("flopper", DyeColor.YELLOW, DyeColor.YELLOW)
    );
    private static final List<VariantEntry> SLIME_SIZES = List.of(
            entry("small", "Small"),
            entry("medium", "Medium"),
            entry("large", "Large"),
            entry("huge", "Huge")
    );
    private static final List<VariantEntry> CREEPERS = List.of(
            entry("normal", "Normal creeper"),
            entry("charged", "Charged creeper")
    );
    private static final List<VariantEntry> GOATS = List.of(
            entry("normal", "Normal goat"),
            entry("screaming", "Screaming goat"),
            entry("left_horn", "Left horn goat"),
            entry("right_horn", "Right horn goat"),
            entry("no_horns", "No horn goat"),
            entry("screaming_left_horn", "Screaming left horn goat"),
            entry("screaming_right_horn", "Screaming right horn goat"),
            entry("screaming_no_horns", "Screaming no horn goat")
    );

    private EntityVariantCatalog() {
    }

    public static boolean hasVariants(String entityId) {
        return !variantsFor(entityId).isEmpty();
    }

    public static List<VariantEntry> variantsFor(String entityId) {
        return switch (normalizeEntityId(entityId)) {
            case "minecraft:villager", "minecraft:zombie_villager" -> EntityVillagerVariants.variants();
            case "minecraft:cat" -> CATS;
            case "minecraft:frog" -> FROGS;
            case "minecraft:axolotl" -> AXOLOTLS;
            case "minecraft:rabbit" -> RABBITS;
            case "minecraft:fox" -> FOXES;
            case "minecraft:parrot" -> PARROTS;
            case "minecraft:horse" -> HORSES;
            case "minecraft:llama", "minecraft:trader_llama" -> LLAMAS;
            case "minecraft:mooshroom" -> MOOSHROOMS;
            case "minecraft:panda" -> PANDAS;
            case "minecraft:sheep" -> SHEEP;
            case "minecraft:tropical_fish" -> TROPICAL_FISH;
            case "minecraft:slime", "minecraft:magma_cube" -> SLIME_SIZES;
            case "minecraft:creeper" -> CREEPERS;
            case "minecraft:goat" -> GOATS;
            default -> List.of();
        };
    }

    public static List<VariantEntry> search(String entityId, String query) {
        String cleanQuery = SearchFilter.normalize(query);
        if (cleanQuery.isBlank()) {
            return variantsFor(entityId);
        }
        return variantsFor(entityId).stream()
                .filter(entry -> SearchFilter.matches(cleanQuery, entry.key(), entry.label()))
                .toList();
    }

    public static boolean hasVariantFolders(String entityId) {
        return EntityVillagerVariants.isVillager(normalizeEntityId(entityId));
    }

    public static List<VariantFolder> variantFoldersFor(String entityId, String query) {
        if (!hasVariantFolders(entityId)) {
            return List.of();
        }
        return EntityVillagerVariants.folders(query);
    }

    public static List<VariantEntry> variantsForFolder(String entityId, String folderKey, String query) {
        if (!hasVariantFolders(entityId)) {
            return search(entityId, query);
        }
        return EntityVillagerVariants.variantsForFolder(folderKey, query);
    }

    public static String variantFolderFor(String entityId, String variantKey) {
        if (!hasVariantFolders(entityId)) {
            return "";
        }
        return EntityVillagerVariants.folderFor(variantKey);
    }

    public static String defaultVariantForFolder(String entityId, String folderKey) {
        if (!hasVariantFolders(entityId)) {
            return "";
        }
        return EntityVillagerVariants.defaultVariantForFolder(folderKey);
    }

    public static String variantFolderLabel(String entityId, String folderKey) {
        if (!hasVariantFolders(entityId)) {
            return "";
        }
        return EntityVillagerVariants.folderLabel(folderKey);
    }

    public static String normalizeVariantKey(String entityId, String variantKey) {
        String key = variantKey == null ? "" : variantKey.trim().toLowerCase(Locale.ROOT);
        if (key.isBlank()) {
            return "";
        }
        if (EntityVillagerVariants.isVillager(normalizeEntityId(entityId))) {
            return EntityVillagerVariants.normalizeVariantKey(key);
        }
        for (VariantEntry entry : variantsFor(entityId)) {
            if (entry.key().equals(key)) {
                return key;
            }
        }
        return "";
    }

    public static String labelFor(String entityId, String variantKey) {
        if (EntityVillagerVariants.isVillager(normalizeEntityId(entityId))) {
            return EntityVillagerVariants.labelFor(variantKey);
        }
        String key = normalizeVariantKey(entityId, variantKey);
        for (VariantEntry entry : variantsFor(entityId)) {
            if (entry.key().equals(key)) {
                return entry.label();
            }
        }
        return key;
    }

    public static void apply(Entity entity, String variantKey) {
        if (entity == null || variantKey == null || variantKey.isBlank()) {
            return;
        }
        String entityId = normalizeEntityId(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString());
        String key = normalizeVariantKey(entityId, variantKey);
        if (key.isBlank()) {
            return;
        }
        switch (entityId) {
            case "minecraft:villager", "minecraft:zombie_villager" -> EntityVillagerVariants.apply(entity, key);
            case "minecraft:cat" -> EntityVariantApplier.applyCat(entity, key);
            case "minecraft:frog" -> EntityVariantApplier.applyFrog(entity, key);
            case "minecraft:axolotl" -> EntityVariantApplier.applyAxolotl(entity, key);
            case "minecraft:rabbit" -> EntityVariantApplier.applyRabbit(entity, key);
            case "minecraft:fox" -> EntityVariantApplier.applyFox(entity, key);
            case "minecraft:parrot" -> EntityVariantApplier.applyParrot(entity, key);
            case "minecraft:horse" -> EntityVariantApplier.applyHorse(entity, key);
            case "minecraft:llama", "minecraft:trader_llama" -> EntityVariantApplier.applyLlama(entity, key);
            case "minecraft:mooshroom" -> EntityVariantApplier.applyMooshroom(entity, key);
            case "minecraft:panda" -> EntityVariantApplier.applyPanda(entity, key);
            case "minecraft:sheep" -> EntityVariantApplier.applySheep(entity, key);
            case "minecraft:tropical_fish" -> EntityVariantApplier.applyTropicalFish(entity, key);
            case "minecraft:slime", "minecraft:magma_cube" -> EntityVariantApplier.applySlime(entity, key);
            case "minecraft:creeper" -> EntityVariantApplier.applyCreeper(entity, key);
            case "minecraft:goat" -> EntityVariantApplier.applyGoat(entity, key);
            default -> {
            }
        }
    }

    private static List<VariantEntry> sheepVariants() {
        List<VariantEntry> entries = new ArrayList<>();
        for (DyeColor color : DyeColor.values()) {
            entries.add(entry(color.getName(), title(color.getName()) + " sheep"));
        }
        entries.add(entry("jeb", "Rainbow sheep"));
        return List.copyOf(entries);
    }

    private static List<VariantEntry> horseVariants() {
        List<VariantEntry> entries = new ArrayList<>();
        for (net.minecraft.world.entity.animal.horse.Variant variant : net.minecraft.world.entity.animal.horse.Variant.values()) {
            String color = variant.getSerializedName();
            entries.add(entry(color, title(color) + " horse"));
            entries.add(entry(color + ".white", title(color) + " horse with white markings"));
            entries.add(entry(color + ".white_field", title(color) + " horse with white field"));
            entries.add(entry(color + ".white_dots", title(color) + " horse with white dots"));
            entries.add(entry(color + ".black_dots", title(color) + " horse with black dots"));
        }
        return List.copyOf(entries);
    }

    private static VariantEntry fish(String pattern, DyeColor baseColor, DyeColor patternColor) {
        String key = pattern + "." + baseColor.getName() + "." + patternColor.getName();
        return entry(key, title(baseColor.getName()) + " " + title(patternColor.getName()) + " " + title(pattern));
    }

    private static VariantEntry entry(String key, String label) {
        return new VariantEntry(key, label);
    }

    private static String normalizeEntityId(String entityId) {
        String value = entityId == null ? "" : entityId.trim().toLowerCase(Locale.ROOT);
        if (value.isBlank() || value.contains(":")) {
            return value;
        }
        return "minecraft:" + value;
    }

    private static String title(String key) {
        String[] parts = key.split("_");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(part.substring(0, 1).toUpperCase(Locale.ROOT));
            if (part.length() > 1) {
                builder.append(part.substring(1));
            }
        }
        return builder.toString();
    }

    public record VariantEntry(String key, String label) {
    }

    public record VariantFolder(String key, String label) {
    }
}