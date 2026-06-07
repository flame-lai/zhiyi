package com.zhiyi.erp.module.base.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("style_part")
public class StylePart {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long styleId;
    private String partName;
    private String fabric;
    private BigDecimal qtyPerPiece;
    private String remark;
    private Integer sort;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
