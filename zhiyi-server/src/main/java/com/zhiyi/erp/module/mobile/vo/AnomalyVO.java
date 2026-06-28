package com.zhiyi.erp.module.mobile.vo;

import lombok.Data;

@Data
public class AnomalyVO {
    private Long id;
    private String bundleNo;
    private String orderNo;
    private String description;
    private String frontProcessName;
    private String backProcessName;
    private String reportEmployeeName;
    private String reportTime;
    private Integer status;       // 0=待处理, 1=已忽略, 2=已清除, 3=已补录
    private Integer handleType;
    private String handleRemark;
    private String handleTime;
}
