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
    private String part;            // 部位（多部位用逗号分隔，如"袖子,前幅"）
    private String color;           // 颜色
    private String size;            // 尺码
    private Integer qtyPerBundle;   // 每扎件数
    private Integer ratio;          // 配比
    private Integer planQty;        // 计划成衣件数
    private Integer planPieces;     // 计划裁片片数
    private Integer actualQty;      // 实际成衣件数
    private Integer actualPieces;   // 实际裁片片数
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
