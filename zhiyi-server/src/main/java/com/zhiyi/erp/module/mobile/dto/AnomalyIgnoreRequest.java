package com.zhiyi.erp.module.mobile.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AnomalyIgnoreRequest {
    @NotBlank(message = "原因不能为空")
    private String remark;
}
