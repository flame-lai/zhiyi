package com.zhiyi.erp.module.mobile.vo;

import lombok.Data;

import java.util.List;

@Data
public class BundleDetailVO {
    private Long id;
    private String globalBundleNo;
    private String bundleNo;
    private Integer qty;
    private String orderNo;
    private String styleNo;
    private String color;
    private String size;
    private String status;  // 生产中 / 已完成
    private List<ProcessReportStatusVO> processes;
}
