package com.zhiyi.erp.module.cutting.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("cutting_plan_detail")
public class CuttingPlanDetail {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long cuttingPlanId;     // 裁床计划ID
    private String color;           // 颜色
    private String size;            // 尺码
    private Integer ratio;          // 配比
    private Integer planQty;        // 计划件数=层数×配比
    private Integer actualQty;      // 实际件数
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
