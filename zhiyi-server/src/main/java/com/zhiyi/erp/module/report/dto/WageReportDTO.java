package com.zhiyi.erp.module.report.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class WageReportDTO {
    private Long id;
    private Long orderId;
    private Long processId;
    private Integer qty;
    private BigDecimal price;
    private BigDecimal amount;
    private LocalDateTime reportTime;
    // JOIN 字段
    private String orderNo;
    private Long styleId;
    private String styleCode;
    private Integer showMobilePrice;
    private String processName;
}
