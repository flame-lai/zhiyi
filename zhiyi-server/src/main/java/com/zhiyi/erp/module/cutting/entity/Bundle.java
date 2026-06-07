package com.zhiyi.erp.module.cutting.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("bundle")
public class Bundle {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long factoryId;
    private Long cuttingPlanId;
    private Long orderId;
    private String color;
    private String size;
    private String fabric;          // 面料
    private String part;            // 部位
    private String bundleNo;        // 扎号，如1-001
    private String globalBundleNo;  // 全局唯一ID，如BUN2026060500045
    private Integer qty;
    private Integer status;         // 0=生产中, 1=已完成
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
