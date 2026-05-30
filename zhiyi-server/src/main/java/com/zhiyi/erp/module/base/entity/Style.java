package com.zhiyi.erp.module.base.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("style")
public class Style {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long factoryId;
    private String styleCode;
    private String styleName;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}
