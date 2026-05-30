package com.zhiyi.erp.module.admin.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("anomaly_alert")
public class AnomalyAlert {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long factoryId;
    private Long bundleId;
    private Long orderId;
    private Long frontProcessId;
    private Long backProcessId;
    private Long reportEmployeeId;
    private Integer status;
    private Integer handleType;
    private Long handleBy;
    private LocalDateTime handleTime;
    private String handleRemark;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
