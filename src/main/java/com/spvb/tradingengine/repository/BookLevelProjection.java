package com.spvb.tradingengine.repository;

import java.math.BigDecimal;

public interface BookLevelProjection {
    String getSide();

    BigDecimal getPrice();

    long getQuantity();
}
