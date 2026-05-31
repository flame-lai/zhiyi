package com.zhiyi.erp.module.order.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("order_color_size")
public class OrderColorSize {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    private String color;
    private String size;
    private Integer qty;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
