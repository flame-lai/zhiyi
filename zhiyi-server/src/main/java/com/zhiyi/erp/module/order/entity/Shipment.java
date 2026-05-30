package com.zhiyi.erp.module.order.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("shipment")
public class Shipment {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long factoryId;
    private Long orderId;
    private Long orderItemId;
    private Integer qty;
    private LocalDate shipmentDate;
    private String remark;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    private Long createdBy;
}
