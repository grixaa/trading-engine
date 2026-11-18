package com.spvb.tradingengine.api.dto;

import com.spvb.tradingengine.enums.AssetType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import static com.spvb.tradingengine.util.ApiConstants.ASSET_CODE_PATTERN;

public record CreateAssetRequest(
        @NotNull @Pattern(regexp = ASSET_CODE_PATTERN) String code,
        @NotNull AssetType type
) {}
