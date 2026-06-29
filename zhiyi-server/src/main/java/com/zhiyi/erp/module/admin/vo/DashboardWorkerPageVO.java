package com.zhiyi.erp.module.admin.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class DashboardWorkerPageVO {
    private List<DashboardWorkerCapacityVO> records;
    private Integer totalQty;
    private BigDecimal totalAmount;
    private Long total;
    private Integer page;
    private Integer size;
}
