package com.abo47.oresandstuff.item;

/**
 * Legacy bio scanner item - now merged into {@link ScannerItem}.
 * Kept for save compatibility; defaults to BIO mode when no NBT is present.
 * New players get the unified {@code scanner} item which can toggle modes.
 */
public class BioScannerItem extends ScannerItem {
    public BioScannerItem(Properties properties) {
        super(properties);
    }
}
