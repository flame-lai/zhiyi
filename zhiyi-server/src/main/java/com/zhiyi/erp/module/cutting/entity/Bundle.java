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
    private Long orderItemId;
    private String bundleNo;
    private String globalBundleNo;
    private Integer qty;
    private Integer status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
