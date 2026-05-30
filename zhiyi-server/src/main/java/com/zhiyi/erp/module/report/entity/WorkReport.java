package com.zhiyi.erp.module.report.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("work_report")
public class WorkReport {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long factoryId;
    private Long bundleId;
    private Long orderId;
    private Long processId;
    private Long employeeId;
    private Integer qty;
    private BigDecimal price;
    private BigDecimal amount;
    private Integer reportType;
    private Integer status;
    private LocalDateTime reportTime;
    private String clearReason;
    private Integer supplementFlag;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    private Long createdBy;
}
