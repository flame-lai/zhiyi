package com.zhiyi.erp.module.admin.dto;

import lombok.Data;

@Data
public class WageQueryDTO {
    private String yearMonth;
    private Long employeeId;
    private String wageType;
    private Integer status;
}
