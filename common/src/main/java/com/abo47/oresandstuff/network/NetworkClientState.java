package com.abo47.oresandstuff.network;

public final class NetworkClientState {
    private static ScannerResultPacket scannerResult;
    private static BioScanInfoPacket bioInfo;
    private static BioScanLibraryPacket bioLibrary;

    private NetworkClientState() {
    }

    public static synchronized void setScannerResult(ScannerResultPacket value) {
        scannerResult = value;
    }

    public static synchronized void setBioInfo(BioScanInfoPacket value) {
        bioInfo = value;
    }

    public static synchronized void setBioLibrary(BioScanLibraryPacket value) {
        bioLibrary = value;
    }

    public static synchronized ScannerResultPacket scannerResult() {
        return scannerResult;
    }

    public static synchronized BioScanInfoPacket bioInfo() {
        return bioInfo;
    }

    public static synchronized BioScanLibraryPacket bioLibrary() {
        return bioLibrary;
    }
}
