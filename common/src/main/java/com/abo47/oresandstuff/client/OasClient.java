package com.abo47.oresandstuff.client;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.joml.Matrix4f;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import com.abo47.oresandstuff.OresAndStuffConfig;
import com.abo47.oresandstuff.client.OasShaders;
import com.abo47.oresandstuff.client.screen.BioLibraryScreen;
import com.abo47.oresandstuff.client.theme.tokens.OasColors;
import com.abo47.oresandstuff.content.ModBlocks;
import com.abo47.oresandstuff.content.ModItems;
import com.abo47.oresandstuff.data.OreNodeDataManager;
import com.abo47.oresandstuff.item.BioScannerItem;
import com.abo47.oresandstuff.item.ScannerItem;
import com.abo47.oresandstuff.network.BioScanInfoPacket;
import com.abo47.oresandstuff.network.BioScanLibraryPacket;
import com.abo47.oresandstuff.network.NetworkChannels;
import com.abo47.oresandstuff.network.ScannerResultPacket;

public final class OasClient {
    private OasClient() {
    }

    static Minecraft minecraft;
    static ResourceLocation currentOreType;
    static final List<ScannerFxTypes.TargetMarker> markers = new ArrayList<>();
    private static final List<ScannerFxTypes.ScanPulse> pulses = new ArrayList<>();
    private static long targetExpireMs = 0;
    private static ActiveBioScan activeBioScan;
    private static boolean bioUseWasDown = false;
    private static final long BIO_SCAN_REPLY_TIMEOUT_MS = 1500L;

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
        var scannerCfg = OresAndStuffConfig.scanner();
        pulses.add(new ScannerFxTypes.ScanPulse(minecraft.player.getX(), minecraft.player.getY(), minecraft.player.getZ(), scannerCfg.pulseDurationMs));
        playConfigSound(SoundEvents.BEACON_POWER_SELECT, scannerCfg.scanSoundEnabled, scannerCfg.scanSoundVolume, scannerCfg.scanSoundPitch);
    }

    private static void playConfigSound(SoundEvent event, boolean enabled, double volume, double pitch) {
        if (!enabled || minecraft == null || minecraft.player == null || minecraft.level == null) {
            return;
        }
        minecraft.level.playLocalSound(minecraft.player.getX(), minecraft.player.getY(), minecraft.player.getZ(), event, SoundSource.PLAYERS, (float) volume, (float) pitch, false);
    }

    public static void onBioScanInfo(BioScanInfoPacket packet) {
        if (minecraft == null || activeBioScan == null) return;
        if (!activeBioScan.requestSent || activeBioScan.completed) return;
        activeBioScan.completed = true;
        activeBioScan.progress01 = 1f;
    }

    public static void onBioScanLibrary(BioScanLibraryPacket packet) {
        if (minecraft == null || minecraft.player == null) return;
        BioLibraryScreen.open(minecraft.player, packet.entries());
    }

    static boolean holdingOreScanner() {
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

        boolean bioUseDown = holdingBioScanner() && minecraft.options.keyUse.isDown();
        boolean bioUsePressed = bioUseDown && !bioUseWasDown;
        bioUseWasDown = bioUseDown;
        if (activeBioScan != null) {
            if (activeBioScan.completed) {
                activeBioScan = null;
            } else if (activeBioScan.requestSent && now - activeBioScan.requestSentMs > BIO_SCAN_REPLY_TIMEOUT_MS) {
                activeBioScan.completed = true;
                activeBioScan.progress01 = 1f;
            }
            if (activeBioScan != null && !activeBioScan.completed) {
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
            activeBioScan.ratePerMs = (stableLock ? 1f : -(float) OresAndStuffConfig.bioScan().drainMultiplier) / activeBioScan.durationMs;
            float rate = dtMs / (float) activeBioScan.durationMs;
            if (stableLock) {
                activeBioScan.progress01 = Mth.clamp(activeBioScan.progress01 + rate, 0f, 1f);
            } else {
                activeBioScan.progress01 = Mth.clamp(activeBioScan.progress01 - rate * (float) OresAndStuffConfig.bioScan().drainMultiplier, 0f, 1f);
                if (activeBioScan.progress01 <= 0.001f) {
                    activeBioScan = null;
                    return;
                }
            }
            if (activeBioScan.progress01 >= 1f && !activeBioScan.requestSent) {
                activeBioScan.requestSent = true;
                activeBioScan.requestSentMs = now;
                NetworkChannels.bioScanRequest(activeBioScan.entityId);
                int cooldown = OresAndStuffConfig.bioScan().cooldownTicks;
                if (cooldown > 0) {
                    minecraft.player.getCooldowns().addCooldown(ModItems.BIO_SCANNER, cooldown);
                }
                var sc = OresAndStuffConfig.scanner();
                playConfigSound(SoundEvents.RESPAWN_ANCHOR_CHARGE, sc.pingSoundEnabled, sc.pingSoundVolume, sc.pingSoundPitch);
            }
            }
        } else if (bioUsePressed) {
            if (minecraft.player.getCooldowns().isOnCooldown(ModItems.BIO_SCANNER)) return;
            var hit = findBioTarget();
            if (hit != null && hit.getEntity() instanceof LivingEntity living) {
                activeBioScan = new ActiveBioScan(living.getUUID(), living.getId(), OresAndStuffConfig.bioScan().durationMs);
                var sc = OresAndStuffConfig.scanner();
                playConfigSound(SoundEvents.BEACON_POWER_SELECT, sc.pingSoundEnabled, sc.pingSoundVolume, sc.pingSoundPitch);
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
            var scannerCfg = OresAndStuffConfig.scanner();
            for (ScannerFxTypes.ScanPulse pulse : pulses) {
                float tRaw = (now - pulse.startMs) / (float) pulse.durationMs;
                float t = Mth.clamp(tRaw, 0f, 1f);
                float maxRadius = scannerCfg.pulseRangeBlocks;
                float elapsedSec = (now - pulse.startMs) / 1000f;
                float speedRadius = (float) (elapsedSec * scannerCfg.pulseSpeedBlocksPerSec);
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

        RenderSystem.disableDepthTest();
        NodeHighlightRenderer.draw(pose, minecraft, NodeClusterTracker.activeFlashes(), NodeClusterTracker.hitBursts(), now);
        RenderSystem.enableDepthTest();

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
        if (!oreScannerHeld && activeBioScan == null) return;
        int w = scaledWidth;
        int cx = w / 2;
        if (oreScannerHeld) {
            ScannerHud.render(g, cx);
        }

        if (activeBioScan != null) {
            float p = renderProgress01(activeBioScan);
            int bw = 140;
            int bx = (w - bw) / 2;
            int by = scaledHeight - 44;
            g.fill(bx, by, bx + bw, by + 8, OasColors.withAlpha(OasColors.BG_0, 0xAA));
            g.fill(bx + 1, by + 1, bx + 1 + (int) ((bw - 2) * p), by + 7, OasColors.withAlpha(OasColors.ACCENT_PRIMARY, 255));
            String label = activeBioScan.completed ? net.minecraft.network.chat.Component.translatable("Completed").getString()
                    : (activeBioScan.progress01 >= 0.5f ? net.minecraft.network.chat.Component.translatable("Decoding...").getString() : net.minecraft.network.chat.Component.translatable("Scanning...").getString());
            g.drawCenteredString(minecraft.font, label, w / 2, by - 10, OasColors.ACCENT_SOFT);
            if (OresAndStuffConfig.debug().debugLogging && minecraft.level != null) {
                Entity dbgEntity = minecraft.level.getEntity(activeBioScan.entityId);
                if (dbgEntity != null) {
                    String dbg = String.format("BioDbg id=%d p=%.2f yaw=%.1f pos=(%.1f,%.1f,%.1f)",
                            dbgEntity.getId(), p, dbgEntity.getYRot(), dbgEntity.getX(), dbgEntity.getY(), dbgEntity.getZ());
                    g.drawCenteredString(minecraft.font, dbg, w / 2, by + 12, OasColors.TEXT_MUTED);
                }
            }
        }

    }

    private static float renderProgress01(ActiveBioScan scan) {
        if (scan.completed || scan.progress01 >= 1f) {
            return 1f;
        }
        float p = scan.progress01 + (System.currentTimeMillis() - scan.lastUpdateMs) * scan.ratePerMs;
        return Mth.clamp(p, 0f, 1f);
    }

    private static final class ActiveBioScan {
        final UUID entityUuid;
        final int entityId;
        final long durationMs;
        float progress01;
        float ratePerMs;
        long lastUpdateMs;
        long startedMs;
        long requestSentMs;
        boolean requestSent;
        boolean locked;
        boolean completed;

        ActiveBioScan(UUID entityUuid, int entityId, int durationMs) {
            this.entityUuid = entityUuid;
            this.entityId = entityId;
            this.startedMs = System.currentTimeMillis();
            this.durationMs = Math.max(300, durationMs);
            this.lastUpdateMs = this.startedMs;
        }
    }

}
