package com.zhiyi.erp.module.mobile.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ColorSizeProgressVO {
    private String styleNo;
    private String color;
    private String size;
    private Integer totalBundles;
    private BigDecimal completionRate;
    private List<ProcessProgressRow> processes;  // 工序维度
    private List<BundleProcessRow> bundles;      // 扎号维度

    @Data
    public static class ProcessProgressRow {
        private Long processId;
        private String processName;
        private Integer completed;   // 已完成扎数
        private Integer total;       // 总扎数
        private BigDecimal rate;     // completed/total
    }

    @Data
    public static class BundleProcessRow {
        private Long bundleId;
        private String globalBundleNo;
        private String bundleNo;
        private List<BundleProcessStatus> processes;  // 该扎各工序状态
    }

    @Data
    public static class BundleProcessStatus {
        private Long processId;
        private String processName;
        private String status;     // DONE / PENDING
        private String doneBy;
        private String doneTime;
    }
}
