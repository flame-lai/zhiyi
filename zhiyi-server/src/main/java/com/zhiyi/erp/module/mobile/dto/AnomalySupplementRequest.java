package com.zhiyi.erp.module.mobile.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AnomalySupplementRequest {
    @NotNull(message = "工人不能为空")
    private Long employeeId;
    @NotNull(message = "件数不能为空")
    private Integer qty;
    private String reportTime;
}
