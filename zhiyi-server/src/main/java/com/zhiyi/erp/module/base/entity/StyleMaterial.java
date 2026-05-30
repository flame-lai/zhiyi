package com.zhiyi.erp.module.base.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
@TableName("style_material")
public class StyleMaterial {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long styleId;
    private String materialName;
    private Integer type;
    private BigDecimal unitQty;
    private String unit;
}
