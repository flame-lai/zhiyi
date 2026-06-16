package com.zhiyi.erp.module.mobile.vo;

import lombok.Data;
import java.util.List;

@Data
public class CuttingScanVO {
    private String scanType = "CUTTING";
    private String code;
    private CuttingInfo cutting;
    private List<BundleItem> bundles;
    private List<ProcessReportStatusVO> processes;

    @Data
    public static class CuttingInfo {
        private Long id;
        private String barcode;
        private Integer bedNo;
        private Integer totalBundleCount;
        private Integer totalQty;
        private String orderNo;
        private String styleNo;
        private String color;
        private String size;
    }

    @Data
    public static class BundleItem {
        private Long id;
        private String globalBundleNo;
        private String bundleNo;
        private Integer qty;
        private Boolean disabled;
        private String disabledReason;
    }
}
