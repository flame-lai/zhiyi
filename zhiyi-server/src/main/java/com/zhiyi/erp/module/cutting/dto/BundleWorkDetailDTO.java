package com.zhiyi.erp.module.cutting.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class BundleWorkDetailDTO {
    private Long processId;
    private String processName;     // 工序名称
    private Boolean completed;      // 是否完成
    private String employeeName;    // 报工人
    private LocalDateTime reportTime; // 报工时间
}
