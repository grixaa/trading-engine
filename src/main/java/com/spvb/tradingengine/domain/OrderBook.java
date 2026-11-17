package com.spvb.tradingengine.domain;

import java.util.List;

public record OrderBook(
        String seccode,
        List<QuoteLevel> bids,
        List<QuoteLevel> asks
) {}
