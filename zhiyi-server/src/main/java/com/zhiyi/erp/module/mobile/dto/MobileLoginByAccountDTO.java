package com.zhiyi.erp.module.mobile.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MobileLoginByAccountDTO {
    @NotBlank(message = "账号不能为空")
    private String account;
    @NotBlank(message = "密码不能为空")
    private String password;
}
