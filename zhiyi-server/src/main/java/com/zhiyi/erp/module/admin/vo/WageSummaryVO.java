package com.zhiyi.erp.module.admin.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class WageSummaryVO {
    private Long summaryId;
    private Long employeeId;
    private String employeeNo;
    private String employeeName;
    private String yearMonth;
    private Integer pieceQty;
    private BigDecimal pieceWage;
    private BigDecimal timedHours;
    private BigDecimal timedWage;
    private BigDecimal otherWage;
    private BigDecimal totalWage;
    private Integer status;
}
