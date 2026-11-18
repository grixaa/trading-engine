package com.spvb.tradingengine.util;

import lombok.experimental.UtilityClass;

@UtilityClass
public final class ApiConstants {
    public static final String ASSET_CODE_PATTERN = "[A-Z0-9][A-Z0-9._-]{0,31}";
    public static final String USER_ID_HEADER = "X-User-Id";
}
