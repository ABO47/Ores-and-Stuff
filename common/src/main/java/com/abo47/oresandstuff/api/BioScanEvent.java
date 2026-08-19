package com.abo47.oresandstuff.api;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

/**
 * Fired on the server whenever a player completes a bio scan of an entity.
 * <p>
 * Listen via {@link BioScanEvents#register(java.util.function.Consumer)}.
 */
public record BioScanEvent(ServerPlayer player,
                           LivingEntity entity,
                           EntityType<?> entityType,
                           ResourceLocation entityId,
                           boolean firstScan) {
}
