package com.spvb.tradingengine.domain;

import java.util.List;

public record CursorPage<T>(
        List<T> items,
        Long nextBeforeId
) {}
