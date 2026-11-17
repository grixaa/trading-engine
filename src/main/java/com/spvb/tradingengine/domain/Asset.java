package com.spvb.tradingengine.domain;

import lombok.Builder;
import lombok.NonNull;

@Builder
public record Asset(
        @NonNull String code,
        @NonNull AssetType type
) {}
