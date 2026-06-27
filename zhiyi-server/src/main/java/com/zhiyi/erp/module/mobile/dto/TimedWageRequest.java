package com.zhiyi.erp.module.mobile.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class TimedWageRequest {
    @NotBlank(message = "工号不能为空")
    private String employeeNo;
    private String workDate;
    @NotNull(message = "工时不能为空")
    private BigDecimal hours;
    @NotNull(message = "时薪不能为空")
    private BigDecimal hourlyRate;
    private String workContent;
}
