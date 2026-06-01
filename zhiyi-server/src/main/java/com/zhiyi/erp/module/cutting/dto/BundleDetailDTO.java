package com.zhiyi.erp.module.cutting.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class BundleDetailDTO {
    private Long id;
    private Long factoryId;
    private Long cuttingPlanId;
    private Long orderId;
    private String orderNo;
    private Long styleId;
    private String styleCode;
    private String styleName;
    private String color;
    private String size;
    private Integer bedNo;
    private String bundleNo;
    private String globalBundleNo;
    private Integer qty;
    private Integer status;
    private Integer completedProcesses;
    private Integer totalProcesses;
    private LocalDateTime createdAt;
}
