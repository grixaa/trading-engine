package com.spvb.tradingengine.enums;

public enum OrderStatus {
    OPEN,
    PARTIALLY_FILLED,
    FILLED,
    CANCELLED;

    public boolean isActive() {
        return this == OPEN || this == PARTIALLY_FILLED;
    }

    public static OrderStatus forFill(final int filled, final int quantity) {
        if (filled == 0) {
            return OPEN;
        }

        return filled == quantity
                ? FILLED
                : PARTIALLY_FILLED;
    }
}
