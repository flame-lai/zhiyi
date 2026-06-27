package com.zhiyi.erp.module.base.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
@TableName("style_process")
public class StyleProcess {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long styleId;
    private Long processId;
    private BigDecimal refPrice;
    private Integer sort;
    private Integer batchReport;
}
