package com.zhiyi.erp.module.admin.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AnomalyAlertVO {
    private Long id;
    private Long bundleId;
    private String bundleNo;
    private String globalBundleNo;
    private Long orderId;
    private String orderNo;
    private String styleCode;
    private String styleName;
    private String color;
    private String size;
    private Long frontProcessId;
    private String frontProcessName;
    private Long backProcessId;
    private String backProcessName;
    private Long reportEmployeeId;
    private String reportEmployeeNo;
    private String reportEmployeeName;
    private Integer reportQty;
    private LocalDateTime reportTime;
    private String description;
    private Integer status;
    private Integer handleType;
    private Long handleBy;
    private String handleByName;
    private LocalDateTime handleTime;
    private String handleRemark;
    private LocalDateTime createdAt;
}
