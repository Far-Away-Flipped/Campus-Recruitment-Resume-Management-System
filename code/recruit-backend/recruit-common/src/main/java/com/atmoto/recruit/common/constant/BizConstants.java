package com.atmoto.recruit.common.constant;

/** 业务常量 —— URL前缀、文件白名单、限额等 */
public class BizConstants {
    public static final String PORTAL_PREFIX = "/api/portal";
    public static final String ADMIN_PREFIX = "/api/admin";
    public static final String SYSTEM_PREFIX = "/api/system";
    public static final String[] ALLOWED_FILE_EXTENSIONS = {".pdf", ".doc", ".docx"};
    public static final long MAX_FILE_SIZE = 10 * 1024 * 1024L;
    public static final int EXPORT_DAILY_LIMIT = 2000;
    public static final int BATCH_OPERATION_LIMIT = 200;
}
