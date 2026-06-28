package com.zhiyi.erp.module.mobile.vo;

import lombok.Data;

@Data
public class BundleSuggestVO {
    private Long id;
    private String globalBundleNo;
    private String bundleNo;
    private String orderNo;
    private String color;
    private String size;
    private Integer qty;
}
