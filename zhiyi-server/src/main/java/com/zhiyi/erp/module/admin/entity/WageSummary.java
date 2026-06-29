package com.zhiyi.erp.module.admin.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("wage_summary")
public class WageSummary {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long factoryId;
    private Long employeeId;
    @TableField("`year_month`")
    private String yearMonth;
    private BigDecimal pieceWage;
    private BigDecimal timedWage;
    private BigDecimal otherWage;
    private BigDecimal totalWage;
    private Integer status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
