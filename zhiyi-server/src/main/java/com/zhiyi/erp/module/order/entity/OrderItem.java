package com.zhiyi.erp.module.order.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
@TableName("order_item")
public class OrderItem {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    private Long styleId;
    private String color;
    private String size;
    private Integer qty;
    private BigDecimal unitPrice;
}
