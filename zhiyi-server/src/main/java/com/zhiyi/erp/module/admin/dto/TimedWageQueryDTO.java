package com.zhiyi.erp.module.admin.dto;

import lombok.Data;

@Data
public class TimedWageQueryDTO {
    private Long employeeId;
    private String yearMonth;
    private String startDate;
    private String endDate;
}
