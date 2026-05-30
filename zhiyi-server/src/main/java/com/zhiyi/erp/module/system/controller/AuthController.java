package com.zhiyi.erp.module.system.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.zhiyi.erp.common.result.R;
import com.zhiyi.erp.module.system.service.AuthService;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/system/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

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

    @PostMapping("/login")
    public R<Map<String, Object>> loginByAccount(@RequestBody LoginByAccountDTO dto) {
        return R.ok(authService.loginByAccount(dto.getAccount(), dto.getPassword()));
    }

    @PostMapping("/login-by-code")
    public R<Map<String, Object>> loginByCode(@RequestBody LoginByCodeDTO dto) {
        return R.ok(authService.loginByCode(dto.getLoginCode()));
    }

    @PostMapping("/logout")
    public R<Void> logout() {
        StpUtil.logout();
        return R.ok();
    }

    @GetMapping("/info")
    public R<Map<String, Object>> getInfo() {
        Map<String, Object> info = new java.util.HashMap<>();
        info.put("factoryId", StpUtil.getSession().get("factoryId"));
        info.put("employeeId", StpUtil.getSession().get("employeeId"));
        info.put("roleId", StpUtil.getSession().get("roleId"));
        return R.ok(info);
    }
}
