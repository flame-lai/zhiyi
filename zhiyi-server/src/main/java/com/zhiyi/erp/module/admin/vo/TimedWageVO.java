package com.zhiyi.erp.module.admin.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

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
    private LocalDateTime createdAt;
    private Long createdBy;
    private String createdByName;
}
