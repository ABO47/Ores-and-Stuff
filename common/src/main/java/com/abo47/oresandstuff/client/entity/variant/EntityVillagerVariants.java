package com.abo47.oresandstuff.client.entity.variant;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.VillagerDataHolder;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerType;

import com.abo47.oresandstuff.client.ui.controls.SearchFilter;

public final class EntityVillagerVariants {
    private static final String DEFAULT_TYPE = "plains";
    private static final String DEFAULT_PROFESSION = "none";
    private static final List<EntityVariantCatalog.VariantFolder> TYPES = List.of(
            folder("plains", "Plains"),
            folder("desert", "Desert"),
            folder("jungle", "Jungle"),
            folder("savanna", "Savanna"),
            folder("snow", "Snow"),
            folder("swamp", "Swamp"),
            folder("taiga", "Taiga")
    );
    private static final List<EntityVariantCatalog.VariantEntry> PROFESSIONS = List.of(
            entry("none", "Unemployed villager"),
            entry("farmer", "Farmer villager"),
            entry("fisherman", "Fisherman villager"),
            entry("fletcher", "Fletcher villager"),
            entry("shepherd", "Shepherd villager"),
            entry("librarian", "Librarian villager"),
            entry("cartographer", "Cartographer villager"),
            entry("cleric", "Cleric villager"),
            entry("armorer", "Armorer villager"),
            entry("blacksmith", "Blacksmith villager"),
            entry("toolsmith", "Toolsmith villager"),
            entry("weaponsmith", "Weaponsmith villager"),
            entry("butcher", "Butcher villager"),
            entry("leatherworker", "Leatherworker villager"),
            entry("mason", "Mason villager"),
            entry("nitwit", "Nitwit villager")
    );
    private static final List<EntityVariantCatalog.VariantEntry> VARIANTS = buildVariants();

    private EntityVillagerVariants() {
    }

    public static boolean isVillager(String entityId) {
        String normalized = entityId == null ? "" : entityId.trim().toLowerCase(Locale.ROOT);
        return "minecraft:villager".equals(normalized) || "minecraft:zombie_villager".equals(normalized);
    }

    public static List<EntityVariantCatalog.VariantFolder> folders(String query) {
        String cleanQuery = SearchFilter.normalize(query);
        if (cleanQuery.isBlank()) {
            return TYPES;
        }
        return TYPES.stream()
                .filter(folder -> SearchFilter.matches(cleanQuery, folder.key(), folder.label()))
                .toList();
    }

    public static List<EntityVariantCatalog.VariantEntry> variants() {
        return VARIANTS;
    }

    public static List<EntityVariantCatalog.VariantEntry> variantsForFolder(String folderKey, String query) {
        String type = normalizeType(folderKey);
        String cleanQuery = SearchFilter.normalize(query);
        List<EntityVariantCatalog.VariantEntry> entries = new ArrayList<>();
        for (EntityVariantCatalog.VariantEntry profession : PROFESSIONS) {
            EntityVariantCatalog.VariantEntry entry = entry(type + "/" + profession.key(), typeLabel(type) + " " + profession.label().toLowerCase(Locale.ROOT));
            if (cleanQuery.isBlank()
                    || SearchFilter.matches(cleanQuery, entry.key(), entry.label())
                    || SearchFilter.matches(cleanQuery, profession.key(), profession.label())) {
                entries.add(entry);
            }
        }
        return List.copyOf(entries);
    }

    public static String normalizeVariantKey(String variantKey) {
        String key = variantKey == null ? "" : variantKey.trim().toLowerCase(Locale.ROOT);
        if (key.isBlank()) {
            return "";
        }
        String type = DEFAULT_TYPE;
        String profession = key;
        int separator = key.indexOf('/');
        if (separator >= 0) {
            type = key.substring(0, separator);
            profession = key.substring(separator + 1);
        } else if (isType(key)) {
            type = key;
            profession = DEFAULT_PROFESSION;
        }
        type = normalizeType(type);
        profession = normalizeProfession(profession);
        if (type.isBlank() || profession.isBlank()) {
            return "";
        }
        return type + "/" + profession;
    }

    public static String folderFor(String variantKey) {
        String key = normalizeVariantKey(variantKey);
        if (key.isBlank()) {
            return DEFAULT_TYPE;
        }
        int separator = key.indexOf('/');
        return separator < 0 ? DEFAULT_TYPE : key.substring(0, separator);
    }

    public static String defaultVariantForFolder(String folderKey) {
        return normalizeType(folderKey) + "/" + DEFAULT_PROFESSION;
    }

    public static String folderLabel(String folderKey) {
        return typeLabel(normalizeType(folderKey));
    }

    public static String labelFor(String variantKey) {
        String key = normalizeVariantKey(variantKey);
        if (key.isBlank()) {
            return "";
        }
        int separator = key.indexOf('/');
        String type = separator < 0 ? DEFAULT_TYPE : key.substring(0, separator);
        String profession = separator < 0 ? key : key.substring(separator + 1);
        return typeLabel(type) + " " + professionLabel(profession).toLowerCase(Locale.ROOT);
    }

    public static void apply(Entity entity, String variantKey) {
        String key = normalizeVariantKey(variantKey);
        if (!(entity instanceof VillagerDataHolder villager) || key.isBlank()) {
            return;
        }
        int separator = key.indexOf('/');
        String type = separator < 0 ? DEFAULT_TYPE : key.substring(0, separator);
        String profession = separator < 0 ? key : key.substring(separator + 1);
        villager.setVillagerData(villager.getVillagerData()
                .setType(villagerType(type))
                .setProfession(villagerProfession(profession))
                .setLevel(2));
    }

    private static List<EntityVariantCatalog.VariantEntry> buildVariants() {
        List<EntityVariantCatalog.VariantEntry> entries = new ArrayList<>();
        for (EntityVariantCatalog.VariantFolder type : TYPES) {
            entries.addAll(variantsForFolder(type.key(), ""));
        }
        return List.copyOf(entries);
    }

    private static String normalizeType(String key) {
        String value = key == null ? "" : key.trim().toLowerCase(Locale.ROOT);
        return isType(value) ? value : DEFAULT_TYPE;
    }

    private static boolean isType(String key) {
        for (EntityVariantCatalog.VariantFolder type : TYPES) {
            if (type.key().equals(key)) {
                return true;
            }
        }
        return false;
    }

    private static String normalizeProfession(String key) {
        String value = key == null ? "" : key.trim().toLowerCase(Locale.ROOT);
        for (EntityVariantCatalog.VariantEntry profession : PROFESSIONS) {
            if (profession.key().equals(value)) {
                return value;
            }
        }
        return "";
    }

    private static String typeLabel(String key) {
        for (EntityVariantCatalog.VariantFolder type : TYPES) {
            if (type.key().equals(key)) {
                return type.label();
            }
        }
        return "Plains";
    }

    private static String professionLabel(String key) {
        for (EntityVariantCatalog.VariantEntry profession : PROFESSIONS) {
            if (profession.key().equals(key)) {
                return profession.label();
            }
        }
        return "Unemployed villager";
    }

    private static VillagerType villagerType(String key) {
        return switch (key) {
            case "desert" -> VillagerType.DESERT;
            case "jungle" -> VillagerType.JUNGLE;
            case "savanna" -> VillagerType.SAVANNA;
            case "snow" -> VillagerType.SNOW;
            case "swamp" -> VillagerType.SWAMP;
            case "taiga" -> VillagerType.TAIGA;
            default -> VillagerType.PLAINS;
        };
    }

    private static VillagerProfession villagerProfession(String key) {
        return switch (key) {
            case "farmer" -> VillagerProfession.FARMER;
            case "fisherman" -> VillagerProfession.FISHERMAN;
            case "fletcher" -> VillagerProfession.FLETCHER;
            case "shepherd" -> VillagerProfession.SHEPHERD;
            case "librarian" -> VillagerProfession.LIBRARIAN;
            case "cartographer" -> VillagerProfession.CARTOGRAPHER;
            case "cleric" -> VillagerProfession.CLERIC;
            case "armorer" -> VillagerProfession.ARMORER;
            case "blacksmith", "weaponsmith" -> VillagerProfession.WEAPONSMITH;
            case "toolsmith" -> VillagerProfession.TOOLSMITH;
            case "butcher" -> VillagerProfession.BUTCHER;
            case "leatherworker" -> VillagerProfession.LEATHERWORKER;
            case "mason" -> VillagerProfession.MASON;
            case "nitwit" -> VillagerProfession.NITWIT;
            default -> VillagerProfession.NONE;
        };
    }

    private static EntityVariantCatalog.VariantFolder folder(String key, String label) {
        return new EntityVariantCatalog.VariantFolder(key, label);
    }

    private static EntityVariantCatalog.VariantEntry entry(String key, String label) {
        return new EntityVariantCatalog.VariantEntry(key, label);
    }
}