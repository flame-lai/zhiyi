package com.zhiyi.erp.module.mobile.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MobileLoginByCodeDTO {
    @NotBlank(message = "登录码不能为空")
    private String loginCode;
}
