package com.zhiyi.erp.module.mobile.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AnomalySupplementRequest {
    @NotBlank(message = "工号不能为空")
    private String employeeNo;
    @NotNull(message = "件数不能为空")
    private Integer qty;
    private String reportTime;
}
