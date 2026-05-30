package com.zhiyi.erp.module.base.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("process")
public class Process {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long factoryId;
    private String category;
    private String name;
    private BigDecimal refPrice;
    private Integer sort;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}
