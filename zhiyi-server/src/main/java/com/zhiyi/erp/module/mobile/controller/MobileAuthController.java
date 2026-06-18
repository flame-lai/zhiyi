package com.zhiyi.erp.module.mobile.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.zhiyi.erp.common.result.R;
import com.zhiyi.erp.module.mobile.dto.MobileLoginByAccountDTO;
import com.zhiyi.erp.module.mobile.dto.MobileLoginByCodeDTO;
import com.zhiyi.erp.module.mobile.service.MobileService;
import com.zhiyi.erp.module.mobile.vo.MobileLoginVO;
import com.zhiyi.erp.module.mobile.vo.MobileProfileVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Tag(name = "移动端认证", description = "移动端登录、登出、用户信息")
@RestController
@RequestMapping("/api/mobile/auth")
public class MobileAuthController {

    @Autowired
    private MobileService mobileService;

    @Operation(summary = "工号/登录码登录")
    @PostMapping("/login-by-code")
    public R<MobileLoginVO> loginByCode(@Valid @RequestBody MobileLoginByCodeDTO dto) {
        return R.ok(mobileService.loginByCode(dto.getLoginCode()));
    }

    @Operation(summary = "账号密码登录")
    @PostMapping("/login-by-account")
    public R<MobileLoginVO> loginByAccount(@Valid @RequestBody MobileLoginByAccountDTO dto) {
        return R.ok(mobileService.loginByAccount(dto.getAccount(), dto.getPassword()));
    }

    @Operation(summary = "登出")
    @PostMapping("/logout")
    public R<Void> logout() {
        StpUtil.logout();
        return R.ok();
    }

    @Operation(summary = "获取当前用户信息（含菜单权限）")
    @GetMapping("/profile")
    public R<MobileProfileVO> getProfile() {
        Long employeeId = StpUtil.getSession().getLong("employeeId");
        return R.ok(mobileService.getProfile(employeeId));
    }
}
