package com.zhiyi.erp.module.cutting.dto;

import lombok.Data;

@Data
public class CuttingProgressDTO {
    private String part;
    private String color;
    private String size;
    private Integer totalNeeded;   // 需裁总数
    private Integer alreadyCut;    // 已裁数量
    private Integer remaining;     // 待裁数量
}
