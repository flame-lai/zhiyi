package com.zhiyi.erp.module.order.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
@TableName("order_material")
public class OrderMaterial {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    private String materialName;
    private Integer type;
    private BigDecimal unitQty;
    private String unit;
}
