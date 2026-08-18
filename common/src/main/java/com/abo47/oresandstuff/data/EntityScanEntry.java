package com.abo47.oresandstuff.data;

import java.util.List;

import net.minecraft.resources.ResourceLocation;

public record EntityScanEntry(ResourceLocation entityId,
                              String title,
                              String category,
                              String summary,
                              List<String> facts) {
}
