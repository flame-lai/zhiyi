package com.zhiyi.erp.module.mobile.vo;

import lombok.Data;
import java.util.List;

@Data
public class BundleScanVO {
    private String scanType = "BUNDLE";
    private String code;
    private BundleInfo bundle;
    private List<ProcessReportStatusVO> processes;

    @Data
    public static class BundleInfo {
        private Long id;
        private String globalBundleNo;
        private String bundleNo;
        private Integer qty;
        private String orderNo;
        private String styleNo;
        private String color;
        private String size;
    }
}
