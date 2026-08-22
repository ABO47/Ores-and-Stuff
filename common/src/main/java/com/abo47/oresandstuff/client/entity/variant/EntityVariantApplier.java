package com.abo47.oresandstuff.client.entity.variant;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.CatVariant;
import net.minecraft.world.entity.animal.Fox;
import net.minecraft.world.entity.animal.FrogVariant;
import net.minecraft.world.entity.animal.MushroomCow;
import net.minecraft.world.entity.animal.Panda;
import net.minecraft.world.entity.animal.Parrot;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.animal.TropicalFish;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.entity.animal.frog.Frog;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.entity.animal.horse.Llama;
import net.minecraft.world.entity.animal.horse.Markings;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.item.DyeColor;

final class EntityVariantApplier {
    private EntityVariantApplier() {
    }

    static void applyCat(Entity entity, String key) {
        if (entity instanceof Cat cat) {
            CatVariant variant = BuiltInRegistries.CAT_VARIANT.getOrThrow(switch (key) {
                case "tabby" -> CatVariant.TABBY;
                case "black" -> CatVariant.BLACK;
                case "red" -> CatVariant.RED;
                case "siamese" -> CatVariant.SIAMESE;
                case "british_shorthair" -> CatVariant.BRITISH_SHORTHAIR;
                case "calico" -> CatVariant.CALICO;
                case "persian" -> CatVariant.PERSIAN;
                case "ragdoll" -> CatVariant.RAGDOLL;
                case "white" -> CatVariant.WHITE;
                case "jellie" -> CatVariant.JELLIE;
                case "all_black" -> CatVariant.ALL_BLACK;
                default -> CatVariant.TABBY;
            });
            cat.setVariant(variant);
        }
    }

    static void applyFrog(Entity entity, String key) {
        if (entity instanceof Frog frog) {
            frog.setVariant(switch (key) {
                case "warm" -> FrogVariant.WARM;
                case "cold" -> FrogVariant.COLD;
                default -> FrogVariant.TEMPERATE;
            });
        }
    }

    static void applyAxolotl(Entity entity, String key) {
        if (entity instanceof Axolotl axolotl) {
            axolotl.setVariant(switch (key) {
                case "wild" -> Axolotl.Variant.WILD;
                case "gold" -> Axolotl.Variant.GOLD;
                case "cyan" -> Axolotl.Variant.CYAN;
                case "blue" -> Axolotl.Variant.BLUE;
                default -> Axolotl.Variant.LUCY;
            });
        }
    }

    static void applyRabbit(Entity entity, String key) {
        if (entity instanceof Rabbit rabbit) {
            if ("toast".equals(key)) {
                rabbit.setVariant(Rabbit.Variant.BROWN);
                rabbit.setCustomName(Component.literal("Toast"));
                return;
            }
            rabbit.setCustomName(null);
            rabbit.setVariant(switch (key) {
                case "white" -> Rabbit.Variant.WHITE;
                case "black" -> Rabbit.Variant.BLACK;
                case "white_splotched" -> Rabbit.Variant.WHITE_SPLOTCHED;
                case "gold" -> Rabbit.Variant.GOLD;
                case "salt" -> Rabbit.Variant.SALT;
                case "evil" -> Rabbit.Variant.EVIL;
                default -> Rabbit.Variant.BROWN;
            });
        }
    }

    static void applyFox(Entity entity, String key) {
        if (entity instanceof Fox fox) {
            fox.setVariant("snow".equals(key) ? Fox.Type.SNOW : Fox.Type.RED);
        }
    }

    static void applyParrot(Entity entity, String key) {
        if (entity instanceof Parrot parrot) {
            parrot.setVariant(switch (key) {
                case "blue" -> Parrot.Variant.BLUE;
                case "green" -> Parrot.Variant.GREEN;
                case "yellow_blue" -> Parrot.Variant.YELLOW_BLUE;
                case "gray" -> Parrot.Variant.GRAY;
                default -> Parrot.Variant.RED_BLUE;
            });
        }
    }

    static void applyHorse(Entity entity, String key) {
        if (entity instanceof Horse horse) {
            String[] parts = key.split("\\.", 2);
            net.minecraft.world.entity.animal.horse.Variant variant = horseVariant(parts[0]);
            Markings markings = parts.length > 1 ? horseMarkings(parts[1]) : Markings.NONE;
            CompoundTag tag = new CompoundTag();
            horse.addAdditionalSaveData(tag);
            tag.putInt("Variant", variant.getId() & 255 | markings.getId() << 8 & 0xFF00);
            horse.readAdditionalSaveData(tag);
        }
    }

    static void applyLlama(Entity entity, String key) {
        if (entity instanceof Llama llama) {
            llama.setVariant(switch (key) {
                case "white" -> Llama.Variant.WHITE;
                case "brown" -> Llama.Variant.BROWN;
                case "gray" -> Llama.Variant.GRAY;
                default -> Llama.Variant.CREAMY;
            });
        }
    }

    static void applyMooshroom(Entity entity, String key) {
        if (entity instanceof MushroomCow cow) {
            cow.setVariant("brown".equals(key) ? MushroomCow.MushroomType.BROWN : MushroomCow.MushroomType.RED);
        }
    }

    static void applyPanda(Entity entity, String key) {
        if (entity instanceof Panda panda) {
            Panda.Gene gene = switch (key) {
                case "lazy" -> Panda.Gene.LAZY;
                case "worried" -> Panda.Gene.WORRIED;
                case "playful" -> Panda.Gene.PLAYFUL;
                case "brown" -> Panda.Gene.BROWN;
                case "weak" -> Panda.Gene.WEAK;
                case "aggressive" -> Panda.Gene.AGGRESSIVE;
                default -> Panda.Gene.NORMAL;
            };
            panda.setMainGene(gene);
            panda.setHiddenGene(gene);
            panda.setAttributes();
        }
    }

    static void applySheep(Entity entity, String key) {
        if (entity instanceof Sheep sheep) {
            sheep.setSheared(false);
            if ("jeb".equals(key)) {
                sheep.setColor(DyeColor.WHITE);
                sheep.setCustomName(Component.literal("jeb_"));
                return;
            }
            sheep.setCustomName(null);
            sheep.setColor(dyeColor(key, DyeColor.WHITE));
        }
    }

    static void applyTropicalFish(Entity entity, String key) {
        if (entity instanceof TropicalFish fish) {
            String[] parts = key.split("\\.");
            if (parts.length != 3) {
                return;
            }
            TropicalFish.Pattern pattern = tropicalFishPattern(parts[0]);
            TropicalFish.Variant variant = new TropicalFish.Variant(pattern, dyeColor(parts[1], DyeColor.WHITE), dyeColor(parts[2], DyeColor.WHITE));
            CompoundTag tag = new CompoundTag();
            fish.addAdditionalSaveData(tag);
            tag.putInt("Variant", variant.getPackedId());
            fish.readAdditionalSaveData(tag);
        }
    }

    static void applySlime(Entity entity, String key) {
        if (entity instanceof Slime slime) {
            slime.setSize(switch (key) {
                case "medium" -> 2;
                case "large" -> 4;
                case "huge" -> 8;
                default -> 1;
            }, true);
        }
    }

    static void applyCreeper(Entity entity, String key) {
        if (entity instanceof Creeper creeper) {
            CompoundTag tag = new CompoundTag();
            creeper.addAdditionalSaveData(tag);
            tag.putBoolean("powered", "charged".equals(key));
            tag.putBoolean("ignited", false);
            creeper.readAdditionalSaveData(tag);
        }
    }

    static void applyGoat(Entity entity, String key) {
        if (entity instanceof Goat goat) {
            boolean screaming = key.startsWith("screaming");
            boolean leftHorn = !key.contains("right_horn") && !key.contains("no_horns");
            boolean rightHorn = !key.contains("left_horn") && !key.contains("no_horns");
            CompoundTag tag = new CompoundTag();
            goat.addAdditionalSaveData(tag);
            tag.putBoolean("IsScreamingGoat", screaming);
            tag.putBoolean("HasLeftHorn", leftHorn);
            tag.putBoolean("HasRightHorn", rightHorn);
            goat.readAdditionalSaveData(tag);
        }
    }

    private static net.minecraft.world.entity.animal.horse.Variant horseVariant(String key) {
        return switch (key) {
            case "creamy" -> net.minecraft.world.entity.animal.horse.Variant.CREAMY;
            case "chestnut" -> net.minecraft.world.entity.animal.horse.Variant.CHESTNUT;
            case "brown" -> net.minecraft.world.entity.animal.horse.Variant.BROWN;
            case "black" -> net.minecraft.world.entity.animal.horse.Variant.BLACK;
            case "gray" -> net.minecraft.world.entity.animal.horse.Variant.GRAY;
            case "dark_brown" -> net.minecraft.world.entity.animal.horse.Variant.DARK_BROWN;
            default -> net.minecraft.world.entity.animal.horse.Variant.WHITE;
        };
    }

    private static Markings horseMarkings(String key) {
        return switch (key) {
            case "white" -> Markings.WHITE;
            case "white_field" -> Markings.WHITE_FIELD;
            case "white_dots" -> Markings.WHITE_DOTS;
            case "black_dots" -> Markings.BLACK_DOTS;
            default -> Markings.NONE;
        };
    }

    private static TropicalFish.Pattern tropicalFishPattern(String key) {
        for (TropicalFish.Pattern pattern : TropicalFish.Pattern.values()) {
            if (pattern.getSerializedName().equals(key)) {
                return pattern;
            }
        }
        return TropicalFish.Pattern.KOB;
    }

    private static DyeColor dyeColor(String key, DyeColor fallback) {
        return DyeColor.byName(key, fallback);
    }
}
