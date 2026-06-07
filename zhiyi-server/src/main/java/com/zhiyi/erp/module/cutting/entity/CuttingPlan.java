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
    private Long orderId;
    private String fabric;          // 裁剪面料
    private String part;            // 裁剪部位，默认"全套"
    private Integer bedNo;          // 床次
    private Integer layers;         // 铺料层数
    private Integer planQty;        // 总计划数（各明细plan_qty之和）
    private Integer actualQty;      // 总实际数（各明细actual_qty之和）
    private Integer qtyPerBundle;   // 每扎件数
    private Integer bundleCount;    // 总扎数
    private LocalDate cuttingDate;  // 裁床日期
    private String barcode;         // 裁床条形码ID，如CUT202606050001
    private Integer status;         // 0=待裁剪, 1=已裁剪, 2=已开菲, 3=已报工
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}
