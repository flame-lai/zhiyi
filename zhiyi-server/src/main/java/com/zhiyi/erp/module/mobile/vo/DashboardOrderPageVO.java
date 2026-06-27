package com.zhiyi.erp.module.mobile.vo;

import lombok.Data;
import java.util.List;

@Data
public class DashboardOrderPageVO {
    private List<DashboardOrderVO> records;
    private long total;
    private int page;
    private int size;
    private boolean hasMore;
}
