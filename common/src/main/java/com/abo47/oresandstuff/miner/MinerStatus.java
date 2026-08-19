package com.abo47.oresandstuff.miner;

import net.minecraft.network.chat.Component;

public enum MinerStatus {
    NO_NODE(0, "miner.status.no_node", "miner.status.no_node.styled"),
    NO_POWER(1, "miner.status.no_power", "miner.status.no_power.styled"),
    OUTPUT_FULL(2, "miner.status.output_full", "miner.status.output_full.styled"),
    RUNNING(3, "miner.status.running", "miner.status.running.styled"),
    STOPPED(4, "miner.status.stopped", "miner.status.stopped.styled");

    private final int code;
    private final String plainKey;
    private final String styledKey;

    MinerStatus(int code, String plainKey, String styledKey) {
        this.code = code;
        this.plainKey = plainKey;
        this.styledKey = styledKey;
    }

    public int code() {
        return code;
    }

    public String plain() {
        return Component.translatable(plainKey).getString();
    }

    public String styled() {
        return Component.translatable(styledKey).getString();
    }
}