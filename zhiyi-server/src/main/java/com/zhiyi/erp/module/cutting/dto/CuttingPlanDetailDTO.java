package com.zhiyi.erp.module.cutting.dto;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class CuttingPlanDetailDTO {
    private Long id;
    private Long factoryId;
    private Long orderId;
    private String orderNo;
    private Long styleId;
    private String styleCode;
    private String styleName;
    private String fabric;          // 裁剪面料
    private String part;            // 裁剪部位
    private Integer bedNo;          // 床次
    private Integer layers;         // 铺料层数
    private Integer planQty;        // 总计划数
    private Integer actualQty;      // 总实际数
    private Integer qtyPerBundle;   // 每扎件数
    private Integer bundleCount;    // 总扎数
    private LocalDate cuttingDate;  // 裁床日期
    private String barcode;         // 裁床条形码ID
    private Integer status;         // 状态
    private LocalDateTime createdAt;
}
