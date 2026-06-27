package com.zhiyi.erp.module.mobile.vo;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class AdminReportVO {
    private Long id;
    private Long employeeId;
    private String employeeName;
    private String employeeNo;
    private String orderNo;
    private String bundleNo;
    private String processName;
    private Integer qty;
    private BigDecimal price;
    private BigDecimal amount;
    private Integer reportType;
    private Integer status;
    private String reportTime;
    private String clearReason;
    private Integer supplementFlag;
}
