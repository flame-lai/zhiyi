package com.zhiyi.erp.module.admin.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class DashboardOverviewVO {
    private Long orderCount;
    private Integer bundleCount;
    private Integer orderQty;
    private BigDecimal averageCompletionRate;
    private Integer todayReportQty;
    private BigDecimal todayReportAmount;
}
