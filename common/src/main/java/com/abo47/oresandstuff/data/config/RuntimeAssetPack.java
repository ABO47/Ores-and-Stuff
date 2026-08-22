package com.abo47.oresandstuff.data.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.stream.Stream;

import com.abo47.oresandstuff.OresAndStuffMod;
import com.abo47.oresandstuff.data.OreNodeDataManager;
import com.abo47.oresandstuff.node.OreNodeType;

public final class RuntimeAssetPack {
    private static final String PACK_FORMAT = "15";

    private RuntimeAssetPack() {
    }

    public static Path packFolder() {
        return ConfigAssets.folder("runtime_pack");
    }

    public static void regenerate() {
        Path packRoot = packFolder();
        try {
            deleteRecursively(packRoot);
            Files.createDirectories(packRoot);
            write(packRoot.resolve("pack.mcmeta"),
                    "{\"pack\":{\"pack_format\":" + PACK_FORMAT + ",\"description\":\"Ores and Stuff runtime assets\"}}");
        } catch (IOException e) {
            OresAndStuffMod.LOGGER.error("Failed to prepare runtime asset pack {}", packRoot, e);
            return;
        }

        Path minersFolder = ConfigAssets.folder("miners");
        for (MinerTierConfig.MinerTier tier : MinerTierConfig.tiers()) {
            generateMinerAssets(packRoot, minersFolder, tier);
        }

        OreNodeDataManager.INSTANCE.ensureLoaded();
        for (OreNodeType type : OreNodeDataManager.INSTANCE.nodeTypes()) {
            generateOreNodeAssets(packRoot, type);
        }
        OresAndStuffMod.LOGGER.info("Regenerated runtime asset pack with {} miner tier(s)", MinerTierConfig.tiers().size());
    }

    /**
     * Per-(type, tier) ore node blockstate/model. Each model parents the model
     * of the block configured via the tier's node_block (e.g.
     * minecraft:block/netherrack), so any game block can be used as the node
     * look and it renders exactly like that block.
     */
    private static void generateOreNodeAssets(Path packRoot, OreNodeType type) {
        for (int t = 0; t < type.qualityTiers().size(); t++) {
            OreNodeType.QualityTier tier = type.qualityTiers().get(t);
            String id = "ore_node_" + type.id().getPath() + "_t" + t;
            try {
                write(packRoot.resolve("assets/oresandstuff/blockstates/" + id + ".json"),
                        "{\"variants\":{\"\":{\"model\":\"oresandstuff:block/" + id + "\"}}}");
                write(packRoot.resolve("assets/oresandstuff/models/block/" + id + ".json"),
                        "{\"parent\":\"" + tier.nodeBlockModel() + "\"}");
            } catch (IOException e) {
                OresAndStuffMod.LOGGER.error("Failed to generate runtime assets for ore node block {}", id, e);
            }
        }
    }

    private static void generateMinerAssets(Path packRoot, Path minersFolder, MinerTierConfig.MinerTier tier) {
        String id = tier.id();
        if ("mk1".equals(id)) {
            return;
        }
        try {
            write(packRoot.resolve("assets/oresandstuff/blockstates/miner_" + id + ".json"),
                    "{\"variants\":{"
                            + "\"facing=north\":{\"model\":\"oresandstuff:block/miner_" + id + "\",\"y\":0},"
                            + "\"facing=east\":{\"model\":\"oresandstuff:block/miner_" + id + "\",\"y\":90},"
                            + "\"facing=south\":{\"model\":\"oresandstuff:block/miner_" + id + "\",\"y\":180},"
                            + "\"facing=west\":{\"model\":\"oresandstuff:block/miner_" + id + "\",\"y\":270}}}");

            Path tierDir = minersFolder.resolve(id);
            Path modelSrc = tierDir.resolve("models").resolve(tier.model() + ".json");
            if (Files.isRegularFile(modelSrc)) {
                Path target = packRoot.resolve("assets/oresandstuff/models/block/miner_" + id + ".json");
                Files.createDirectories(target.getParent());
                Files.copy(modelSrc, target, StandardCopyOption.REPLACE_EXISTING);
            } else {
                write(packRoot.resolve("assets/oresandstuff/models/block/miner_" + id + ".json"),
                        "{\"parent\":\"oresandstuff:block/miner_mk1\"}");
            }

            write(packRoot.resolve("assets/oresandstuff/models/item/miner_" + id + ".json"),
                    "{\"parent\":\"oresandstuff:block/miner_" + id + "\"}");

            copyTextures(packRoot, tierDir);
        } catch (IOException e) {
            OresAndStuffMod.LOGGER.error("Failed to generate runtime assets for miner tier {}", id, e);
        }
    }

    private static void copyTextures(Path packRoot, Path tierDir) throws IOException {
        Path texturesDir = tierDir.resolve("textures");
        if (!Files.isDirectory(texturesDir)) {
            return;
        }
        try (Stream<Path> files = Files.list(texturesDir)) {
            for (Path file : (Iterable<Path>) files.filter(p -> p.getFileName().toString().endsWith(".png"))::iterator) {
                Path target = packRoot.resolve("assets/oresandstuff/textures/block/" + file.getFileName().toString());
                Files.createDirectories(target.getParent());
                Files.copy(file, target, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    private static void write(Path file, String content) throws IOException {
        Files.createDirectories(file.getParent());
        Files.writeString(file, content, StandardCharsets.UTF_8);
    }

    private static void deleteRecursively(Path root) throws IOException {
        if (!Files.exists(root)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(root)) {
            for (Path p : (Iterable<Path>) paths.sorted((a, b) -> b.getNameCount() - a.getNameCount())::iterator) {
                Files.deleteIfExists(p);
            }
        }
    }
}