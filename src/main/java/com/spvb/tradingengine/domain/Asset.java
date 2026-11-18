package com.spvb.tradingengine.domain;

import com.spvb.tradingengine.enums.AssetType;
import lombok.Builder;
import lombok.NonNull;

@Builder
public record Asset(
        @NonNull String code,
        @NonNull AssetType type
) {}
