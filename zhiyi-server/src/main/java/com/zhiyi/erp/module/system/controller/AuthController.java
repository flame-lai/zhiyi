package com.zhiyi.erp.module.system.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.zhiyi.erp.common.result.R;
import com.zhiyi.erp.module.system.entity.Factory;
import com.zhiyi.erp.module.system.mapper.FactoryMapper;
import com.zhiyi.erp.module.system.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "认证管理", description = "登录、登出、获取用户信息")
@RestController
@RequestMapping("/api/system/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private FactoryMapper factoryMapper;

    @Data
    public static class LoginByAccountDTO {
        @NotBlank(message = "账号不能为空")
        private String account;
        @NotBlank(message = "密码不能为空")
        private String password;
    }

    @Data
    public static class LoginByCodeDTO {
        @NotBlank(message = "登录码不能为空")
        private String loginCode;
    }

    @Operation(summary = "账号密码登录")
    @PostMapping("/login")
    public R<Map<String, Object>> loginByAccount(@RequestBody LoginByAccountDTO dto) {
        return R.ok(authService.loginByAccount(dto.getAccount(), dto.getPassword()));
    }

    @Operation(summary = "登录码登录")
    @PostMapping("/login-by-code")
    public R<Map<String, Object>> loginByCode(@RequestBody LoginByCodeDTO dto) {
        return R.ok(authService.loginByCode(dto.getLoginCode()));
    }

    @Operation(summary = "登出")
    @PostMapping("/logout")
    public R<Void> logout() {
        StpUtil.logout();
        return R.ok();
    }

    @Operation(summary = "获取当前用户信息")
    @GetMapping("/info")
    public R<Map<String, Object>> getInfo() {
        Long factoryId = StpUtil.getSession().getLong("factoryId");
        Factory factory = factoryMapper.selectById(factoryId);

        Map<String, Object> info = new java.util.HashMap<>();
        info.put("factoryId", factoryId);
        info.put("factoryName", factory != null ? factory.getName() : "");
        info.put("employeeId", StpUtil.getSession().get("employeeId"));
        info.put("roleId", StpUtil.getSession().get("roleId"));
        return R.ok(info);
    }
}
