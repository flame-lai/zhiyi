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
    private String color;
    private String size;
    private Integer bedNo;
    private Integer planQty;
    private Integer actualQty;
    private Integer qtyPerBundle;
    private Integer bundleCount;
    private LocalDate cuttingDate;
    private String barcode;
    private Integer status;
    private LocalDateTime createdAt;
}
