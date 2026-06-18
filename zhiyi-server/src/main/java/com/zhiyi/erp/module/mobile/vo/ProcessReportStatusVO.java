package com.zhiyi.erp.module.mobile.vo;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class ProcessReportStatusVO {
    private Long processId;
    private String processName;
    private BigDecimal price;
    private String status;  // AVAILABLE / DONE / PARTIAL_DONE / UNAVAILABLE
    private String doneBy;
    private String doneTime;
    private String hint;
}
