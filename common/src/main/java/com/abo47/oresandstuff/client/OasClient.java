package com.abo47.oresandstuff.client;

import com.abo47.oresandstuff.client.OasShaders;
import com.abo47.oresandstuff.client.screen.BioScanLibraryScreen;
import com.abo47.oresandstuff.client.theme.tokens.OasColors;
import com.abo47.oresandstuff.content.ModBlocks;
import com.abo47.oresandstuff.content.ModItems;
import com.abo47.oresandstuff.data.OreNodeDataManager;
import com.abo47.oresandstuff.item.BioScannerItem;
import com.abo47.oresandstuff.item.ScannerItem;
import com.abo47.oresandstuff.network.BioScanInfoPacket;
import com.abo47.oresandstuff.network.BioScanLibraryPacket;
import com.abo47.oresandstuff.network.ScannerResultPacket;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import com.abo47.oresandstuff.network.NetworkChannels;
import net.minecraft.world.item.Items;

public final class OasClient {
    private OasClient() {
    }

    private static Minecraft minecraft;
    private static ResourceLocation currentOreType;
    private static final List<ScannerFxTypes.TargetMarker> markers = new ArrayList<>();
    private static final List<ScannerFxTypes.ScanPulse> pulses = new ArrayList<>();
    private static long targetExpireMs = 0;
    private static ActiveBioScan activeBioScan;
    private static BioScanNotification activeBioNotification;
    private static long pendingBioNotificationUntilMs = 0L;

    public static void init(Minecraft mc) {
        minecraft = mc;
    }

    public static void onScannerResult(ScannerResultPacket packet) {
        if (minecraft == null || minecraft.player == null || minecraft.level == null) return;
        NodeClusterTracker.resetScanState();
        markers.clear();
        currentOreType = packet.oreType();
        long now = System.currentTimeMillis();
        List<ScannerResultPacket.NodeHit> sorted = packet.hits().stream().sorted(Comparator.comparingInt(ScannerResultPacket.NodeHit::distance)).toList();
        for (int i = 0; i < sorted.size(); i++) {
            ScannerResultPacket.NodeHit hit = sorted.get(i);
            markers.add(new ScannerFxTypes.TargetMarker(hit.pos(), hit.distance(), now + i * 170L));
        }
        targetExpireMs = now + 60000;
        pulses.clear();
        pulses.add(new ScannerFxTypes.ScanPulse(minecraft.player.getX(), minecraft.player.getY(), minecraft.player.getZ(), OasVisuals.SCANNER_PULSE_DURATION_MS));
        if (minecraft.level != null) {
            minecraft.level.playLocalSound(minecraft.player.getX(), minecraft.player.getY(), minecraft.player.getZ(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.85f, 1.08f, false);
        }
    }

    public static void onBioScanInfo(BioScanInfoPacket packet) {
        if (minecraft == null) return;
        activeBioScan = null;
        if (pendingBioNotificationUntilMs == 0L) return;
        pendingBioNotificationUntilMs = 0L;
        ResourceLocation entityId = ResourceLocation.tryParse(packet.entityId());
        EntityType<?> type = entityId == null ? null : BuiltInRegistries.ENTITY_TYPE.get(entityId);
        activeBioNotification = new BioScanNotification(packet, type, OasVisuals.BIO_SCAN_NOTIFICATION_MS);
    }

    public static void onBioScanLibrary(BioScanLibraryPacket packet) {
        if (minecraft == null) return;
        minecraft.setScreen(new BioScanLibraryScreen(packet.entries()));
    }

    private static boolean holdingOreScanner() {
        return minecraft != null && minecraft.player != null && minecraft.player.getMainHandItem().getItem() instanceof ScannerItem;
    }

    private static boolean holdingBioScanner() {
        return minecraft != null && minecraft.player != null && minecraft.player.getMainHandItem().getItem() instanceof BioScannerItem;
    }

    public static void clientTick() {
        if (minecraft == null || minecraft.player == null) return;
        if (OasKeyBindings.OPEN_SETTINGS.consumeClick() && minecraft.player != null) {
            if (minecraft.screen instanceof ModSettingsScreen.SettingsContainer) {
                minecraft.setScreen(null);
            } else if (minecraft.screen == null) {
                ModSettingsScreen.open(minecraft.player);
            }
        }
        boolean oreScannerHeld = holdingOreScanner();
        long now = System.currentTimeMillis();
        if (oreScannerHeld) {
            pulses.removeIf(p -> p.done(now));
            if (now > targetExpireMs) {
                markers.clear();
            }

            float playerYaw = minecraft.player.getYRot();
            for (ScannerFxTypes.TargetMarker marker : markers) {
                double dx = (marker.pos.getX() + 0.5) - minecraft.player.getX();
                double dz = (marker.pos.getZ() + 0.5) - minecraft.player.getZ();
                float dist = (float) Math.sqrt(dx * dx + dz * dz);
                marker.distance += (dist - marker.distance) * 0.10f;
                double targetYaw = Math.toDegrees(Math.atan2(-dx, dz));
                float delta = Mth.wrapDegrees((float) targetYaw - playerYaw);
                float desiredOffset = marker.distance < 7.0f ? 0f : (float) (Math.sin(Math.toRadians(delta)) * 100.0);
                if (!marker.initialized) {
                    marker.smoothOffset = desiredOffset;
                    marker.initialized = true;
                }
                float smoothing = marker.distance < 14.0f ? 0.10f : 0.16f;
                marker.smoothOffset = Mth.clamp(marker.smoothOffset + (desiredOffset - marker.smoothOffset) * smoothing, -102f, 102f);
            }
        }

        if (activeBioScan != null) {
            Entity target = null;
            for (var e : minecraft.level.entitiesForRendering()) {
                if (e.getUUID().equals(activeBioScan.entityUuid)) {
                    target = e;
                    break;
                }
            }
            if (target == null) {
                activeBioScan = null;
                return;
            }
            long dtMs = Math.max(1, now - activeBioScan.lastUpdateMs);
            activeBioScan.lastUpdateMs = now;
            Vec3 eye = minecraft.player.getEyePosition();
            Vec3 to = target.position().add(0, target.getBbHeight() * 0.5, 0).subtract(eye);
            double dist = to.length();
            Vec3 look = minecraft.player.getLookAngle();
            double dot = dist > 0.0001 ? look.normalize().dot(to.normalize()) : 0;
            boolean stableLock = holdingBioScanner() && minecraft.options.keyUse.isDown() && dist <= 24.0 && dot > 0.94;
            activeBioScan.locked = stableLock;
            float rate = dtMs / (float) activeBioScan.durationMs;
            if (stableLock) {
                activeBioScan.progress01 = Mth.clamp(activeBioScan.progress01 + rate, 0f, 1f);
            } else {
                activeBioScan.progress01 = Mth.clamp(activeBioScan.progress01 - rate * OasVisuals.BIO_SCAN_DRAIN_MULTIPLIER, 0f, 1f);
                if (activeBioScan.progress01 <= 0.001f) {
                    activeBioScan = null;
                    return;
                }
            }
            if (activeBioScan.progress01 >= 1f && !activeBioScan.requestSent) {
                activeBioScan.requestSent = true;
                pendingBioNotificationUntilMs = System.currentTimeMillis() + 12000L;
                NetworkChannels.bioScanRequest(activeBioScan.entityId);
                int cooldown = OasVisuals.BIO_SCAN_COOLDOWN_TICKS;
                if (cooldown > 0) {
                    minecraft.player.getCooldowns().addCooldown(ModItems.BIO_SCANNER, cooldown);
                }
                minecraft.level.playLocalSound(minecraft.player.getX(), minecraft.player.getY(), minecraft.player.getZ(), SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.PLAYERS, 1.0f, 1.25f, false);
            }
        } else if (holdingBioScanner() && minecraft.options.keyUse.isDown()) {
            if (minecraft.player.getCooldowns().isOnCooldown(ModItems.BIO_SCANNER)) return;
            var hit = findBioTarget();
            if (hit != null && hit.getEntity() instanceof LivingEntity living) {
                activeBioScan = new ActiveBioScan(living.getUUID(), living.getId(), OasVisuals.BIO_SCAN_DURATION_MS);
                minecraft.level.playLocalSound(minecraft.player.getX(), minecraft.player.getY(), minecraft.player.getZ(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.85f, 1.1f, false);
            }
        }
    }

    private static EntityHitResult findBioTarget() {
        if (minecraft == null || minecraft.player == null || minecraft.level == null) return null;
        Vec3 eye = minecraft.player.getEyePosition();
        Vec3 look = minecraft.player.getLookAngle();
        Vec3 end = eye.add(look.scale(24.0));
        AABB aabb = minecraft.player.getBoundingBox().expandTowards(look.scale(24.0)).inflate(1.0);
        return ProjectileUtil.getEntityHitResult(minecraft.player, eye, end, aabb, e -> e instanceof LivingEntity && e.isAlive(), 24.0 * 24.0);
    }

    public static void renderLevel(PoseStack pose, float partialTick, Matrix4f projectionMatrix) {
        OasShaders.ensureLoaded();
        if (minecraft == null || minecraft.level == null || minecraft.player == null) return;
        boolean oreScannerHeld = holdingOreScanner();
        boolean bioScannerHeld = holdingBioScanner();
        if (!oreScannerHeld && !bioScannerHeld && activeBioScan == null) return;
        if (pulses.isEmpty() && NodeClusterTracker.activeFlashes().isEmpty() && activeBioScan == null) return;

        Vec3 cameraPos = minecraft.gameRenderer.getMainCamera().getPosition();
        Vec3 cameraLook = minecraft.player.getLookAngle().normalize();
        pose.pushPose();
        pose.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
        Matrix4f invView = new Matrix4f(pose.last().pose()).invert();
        Matrix4f invProj = new Matrix4f(projectionMatrix).invert();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enablePolygonOffset();
        RenderSystem.polygonOffset(-2f, -2f);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        long now = System.currentTimeMillis();
        if (oreScannerHeld) {
            for (ScannerFxTypes.ScanPulse pulse : pulses) {
                float tRaw = (now - pulse.startMs) / (float) pulse.durationMs;
                float t = Mth.clamp(tRaw, 0f, 1f);
                float maxRadius = OasVisuals.SCANNER_PULSE_RANGE_BLOCKS;
                float elapsedSec = (now - pulse.startMs) / 1000f;
                float speedRadius = elapsedSec * OasVisuals.SCANNER_PULSE_SPEED_BLOCKS_PER_SEC;
                float eased = 1f - (1f - t) * (1f - t);
                float easedRadius = 3f + eased * maxRadius;
                float radius = Math.min(maxRadius, Math.min(speedRadius, easedRadius));

                ScannerRenderer.draw(pulse, radius, invView, invProj);
                NodeClusterTracker.triggerOnWaveHit(minecraft, pulse, radius, now, markers, currentOreType);
            }
        }

        if (activeBioScan != null && !activeBioScan.requestSent) {
            Entity target = minecraft.level.getEntity(activeBioScan.entityId);
            if (target != null && (activeBioScan.locked || activeBioScan.progress01 > 0.02f)) {
                RenderSystem.depthMask(true);
                RenderSystem.enableDepthTest();
                float partial = partialTick;
                double rx = Mth.lerp(partial, target.xo, target.getX());
                double ry = Mth.lerp(partial, target.yo, target.getY());
                double rz = Mth.lerp(partial, target.zo, target.getZ());
                float pr = Mth.clamp(activeBioScan.progress01, 0f, 1f);
                pose.pushPose();
                pose.translate(rx, ry, rz);
                BioScanEntityOverlayRenderer.render(pose, partial, minecraft.renderBuffers().bufferSource(), target, pr);
                pose.popPose();
                BioScanVisualRenderer.render(pose, minecraft, target, rx, ry, rz, partial, pr, now, activeBioScan.startedMs, invView, invProj);
                RenderSystem.depthMask(false);
            }
        }

        NodeHighlightRenderer.draw(pose, minecraft, NodeClusterTracker.activeFlashes(), NodeClusterTracker.hitBursts(), now);

        RenderSystem.disablePolygonOffset();
        RenderSystem.polygonOffset(0f, 0f);
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        pose.popPose();
    }

    public static void renderHud(GuiGraphics g, int scaledWidth, int scaledHeight) {
        OasShaders.ensureLoaded();
        if (minecraft == null || minecraft.player == null) return;
        boolean oreScannerHeld = holdingOreScanner();
        if (!oreScannerHeld && activeBioScan == null && activeBioNotification == null) return;
        int w = scaledWidth;
        int cx = w / 2;
        if (oreScannerHeld) {
            int y = 14;
            int left = cx - 110;
            int right = cx + 110;
            g.fill(left, y, right, y + 14, OasColors.withAlpha(OasColors.TEXT_PRIMARY, 0xAA));
            g.fill(left + 1, y + 1, right - 1, y + 13, OasColors.withAlpha(OasColors.BG_0, 0x77));
            for (int i = -100; i <= 100; i += 10) {
                int px = cx + i;
                int hh = (i % 50 == 0) ? 8 : 4;
                g.fill(px, y + 3, px + 1, y + 3 + hh, OasColors.withAlpha(OasColors.TEXT_PRIMARY, 0xCC));
            }
            g.drawCenteredString(minecraft.font, "N", cx, y - 10, OasColors.TEXT_SECONDARY);
            long now = System.currentTimeMillis();
            if (currentOreType != null) {
                OreNodeDataManager.INSTANCE.getNodeType(currentOreType).ifPresent(type -> {
                    ResourceLocation outItem = type.outputItem();
                    Item item = outItem == null ? null : BuiltInRegistries.ITEM.get(outItem);
                    if (item == null || item == Items.AIR) return;
                    for (ScannerFxTypes.TargetMarker marker : markers) {
                        if (!marker.visible(now)) continue;
                        if (!NodeClusterTracker.isNodeTouched(marker.pos)) continue;
                        int iconX = cx + (int) marker.smoothOffset - 8;
                        int iconY = y - 2;
                        g.renderItem(new ItemStack(item), iconX, iconY);
                        String d = (int) marker.distance + "m";
                        g.drawCenteredString(minecraft.font, d, iconX + 8, y + 18, OasColors.TEXT_PRIMARY);
                    }
                });
            }
            if (markers.isEmpty()) g.drawCenteredString(minecraft.font, "No scan target", cx, y + 18, OasColors.TEXT_MUTED);
        }

        if (activeBioScan != null) {
            float p = Mth.clamp(activeBioScan.progress01, 0f, 1f);
            int bw = 140;
            int bx = (w - bw) / 2;
            int by = scaledHeight - 44;
            g.fill(bx, by, bx + bw, by + 8, OasColors.withAlpha(OasColors.BG_0, 0xAA));
            g.fill(bx + 1, by + 1, bx + 1 + (int) ((bw - 2) * p), by + 7, OasColors.ACCENT_PRIMARY);
            g.drawCenteredString(minecraft.font, activeBioScan.requestSent ? "Decoding..." : "Scanning...", w / 2, by - 10, OasColors.ACCENT_SOFT);
        }

        if (activeBioNotification != null) {
            long now = System.currentTimeMillis();
            long age = now - activeBioNotification.startMs;
            if (age >= activeBioNotification.durationMs) {
                activeBioNotification = null;
            } else {
                float ageF = (float) age;
                float inMs = 280f;
                float outMs = 320f;
                float slideProgress;
                if (ageF < inMs) {
                    slideProgress = ageF / inMs;
                } else if (ageF > activeBioNotification.durationMs - outMs) {
                    slideProgress = (activeBioNotification.durationMs - ageF) / outMs;
                } else {
                    slideProgress = 1f;
                }
                slideProgress = Mth.clamp(slideProgress, 0f, 1f);

                int panelW = 280;
                int panelH = 86;
                int y = 14;
                int x = (int) (-panelW + panelW * slideProgress);

                g.fill(x, y, x + panelW, y + panelH, OasColors.withAlpha(OasColors.BG_0, 0xCC));
                g.fill(x, y, x + 2, y + panelH, OasColors.ACCENT_PRIMARY);

                int leftPaneW = 86;
                g.fill(x + 4, y + 4, x + leftPaneW, y + panelH - 4, OasColors.withAlpha(OasColors.BG_2, 0x66));
                g.fill(x + leftPaneW + 2, y + 4, x + panelW - 4, y + panelH - 4, OasColors.withAlpha(OasColors.BG_2, 0x44));

                if (activeBioNotification.entityType != null && minecraft.level != null) {
                    LivingEntity preview = activeBioNotification.entityType.create(minecraft.level) instanceof LivingEntity living ? living : null;
                    if (preview != null) {
                        preview.tickCount = (int) (now / 50L);
                        InventoryScreen.renderEntityInInventoryFollowsMouse(g, x + 44, y + 74, 26, 0f, 0f, preview);
                    }
                }

                int tx = x + leftPaneW + 8;
                g.drawString(minecraft.font, "BIO SCAN COMPLETE", tx, y + 8, OasColors.ACCENT_SOFT, false);
                g.drawString(minecraft.font, activeBioNotification.data.title(), tx, y + 20, OasColors.TEXT_PRIMARY, false);
                g.drawString(minecraft.font, activeBioNotification.data.category(), tx, y + 31, OasColors.ACCENT_MINT, false);
                String summary = activeBioNotification.data.summary();
                if (summary.length() > 86) summary = summary.substring(0, 86) + "...";
                g.drawString(minecraft.font, summary, tx, y + 44, OasColors.TEXT_SECONDARY, false);
                if (!activeBioNotification.data.facts().isEmpty()) {
                    String fact = activeBioNotification.data.facts().get(0);
                    if (fact.length() > 84) fact = fact.substring(0, 84) + "...";
                    g.drawString(minecraft.font, "- " + fact, tx, y + 58, OasColors.TEXT_SECONDARY, false);
                }
            }
        }
    }

    private static final class ActiveBioScan {
        final UUID entityUuid;
        final int entityId;
        final long durationMs;
        float progress01;
        long lastUpdateMs;
        long startedMs;
        boolean requestSent;
        boolean locked;

        ActiveBioScan(UUID entityUuid, int entityId, int durationMs) {
            this.entityUuid = entityUuid;
            this.entityId = entityId;
            this.startedMs = System.currentTimeMillis();
            this.durationMs = Math.max(300, durationMs);
            this.lastUpdateMs = this.startedMs;
        }
    }

    private static final class BioScanNotification {
        final BioScanInfoPacket data;
        final EntityType<?> entityType;
        final long startMs;
        final long durationMs;

        BioScanNotification(BioScanInfoPacket data, EntityType<?> entityType, long durationMs) {
            this.data = data;
            this.entityType = entityType;
            this.durationMs = durationMs;
            this.startMs = System.currentTimeMillis();
        }
    }
}
