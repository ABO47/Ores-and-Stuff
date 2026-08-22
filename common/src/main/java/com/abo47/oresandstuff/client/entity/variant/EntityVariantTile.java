package com.abo47.oresandstuff.client.entity.variant;

record EntityVariantTile(
        boolean folder,
        EntityVariantCatalog.VariantFolder folderEntry,
        EntityVariantCatalog.VariantEntry variantEntry
) {
    static EntityVariantTile folder(EntityVariantCatalog.VariantFolder folder) {
        return new EntityVariantTile(true, folder, null);
    }

    static EntityVariantTile variant(EntityVariantCatalog.VariantEntry variant) {
        return new EntityVariantTile(false, null, variant);
    }
}
