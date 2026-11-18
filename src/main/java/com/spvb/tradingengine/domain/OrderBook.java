package com.spvb.tradingengine.domain;

import lombok.NonNull;

import java.util.List;

public record OrderBook(
        @NonNull String seccode,
        @NonNull List<QuoteLevel> bids,
        @NonNull List<QuoteLevel> asks
) {}
