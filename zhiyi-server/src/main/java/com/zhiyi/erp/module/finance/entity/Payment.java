package com.zhiyi.erp.module.finance.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("payment")
public class Payment {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long factoryId;
    private Long orderId;
    private Integer type;
    private BigDecimal amount;
    private LocalDate paymentDate;
    private String remark;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    private Long createdBy;
}
