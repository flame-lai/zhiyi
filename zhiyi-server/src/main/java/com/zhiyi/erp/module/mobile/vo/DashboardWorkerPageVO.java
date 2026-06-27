package com.zhiyi.erp.module.mobile.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class DashboardWorkerPageVO {
    private List<DashboardWorkerVO> workers;
    private Integer totalQty;
    private BigDecimal totalAmount;
    private long total;
    private int page;
    private int size;
    private boolean hasMore;
}
