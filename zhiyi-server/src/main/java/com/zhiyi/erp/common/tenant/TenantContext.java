package com.zhiyi.erp.common.tenant;

public class TenantContext {
    private static final ThreadLocal<Long> TENANT_ID = new ThreadLocal<>();

    public static void set(Long factoryId) {
        TENANT_ID.set(factoryId);
    }

    public static Long get() {
        return TENANT_ID.get();
    }

    public static void clear() {
        TENANT_ID.remove();
    }
}
