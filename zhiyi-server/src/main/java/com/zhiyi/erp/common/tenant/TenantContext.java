package com.zhiyi.erp.common.tenant;

public class TenantContext {
    private static final ThreadLocal<Long> TENANT_ID = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> SKIP_TENANT = ThreadLocal.withInitial(() -> false);

    public static void set(Long factoryId) {
        TENANT_ID.set(factoryId);
    }

    public static Long get() {
        return TENANT_ID.get();
    }

    public static void clear() {
        TENANT_ID.remove();
    }

    public static void skipTenant() {
        SKIP_TENANT.set(true);
    }

    public static void enableTenant() {
        SKIP_TENANT.set(false);
    }

    public static boolean isSkipTenant() {
        return SKIP_TENANT.get();
    }
}
