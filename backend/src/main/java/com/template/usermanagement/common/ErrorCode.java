package com.template.usermanagement.common;

public final class ErrorCode {

    private ErrorCode() {}

    // Auth
    public static final String AUTH_INVALID_CREDENTIALS = "AUTH_001";
    public static final String AUTH_TOKEN_EXPIRED = "AUTH_002";
    public static final String AUTH_TOKEN_INVALID = "AUTH_003";
    public static final String AUTH_REFRESH_TOKEN_EXPIRED = "AUTH_004";
    public static final String AUTH_ACCESS_DENIED = "AUTH_005";

    // User
    public static final String USER_NOT_FOUND = "USER_001";
    public static final String USER_ALREADY_EXISTS = "USER_002";
    public static final String USER_INACTIVE = "USER_003";
    public static final String USER_DELETED = "USER_004";

    // Workflow
    public static final String PENDING_NOT_FOUND = "WF_001";
    public static final String PENDING_ALREADY_EXISTS = "WF_002";
    public static final String PENDING_SAME_MAKER_CHECKER = "WF_003";
    public static final String PENDING_INVALID_STATUS = "WF_004";
    public static final String PENDING_NOT_AUTHORIZED = "WF_005";

    // Role
    public static final String ROLE_NOT_FOUND = "ROLE_001";
    public static final String USER_NOT_IN_ROLE = "ROLE_002";

    // Wilayah
    public static final String WILAYAH_PROVINCE_NOT_FOUND = "WIL_001";
    public static final String WILAYAH_STATE_NOT_FOUND = "WIL_002";
    public static final String WILAYAH_DISTRICT_NOT_FOUND = "WIL_003";
    public static final String WILAYAH_SUBDISTRICT_NOT_FOUND = "WIL_004";
    public static final String WILAYAH_DUPLICATE_NAME = "WIL_005";

    // Bulk Upload
    public static final String BULK_UPLOAD_NOT_FOUND = "BULK_001";
    public static final String BULK_UPLOAD_INVALID_STATUS = "BULK_002";
    public static final String BULK_UPLOAD_PARSE_ERROR = "BULK_003";

    // Rate Limit
    public static final String RATE_LIMIT_EXCEEDED = "SEC_001";

    // General
    public static final String VALIDATION_ERROR = "GEN_001";
    public static final String INTERNAL_ERROR = "GEN_002";
    public static final String OPTIMISTIC_LOCK_ERROR = "GEN_003";
    public static final String NOT_FOUND = "GEN_004";
}
