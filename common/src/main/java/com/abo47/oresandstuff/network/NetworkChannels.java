package com.abo47.oresandstuff.network;

import com.abo47.oresandstuff.OresAndStuffMod;
import com.lowdragmc.lowdraglib.networking.LDLNetworking;
import net.minecraft.resources.ResourceLocation;

public final class NetworkChannels {
    private NetworkChannels() {
    }

    public static void register() {
        LDLNetworking.NETWORK.registerC2S(ScannerRequestPacket.class);
        LDLNetworking.NETWORK.registerC2S(BioScanRequestPacket.class);
        LDLNetworking.NETWORK.registerS2C(ScannerResultPacket.class);
        LDLNetworking.NETWORK.registerS2C(BioScanInfoPacket.class);
        LDLNetworking.NETWORK.registerS2C(BioScanLibraryPacket.class);
    }
}
