package com.zhiyi.erp.module.admin.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class DashboardOrderProgressVO {
    private Long orderId;
    private String orderNo;
    private String customerName;
    private String styleCode;
    private String styleName;
    private String colorSummary;
    private String sizeSummary;
    private LocalDate deliveryDate;
    private Integer status;
    private Integer totalBundles;
    private Integer totalQty;
    private Integer completedQty;
    private BigDecimal completionRate;
    private String bottleneckProcess;
    private List<ProcessProgress> processProgress;

    @Data
    public static class ProcessProgress {
        private Long processId;
        private String processName;
        private Integer completedQty;
        private Integer plannedQty;
        private BigDecimal completionRate;
    }
}
