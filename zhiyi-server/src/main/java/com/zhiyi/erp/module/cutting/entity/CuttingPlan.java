package com.zhiyi.erp.module.cutting.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("cutting_plan")
public class CuttingPlan {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long factoryId;
    private Long orderItemId;
    private Integer bedNo;
    private Integer planQty;
    private Integer actualQty;
    private Integer qtyPerBundle;
    private Integer bundleCount;
    private LocalDate cuttingDate;
    private String barcode;
    private Integer status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}
