package com.abo47.oresandstuff.data;

import net.minecraft.resources.ResourceLocation;

import java.util.List;

public record EntityScanEntry(ResourceLocation entityId,
                              String title,
                              String category,
                              String summary,
                              List<String> facts) {
}
