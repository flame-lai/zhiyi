package com.zhiyi.erp.module.mobile.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class DashboardOrderVO {
    private Long orderId;
    private String orderNo;
    private String styleNo;
    private String orderDate;
    private Integer totalBundles;
    private BigDecimal completionRate;
    private String bottleneckProcess;
    private List<ProcessProgress> processProgress;

    @Data
    public static class ProcessProgress {
        private Long processId;
        private String processName;
        private Integer completed;
        private Integer total;
        private BigDecimal rate;
    }
}
