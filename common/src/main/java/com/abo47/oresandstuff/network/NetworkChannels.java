package com.abo47.oresandstuff.network;

import net.minecraft.resources.ResourceLocation;

import com.lowdragmc.lowdraglib.networking.LDLNetworking;

import com.abo47.oresandstuff.OresAndStuffMod;

public final class NetworkChannels {
    private NetworkChannels() {
    }

    public static void register() {
        LDLNetworking.NETWORK.registerC2S(ScannerRequestPacket.class);
        LDLNetworking.NETWORK.registerC2S(BioScanRequestPacket.class);
        LDLNetworking.NETWORK.registerC2S(ScannerModeTogglePacket.class);
        LDLNetworking.NETWORK.registerS2C(ScannerResultPacket.class);
        LDLNetworking.NETWORK.registerS2C(BioScanInfoPacket.class);
        LDLNetworking.NETWORK.registerS2C(BioScanLibraryPacket.class);
    }

    public static void bioScanRequest(int entityId) {
        LDLNetworking.NETWORK.sendToServer(new BioScanRequestPacket(entityId));
    }

    public static void sendScannerRequest(ResourceLocation oreType) {
        LDLNetworking.NETWORK.sendToServer(new ScannerRequestPacket(oreType));
    }

    public static void sendScannerModeToggle() {
        LDLNetworking.NETWORK.sendToServer(new ScannerModeTogglePacket());
    }
}
