package com.abo47.oresandstuff.miner;

public enum MinerStatus {
    NO_NODE(0, "No node", "§cNO NODE"),
    NO_POWER(1, "No power", "§cNO POWER"),
    OUTPUT_FULL(2, "Output full", "§6FULL"),
    RUNNING(3, "Running", "§bRUN"),
    STOPPED(4, "Stopped", "§7STOP");

    private final int code;
    private final String plain;
    private final String styled;

    MinerStatus(int code, String plain, String styled) {
        this.code = code;
        this.plain = plain;
        this.styled = styled;
    }

    public int code() {
        return code;
    }

    public String plain() {
        return plain;
    }

    public String styled() {
        return styled;
    }
}
