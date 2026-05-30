package com.zhiyi.erp.module.admin.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("timed_wage")
public class TimedWage {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long factoryId;
    private Long employeeId;
    private LocalDate workDate;
    private BigDecimal hours;
    private BigDecimal hourlyRate;
    private BigDecimal amount;
    private String workContent;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    private Long createdBy;
}
