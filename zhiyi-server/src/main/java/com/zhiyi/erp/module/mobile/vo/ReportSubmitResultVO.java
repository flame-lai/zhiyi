package com.zhiyi.erp.module.mobile.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class ReportSubmitResultVO {
    private Integer totalQty;
    private BigDecimal totalAmount;
    private List<ReportItem> items;
    private List<SkippedItem> skippedItems;

    @Data
    public static class ReportItem {
        private Long processId;
        private String processName;
        private Integer qty;
        private BigDecimal price;
        private BigDecimal amount;
    }

    @Data
    public static class SkippedItem {
        private String bundleNo;
        private String processName;
        private String reason;
    }
}
