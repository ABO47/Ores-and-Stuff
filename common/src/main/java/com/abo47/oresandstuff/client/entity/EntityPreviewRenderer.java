package com.abo47.oresandstuff.client.entity;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.lwjgl.opengl.GL11;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;

import com.abo47.oresandstuff.client.entity.variant.EntityVariantCatalog;
import com.abo47.oresandstuff.client.ui.controls.SearchFilter;

public final class EntityPreviewRenderer {
    public static final String ENTITY_ASSET_PREFIX = "entity:";
    private static final Map<String, Entity> ENTITY_CACHE = new HashMap<>();
    private static float animationPartialTicks;
    public static final int FRONT_ENTITY_YAW = 205;
    private static final int DEFAULT_ICON_ENTITY_YAW = FRONT_ENTITY_YAW;
    private static final int DEFAULT_ICON_ENTITY_SPIN_SPEED = 0;
    private static final double ICON_ENTITY_FILL = 0.82D;
    private static final double ICON_ENTITY_MAX_SCALE = 96.0D;
    private static final double TILE_ENTITY_FILL = 0.86D;
    private static final double TILE_ENTITY_MAX_SCALE = 112.0D;
    private static final double CANVAS_ENTITY_FILL = 0.94D;
    private static final double CANVAS_ENTITY_MAX_SCALE = 2048.0D;
    private static final Map<Class<?>, Method> IMMUNE_SETTER_CACHE = new HashMap<>();
    private static final Set<Class<?>> IMMUNE_NO_SETTER = new HashSet<>();

    private EntityPreviewRenderer() {
    }

    public static void prewarmEntityCache() {
        if (Minecraft.getInstance().level == null) {
            return;
        }
        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            if (type == null) {
                continue;
            }
            ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
            if (id == null) {
                continue;
            }
            String entityId = id.toString();
            if (!ENTITY_CACHE.containsKey(entityId)) {
                Entity entity = type.create(Minecraft.getInstance().level);
                if (entity != null) {
                    ENTITY_CACHE.put(entityId, entity);
                }
            }
        }
    }

    public static boolean isEntityAsset(String asset) {
        return !entityId(asset).isBlank();
    }

    public static String entityAsset(String entityId) {
        String normalized = normalizeEntityId(entityId);
        return normalized.isBlank() ? "" : ENTITY_ASSET_PREFIX + normalized;
    }

    public static String entityAsset(String entityId, String variantKey) {
        String normalizedEntity = normalizeEntityId(entityId);
        String normalizedVariant = normalizeVariantKey(variantKey);
        if (normalizedEntity.isBlank()) {
            return "";
        }
        return normalizedVariant.isBlank()
                ? ENTITY_ASSET_PREFIX + normalizedEntity
                : ENTITY_ASSET_PREFIX + normalizedEntity + "?variant=" + normalizedVariant;
    }

    public static String withEntityVariant(String asset, String variantKey) {
        String entityId = entityId(asset);
        return entityAsset(entityId, variantKey, entityYaw(asset), entitySpinSpeed(asset));
    }

    public static String withEntityMotion(String asset, int yawDegrees, int spinSpeed) {
        String entityId = entityId(asset);
        return entityAsset(entityId, entityVariant(asset), yawDegrees, spinSpeed);
    }

    public static String entityId(String asset) {
        String value = asset == null ? "" : asset.trim();
        if (!value.startsWith(ENTITY_ASSET_PREFIX)) {
            return "";
        }
        String body = value.substring(ENTITY_ASSET_PREFIX.length());
        int queryIndex = body.indexOf('?');
        return normalizeEntityId(queryIndex >= 0 ? body.substring(0, queryIndex) : body);
    }

    public static String entityVariant(String asset) {
        return entityQueryParam(asset, "variant");
    }

    public static int entityYaw(String asset) {
        return parseIntParam(entityQueryParam(asset, "yaw"), DEFAULT_ICON_ENTITY_YAW, 0, 359);
    }

    public static int entitySpinSpeed(String asset) {
        return parseIntParam(entityQueryParam(asset, "spin"), DEFAULT_ICON_ENTITY_SPIN_SPEED, SpinMath.MIN_ENTITY_SPIN_SPEED, SpinMath.MAX_ENTITY_SPIN_SPEED);
    }

    private static String entityQueryParam(String asset, String key) {
        String value = asset == null ? "" : asset.trim();
        if (!value.startsWith(ENTITY_ASSET_PREFIX)) {
            return "";
        }
        String body = value.substring(ENTITY_ASSET_PREFIX.length());
        int queryIndex = body.indexOf('?');
        if (queryIndex < 0 || queryIndex >= body.length() - 1) {
            return "";
        }
        String[] params = body.substring(queryIndex + 1).split("&");
        for (String param : params) {
            int equals = param.indexOf('=');
            if (equals > 0 && key.equals(param.substring(0, equals))) {
                return param.substring(equals + 1);
            }
        }
        return "";
    }

    public static String entityIdFromSpawnEgg(String itemId) {
        String clean = itemId == null ? "" : itemId.trim();
        if (clean.isBlank() || clean.startsWith("#")) {
            return "";
        }
        ResourceLocation id = ResourceLocation.tryParse(clean);
        if (id == null) {
            return "";
        }
        Item item = BuiltInRegistries.ITEM.get(id);
        if (!(item instanceof SpawnEggItem egg) || item == Items.AIR) {
            return "";
        }
        EntityType<?> type = egg.getType(new ItemStack(item).getTag());
        ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        return entityId == null ? "" : entityId.toString();
    }

    public static String spawnEggIcon(String entityId) {
        EntityType<?> type = entityType(entityId);
        if (type == null) {
            return "minecraft:egg";
        }
        SpawnEggItem egg = SpawnEggItem.byId(type);
        if (egg == null) {
            return "minecraft:egg";
        }
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(egg);
        return itemId == null ? "minecraft:egg" : itemId.toString();
    }

    public static String entityDisplayName(String entityId) {
        EntityType<?> type = entityType(entityId);
        if (type == null) {
            return entityId == null ? "" : entityId;
        }
        return type.getDescription().getString();
    }

    public static List<String> searchableSpawnEggEntries(String filter) {
        String rawQuery = SearchFilter.normalizeUserInput(filter);
        List<String> entries = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            if (!(item instanceof SpawnEggItem egg)) {
                continue;
            }
            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item);
            if (itemId == null) {
                continue;
            }
            EntityType<?> type = egg.getType(new ItemStack(item).getTag());
            ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(type);
            String itemKey = itemId.toString();
            String entityKey = entityId == null ? "" : entityId.toString();
            String itemName = item.getDescription().getString();
            String entityName = type.getDescription().getString();
            if (rawQuery.isBlank()
                    || SearchFilter.matches(rawQuery, itemKey, itemName)
                    || SearchFilter.matches(rawQuery, entityKey, entityName)) {
                entries.add(itemKey);
            }
        }
        entries.sort(String::compareTo);
        return entries;
    }

    public static boolean renderEntity(GuiGraphics graphics, int x, int y, int width, int height, String entityId, float partialTicks) {
        return renderEntity(graphics, x, y, width, height, entityId, DEFAULT_ICON_ENTITY_YAW, DEFAULT_ICON_ENTITY_SPIN_SPEED, partialTicks);
    }

    public static boolean renderEntity(GuiGraphics graphics, int x, int y, int width, int height, String entityId, int yawDegrees, int spinSpeed, float partialTicks) {
        return renderEntityAsset(graphics, x, y, width, height, entityAsset(entityId), yawDegrees, spinSpeed, partialTicks);
    }

    public static boolean renderEntityAsset(GuiGraphics graphics, int x, int y, int width, int height, String asset, int yawDegrees, int spinSpeed, float partialTicks) {
        return renderEntityAsset(graphics, x, y, width, height, asset, yawDegrees, spinSpeed, false, partialTicks);
    }

    public static boolean renderEntityAsset(GuiGraphics graphics, int x, int y, int width, int height, String asset, int yawDegrees, int spinSpeed, boolean darkened, float partialTicks) {
        return renderEntityAssetAtCenter(graphics, x + width / 2, y + height / 2, width, height, asset, yawDegrees, spinSpeed, SpinMath.DEFAULT_MODEL_PITCH, darkened, partialTicks);
    }

    public static boolean renderEntityAssetAtCenter(GuiGraphics graphics, int centerX, int centerY, int width, int height, String asset, int yawDegrees, int spinSpeed, int pitchDegrees, float partialTicks) {
        return renderEntityAssetAtCenter(graphics, centerX, centerY, width, height, asset, yawDegrees, spinSpeed, pitchDegrees, partialTicks, ICON_ENTITY_FILL, ICON_ENTITY_MAX_SCALE);
    }

    public static boolean renderEntityAssetAtCenter(GuiGraphics graphics, int centerX, int centerY, int width, int height, String asset, int yawDegrees, int spinSpeed, float partialTicks) {
        return renderEntityAssetAtCenter(graphics, centerX, centerY, width, height, asset, yawDegrees, spinSpeed, SpinMath.DEFAULT_MODEL_PITCH, partialTicks, ICON_ENTITY_FILL, ICON_ENTITY_MAX_SCALE);
    }

    public static boolean renderEntityAssetAtCenter(GuiGraphics graphics, int centerX, int centerY, int width, int height, String asset, int yawDegrees, int spinSpeed, int pitchDegrees, boolean darkened, float partialTicks) {
        return renderEntityAssetAtCenter(graphics, centerX, centerY, width, height, asset, yawDegrees, spinSpeed, pitchDegrees, darkened, partialTicks, ICON_ENTITY_FILL, ICON_ENTITY_MAX_SCALE);
    }

    public static boolean renderCanvasEntityAssetAtCenter(GuiGraphics graphics, int centerX, int centerY, int width, int height, String asset, int yawDegrees, int spinSpeed, int pitchDegrees, float partialTicks) {
        return renderEntityAssetAtCenter(graphics, centerX, centerY, width, height, asset, yawDegrees, spinSpeed, pitchDegrees, partialTicks, CANVAS_ENTITY_FILL, CANVAS_ENTITY_MAX_SCALE);
    }

    public static boolean renderTileEntityAsset(GuiGraphics graphics, int x, int y, int width, int height, String asset, int yawDegrees, int spinSpeed, int pitchDegrees, float partialTicks) {
        return renderEntityAssetAtCenter(graphics, x + width / 2, y + height / 2, width, height, asset, yawDegrees, spinSpeed, pitchDegrees, partialTicks, TILE_ENTITY_FILL, TILE_ENTITY_MAX_SCALE);
    }

    public static boolean renderTileEntityAsset(GuiGraphics graphics, int x, int y, int width, int height, String asset, int yawDegrees, int spinSpeed, int pitchDegrees, boolean darkened, float partialTicks) {
        return renderEntityAssetAtCenter(graphics, x + width / 2, y + height / 2, width, height, asset, yawDegrees, spinSpeed, pitchDegrees, darkened, partialTicks, TILE_ENTITY_FILL, TILE_ENTITY_MAX_SCALE);
    }

    private static boolean renderEntityAssetAtCenter(GuiGraphics graphics, int centerX, int centerY, int width, int height, String asset, int yawDegrees, int spinSpeed, int pitchDegrees, float partialTicks, double fill, double maxScale) {
        return renderEntityAssetAtCenter(graphics, centerX, centerY, width, height, asset, yawDegrees, spinSpeed, pitchDegrees, false, partialTicks, fill, maxScale);
    }

    private static boolean renderEntityAssetAtCenter(GuiGraphics graphics, int centerX, int centerY, int width, int height, String asset, int yawDegrees, int spinSpeed, int pitchDegrees, boolean darkened, float partialTicks, double fill, double maxScale) {
        EntityAsset parsed = parseEntityAsset(asset);
        Entity entity = cachedEntity(parsed);
        if (entity == null || width <= 0 || height <= 0) {
            return false;
        }
        EntityVariantCatalog.apply(entity, parsed.variantKey());
        int speed = SpinMath.clampEntitySpinSpeed(spinSpeed);
        float yaw = currentYaw(yawDegrees, speed);
        float pitch = SpinMath.normalizeDegrees(pitchDegrees);
        prepareEntityForRender(entity, speed > 0);
        double scale = renderScale(entity, width, height, fill, maxScale);
        float animPartialTicks = speed > 0 ? animationPartialTicks : 0.0F;
        renderEntityInInventory(graphics, centerX, centerY, scale, entity, yaw, pitch, darkened, animPartialTicks);
        return true;
    }

    private static double renderScale(Entity entity, int width, int height, double fill, double maxScale) {
        double entityW = Math.max(0.25D, entity.getBbWidth());
        double entityH = Math.max(0.25D, entity.getBbHeight());
        double scale = Math.min(width / entityW, height / entityH) * fill;
        return Math.max(1.0D, Math.min(maxScale, scale));
    }

    private static Entity cachedEntity(EntityAsset asset) {
        if (asset.entityId().isBlank() || Minecraft.getInstance().level == null) {
            return null;
        }
        EntityType<?> type = entityType(asset.entityId());
        if (type == null) {
            return null;
        }
        Entity entity = ENTITY_CACHE.get(asset.cacheKey());
        if (entity == null || entity.getType() != type || entity.level() != Minecraft.getInstance().level) {
            entity = type.create(Minecraft.getInstance().level);
            if (entity == null) {
                return null;
            }
            ENTITY_CACHE.put(asset.cacheKey(), entity);
        }
        return entity;
    }

    private static EntityType<?> entityType(String entityId) {
        ResourceLocation id = ResourceLocation.tryParse(normalizeEntityId(entityId));
        if (id == null) {
            return null;
        }
        return BuiltInRegistries.ENTITY_TYPE.getOptional(id).orElse(null);
    }

    private static String normalizeEntityId(String entityId) {
        return entityId == null ? "" : entityId.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizeVariantKey(String variantKey) {
        return variantKey == null ? "" : variantKey.trim().toLowerCase(Locale.ROOT);
    }

    private static String entityAsset(String entityId, String variantKey, int yawDegrees, int spinSpeed) {
        String normalizedEntity = normalizeEntityId(entityId);
        if (normalizedEntity.isBlank()) {
            return "";
        }
        String normalizedVariant = EntityVariantCatalog.normalizeVariantKey(normalizedEntity, variantKey);
        int yaw = SpinMath.normalizeDegrees(yawDegrees);
        int spin = SpinMath.clampEntitySpinSpeed(spinSpeed);
        List<String> params = new ArrayList<>();
        if (!normalizedVariant.isBlank()) {
            params.add("variant=" + normalizedVariant);
        }
        if (yaw != DEFAULT_ICON_ENTITY_YAW) {
            params.add("yaw=" + yaw);
        }
        if (spin != DEFAULT_ICON_ENTITY_SPIN_SPEED) {
            params.add("spin=" + spin);
        }
        return params.isEmpty() ? ENTITY_ASSET_PREFIX + normalizedEntity : ENTITY_ASSET_PREFIX + normalizedEntity + "?" + String.join("&", params);
    }

    private static EntityAsset parseEntityAsset(String asset) {
        String entityId = entityId(asset);
        String variantKey = EntityVariantCatalog.normalizeVariantKey(entityId, entityVariant(asset));
        return new EntityAsset(entityId, variantKey);
    }

    private static float currentYaw(int yawDegrees, int spinSpeed) {
        float base = SpinMath.normalizeDegrees(yawDegrees);
        int speed = SpinMath.clampEntitySpinSpeed(spinSpeed);
        if (speed <= 0) {
            return base;
        }
        double elapsedSeconds = (System.nanoTime() % 3_600_000_000_000L) / 1_000_000_000.0;
        double yaw = base + elapsedSeconds * speed;
        return (float) (yaw - Math.floor(yaw / 360.0) * 360.0);
    }

    private static int parseIntParam(String value, int fallback, int min, int max) {
        try {
            return Math.max(min, Math.min(max, Integer.parseInt(value == null ? "" : value.trim())));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static void prepareEntityForRender(Entity entity, boolean animated) {
        if (animated) {
            long now = System.nanoTime();
            entity.tickCount = (int) ((now / 50_000_000L) % 72000L);
            animationPartialTicks = (float) ((now % 50_000_000L) / 50_000_000.0);
        } else {
            entity.tickCount = 0;
            animationPartialTicks = 0.0F;
        }
        entity.setYRot(0.0F);
        entity.setXRot(0.0F);
        entity.yRotO = 0.0F;
        entity.xRotO = 0.0F;
        if (entity instanceof LivingEntity living) {
            living.yBodyRot = 0.0F;
            living.yBodyRotO = 0.0F;
            living.yHeadRot = 0.0F;
            living.yHeadRotO = 0.0F;
        }
        suppressConversionAnimation(entity);
    }

    private static void suppressConversionAnimation(Entity entity) {
        Class<?> clazz = entity.getClass();
        if (IMMUNE_NO_SETTER.contains(clazz)) {
            return;
        }
        Method setter = IMMUNE_SETTER_CACHE.get(clazz);
        if (setter == null) {
            setter = findImmuneSetter(clazz);
            if (setter == null) {
                IMMUNE_NO_SETTER.add(clazz);
                return;
            }
            setter.setAccessible(true);
            IMMUNE_SETTER_CACHE.put(clazz, setter);
        }
        try {
            setter.invoke(entity, true);
        } catch (Exception ignored) {
        }
    }

    private static Method findImmuneSetter(Class<?> clazz) {
        try {
            return clazz.getMethod("setImmuneToZombification", boolean.class);
        } catch (NoSuchMethodException ignored) {
        }
        for (Class<?> c = clazz; c != null && c != Object.class; c = c.getSuperclass()) {
            try {
                return c.getDeclaredMethod("setImmuneToZombification", boolean.class);
            } catch (NoSuchMethodException ignored) {
            }
        }
        return null;
    }

    @SuppressWarnings("deprecation")
    private static void renderEntityInInventory(GuiGraphics graphics, int x, int y, double scale, Entity entity, float yawDegrees, float pitchDegrees, boolean darkened, float partialTicks) {
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
        Quaternionf rotation = new Quaternionf().rotateXYZ((float) Math.toRadians(pitchDegrees), (float) Math.toRadians(yawDegrees), (float) Math.PI);
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0.0D);
        graphics.pose().mulPoseMatrix(new Matrix4f().scaling((float) scale, (float) scale, (float) -scale));
        graphics.pose().mulPose(rotation);
        graphics.pose().translate(0.0D, -entity.getBbHeight() / 2.0D, 0.0D);
        Lighting.setupForEntityInInventory();
        EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        dispatcher.setRenderShadow(false);
        dispatcher.overrideCameraOrientation(new Quaternionf());
        MultiBufferSource source = graphics.bufferSource();
        if (darkened) {
            source = requestedType -> new DarkenedVertexConsumer(graphics.bufferSource().getBuffer(requestedType), 0.05f, 0.05f, 0.06f, 1.0f);
        }
        MultiBufferSource finalSource = source;
        RenderSystem.runAsFancy(() -> dispatcher.render(entity, 0.0D, 0.0D, 0.0D, 0.0F, partialTicks, graphics.pose(), finalSource, 15728880));
        graphics.flush();
        dispatcher.setRenderShadow(true);
        graphics.pose().popPose();
        Lighting.setupFor3DItems();
        RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
    }

    private record EntityAsset(String entityId, String variantKey) {
        String cacheKey() {
            return variantKey.isBlank() ? entityId : entityId + "?variant=" + variantKey;
        }
    }

    private record DarkenedVertexConsumer(VertexConsumer delegate, float r, float g, float b, float a) implements VertexConsumer {
        @Override
        public VertexConsumer vertex(double x, double y, double z) {
            return delegate.vertex(x, y, z);
        }

        @Override
        public VertexConsumer color(int red, int green, int blue, int alpha) {
            int nr = Math.max(0, Math.min(255, (int) (red * r)));
            int ng = Math.max(0, Math.min(255, (int) (green * g)));
            int nb = Math.max(0, Math.min(255, (int) (blue * b)));
            int na = Math.max(0, Math.min(255, (int) (alpha * a)));
            return delegate.color(nr, ng, nb, na);
        }

        @Override
        public VertexConsumer uv(float u, float v) {
            return delegate.uv(u, v);
        }

        @Override
        public VertexConsumer overlayCoords(int u, int v) {
            return delegate.overlayCoords(u, v);
        }

        @Override
        public VertexConsumer uv2(int u, int v) {
            return delegate.uv2(u, v);
        }

        @Override
        public VertexConsumer normal(float x, float y, float z) {
            return delegate.normal(x, y, z);
        }

        @Override
        public void endVertex() {
            delegate.endVertex();
        }

        @Override
        public void defaultColor(int red, int green, int blue, int alpha) {
            delegate.defaultColor(red, green, blue, alpha);
        }

        @Override
        public void unsetDefaultColor() {
            delegate.unsetDefaultColor();
        }
    }

    private static final class SpinMath {
        static final int MIN_ENTITY_SPIN_SPEED = 0;
        static final int MAX_ENTITY_SPIN_SPEED = 360;
        static final int DEFAULT_MODEL_PITCH = 0;

        static int clampEntitySpinSpeed(int spinSpeed) {
            return Math.max(MIN_ENTITY_SPIN_SPEED, Math.min(MAX_ENTITY_SPIN_SPEED, spinSpeed));
        }

        static int normalizeDegrees(int degrees) {
            int v = degrees % 360;
            if (v < 0) {
                v += 360;
            }
            return v;
        }
    }
}
