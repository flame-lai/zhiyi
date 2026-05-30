package com.zhiyi.erp.module.order.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
@TableName("order_process")
public class OrderProcess {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    private Long processId;
    private BigDecimal processPrice;
    private Integer sort;
}
