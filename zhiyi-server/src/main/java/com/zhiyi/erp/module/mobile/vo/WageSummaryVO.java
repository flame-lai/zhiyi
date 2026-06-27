package com.zhiyi.erp.module.mobile.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class WageSummaryVO {
    private Integer totalQty;
    private BigDecimal totalAmount;
    private BigDecimal visibleAmount;
    private List<StyleSummary> styleSummaries;
    private List<DailyGroup> dailyGroups;

    @Data
    public static class StyleSummary {
        private String styleNo;
        private Integer qty;
        private BigDecimal amount;
        private Integer showMobilePrice;
        private List<ProcessSummary> processes;
    }

    @Data
    public static class ProcessSummary {
        private String processName;
        private Integer qty;
        private BigDecimal price;
        private BigDecimal amount;
    }

    @Data
    public static class DailyGroup {
        private String date;
        private BigDecimal amount;
        private List<WorkReportRecordVO> records;
    }
}
