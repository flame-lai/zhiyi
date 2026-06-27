package com.zhiyi.erp.module.mobile.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class DashboardWorkerSummaryVO {
    private List<DashboardWorkerVO> workers;
    private Integer totalQty;
    private BigDecimal totalAmount;
}
