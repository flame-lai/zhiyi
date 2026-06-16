package com.zhiyi.erp.module.mobile.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class TimedWageRequest {
    @NotNull(message = "工人不能为空")
    private Long employeeId;
    private String workDate;
    @NotNull(message = "工时不能为空")
    private BigDecimal hours;
    @NotNull(message = "时薪不能为空")
    private BigDecimal hourlyRate;
    private String workContent;
}
