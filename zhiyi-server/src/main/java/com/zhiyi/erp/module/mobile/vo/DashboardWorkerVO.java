package com.zhiyi.erp.module.mobile.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class DashboardWorkerVO {
    private Long employeeId;
    private String employeeNo;
    private String employeeName;
    private Integer totalQty;
    private BigDecimal totalAmount;
}
