package com.zhiyi.erp.module.report.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ReportRecordDTO {
    private Long id;
    private LocalDateTime reportTime;
    private Integer qty;
    private BigDecimal price;
    private BigDecimal amount;
    // JOIN 字段
    private String orderNo;
    private String styleCode;
    private Integer showMobilePrice;
    private String bundleNo;
    private String processName;
}
