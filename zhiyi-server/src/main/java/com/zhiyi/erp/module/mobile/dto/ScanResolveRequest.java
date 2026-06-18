package com.zhiyi.erp.module.mobile.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ScanResolveRequest {
    @NotBlank(message = "扫码内容不能为空")
    private String code;
}
