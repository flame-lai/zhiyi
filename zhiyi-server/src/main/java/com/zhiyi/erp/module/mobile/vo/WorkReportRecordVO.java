package com.zhiyi.erp.module.mobile.vo;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class WorkReportRecordVO {
    private Long id;
    private String reportTime;
    private String orderNo;
    private String styleNo;
    private String bundleNo;
    private String processName;
    private Integer qty;
    private BigDecimal price;
    private BigDecimal amount;
    private Integer showMobilePrice; // 1=显示工价, 0=隐藏工价
}
