package com.spvb.tradingengine.domain;

import org.springframework.lang.Nullable;

import lombok.NonNull;

import java.util.List;

public record CursorPage<T>(
        @NonNull List<T> items,
        @Nullable Long nextBeforeId
) {}
