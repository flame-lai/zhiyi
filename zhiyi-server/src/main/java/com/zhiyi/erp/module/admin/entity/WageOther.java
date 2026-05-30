package com.zhiyi.erp.module.admin.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("wage_other")
public class WageOther {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long factoryId;
    private Long employeeId;
    private String yearMonth;
    private String itemName;
    private BigDecimal amount;
    private String remark;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    private Long createdBy;
}
