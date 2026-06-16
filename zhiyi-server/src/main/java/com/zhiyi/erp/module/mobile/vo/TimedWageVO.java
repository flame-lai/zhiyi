package com.zhiyi.erp.module.mobile.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class TimedWageVO {
    private Long id;
    private Long employeeId;
    private String employeeNo;
    private String employeeName;
    private LocalDate workDate;
    private BigDecimal hours;
    private BigDecimal hourlyRate;
    private BigDecimal amount;
    private String workContent;
}
