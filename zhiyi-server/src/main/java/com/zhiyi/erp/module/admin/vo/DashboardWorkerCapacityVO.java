package com.zhiyi.erp.module.admin.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class DashboardWorkerCapacityVO {
    private Long employeeId;
    private String employeeNo;
    private String employeeName;
    private Integer reportCount;
    private Integer totalQty;
    private BigDecimal totalAmount;
    private BigDecimal avgPrice;
}
