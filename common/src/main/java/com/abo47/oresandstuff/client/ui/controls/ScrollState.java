package com.abo47.oresandstuff.client.ui.controls;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

public final class ScrollState {
    private final IntSupplier valueSupplier;
    private final IntConsumer valueConsumer;
    private final BooleanSupplier draggingSupplier;
    private final Consumer<Boolean> draggingConsumer;

    private ScrollState(
            IntSupplier valueSupplier,
            IntConsumer valueConsumer,
            BooleanSupplier draggingSupplier,
            Consumer<Boolean> draggingConsumer
    ) {
        this.valueSupplier = valueSupplier;
        this.valueConsumer = valueConsumer;
        this.draggingSupplier = draggingSupplier;
        this.draggingConsumer = draggingConsumer;
    }

    public static ScrollState bind(
            IntSupplier valueSupplier,
            IntConsumer valueConsumer,
            BooleanSupplier draggingSupplier,
            Consumer<Boolean> draggingConsumer
    ) {
        return new ScrollState(valueSupplier, valueConsumer, draggingSupplier, draggingConsumer);
    }

    public int value() {
        return valueSupplier.getAsInt();
    }

    public void setValue(int value) {
        valueConsumer.accept(value);
    }

    public boolean dragging() {
        return draggingSupplier.getAsBoolean();
    }

    public void setDragging(boolean dragging) {
        draggingConsumer.accept(dragging);
    }
}
