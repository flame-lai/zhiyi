package com.zhiyi.erp.common.tenant;

import cn.dev33.satoken.stp.StpUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class TenantInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        try {
            Object loginId = StpUtil.getLoginIdDefaultNull();
            if (loginId != null) {
                // 从Sa-Token的Session中获取factory_id
                Long factoryId = StpUtil.getSession().getLong("factoryId");
                if (factoryId != null) {
                    TenantContext.set(factoryId);
                }
            }
        } catch (Exception ignored) {
            // 未登录时忽略
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        TenantContext.clear();
    }
}
