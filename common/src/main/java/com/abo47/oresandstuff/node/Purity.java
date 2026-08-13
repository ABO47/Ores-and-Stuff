package com.abo47.oresandstuff.node;

public enum Purity {
    IMPURE(0.5, 0.5),
    NORMAL(1.0, 1.0),
    PURE(2.0, 1.5);

    private final double minerMultiplier;
    private final double manualMultiplier;

    Purity(double minerMultiplier, double manualMultiplier) {
        this.minerMultiplier = minerMultiplier;
        this.manualMultiplier = manualMultiplier;
    }

    public double minerMultiplier() {
        return minerMultiplier;
    }

    public double manualMultiplier() {
        return manualMultiplier;
    }
}
