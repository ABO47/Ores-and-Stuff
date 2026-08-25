package com.abo47.oresandstuff.integration.jade;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.Identifiers;
import snownee.jade.api.TooltipPosition;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.theme.IThemeHelper;
import snownee.jade.api.ui.IElement;
import snownee.jade.api.ui.IElementHelper;

import com.abo47.oresandstuff.OresAndStuffMod;
import com.abo47.oresandstuff.data.OreNodeDataManager;
import com.abo47.oresandstuff.node.NodeVisuals;
import com.abo47.oresandstuff.node.OreNodeBlockEntity;
import com.abo47.oresandstuff.node.OreNodeType;
import com.abo47.oresandstuff.world.NodeLocatorService;

/**
 * Renames ore node blocks and their ore decorators (e.g. "Iron Ore" -> "Iron Node")
 * using the display name from the node type json, and swaps the tooltip icon to
 * the node's output item.
 */
public enum OreNodeComponentProvider implements IBlockComponentProvider {
    INSTANCE;

    private static final ResourceLocation UID = new ResourceLocation(OresAndStuffMod.MOD_ID, "ore_node");
    private static final int DECORATOR_SEARCH_RADIUS = 5;
    private static final long DECORATOR_CACHE_TICKS = 40;
    private static final int DECORATOR_CACHE_CAP = 512;

    private final Map<Long, DecoratorHit> decoratorCache = new ConcurrentHashMap<>();

    private record DecoratorHit(long time, ResourceLocation typeId) {
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }

    /**
     * Runs right after Jade's core object-name provider so its title line exists
     * and can be replaced by tag instead of duplicated.
     */
    @Override
    public int getDefaultPriority() {
        return TooltipPosition.HEAD - 50;
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        OreNodeType type = resolve(accessor);
        if (type == null) {
            return;
        }
        tooltip.remove(Identifiers.CORE_OBJECT_NAME);
        tooltip.add(0, IThemeHelper.get().title(type.displayName()));
    }

    @Override
    public IElement getIcon(BlockAccessor accessor, IPluginConfig config, IElement currentIcon) {
        OreNodeType type = resolve(accessor);
        if (type == null) {
            return currentIcon;
        }
        ItemStack icon = new ItemStack(BuiltInRegistries.ITEM.get(type.outputItem()));
        if (icon.isEmpty()) {
            return currentIcon;
        }
        return IElementHelper.get().item(icon);
    }

    private OreNodeType resolve(BlockAccessor accessor) {
        BlockEntity be = accessor.getBlockEntity();
        if (be instanceof OreNodeBlockEntity node) {
            return OreNodeDataManager.INSTANCE.getNodeType(node.getNodeTypeId()).orElse(null);
        }
        Block block = accessor.getBlockState().getBlock();
        if (!NodeVisuals.isVisualOre(block)) {
            return null;
        }
        ResourceLocation typeId = findDecoratorType(accessor);
        if (typeId == null || !NodeVisuals.isVisualBlock(typeId, block)) {
            return null;
        }
        return OreNodeDataManager.INSTANCE.getNodeType(typeId).orElse(null);
    }

    /**
     * Decorators are plain vanilla ore blocks, so the only way to tell them apart
     * from natural ore is a nearby node core. Results are cached briefly because
     * Jade queries every frame while targeting.
     */
    private ResourceLocation findDecoratorType(BlockAccessor accessor) {
        long pos = accessor.getPosition().asLong();
        long now = accessor.getLevel().getGameTime();
        DecoratorHit hit = decoratorCache.get(pos);
        if (hit != null && now - hit.time() < DECORATOR_CACHE_TICKS) {
            return hit.typeId();
        }
        OreNodeBlockEntity node = NodeLocatorService.findAnyNodeAround(accessor.getLevel(), accessor.getPosition(), DECORATOR_SEARCH_RADIUS);
        ResourceLocation typeId = node == null ? null : node.getNodeTypeId();
        if (decoratorCache.size() > DECORATOR_CACHE_CAP) {
            evictStale(now);
        }
        decoratorCache.put(pos, new DecoratorHit(now, typeId));
        return typeId;
    }

    private void evictStale(long now) {
        Iterator<DecoratorHit> it = decoratorCache.values().iterator();
        while (it.hasNext() && decoratorCache.size() > DECORATOR_CACHE_CAP / 2) {
            DecoratorHit hit = it.next();
            if (now - hit.time() >= DECORATOR_CACHE_TICKS) {
                it.remove();
            }
        }
        if (decoratorCache.size() > DECORATOR_CACHE_CAP) {
            decoratorCache.clear();
        }
    }
}
