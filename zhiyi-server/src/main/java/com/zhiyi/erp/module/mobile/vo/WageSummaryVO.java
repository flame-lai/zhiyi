package com.zhiyi.erp.module.mobile.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class WageSummaryVO {
    private BigDecimal todayAmount;
    private BigDecimal monthAmount;
    private List<DailyGroup> dailyGroups;

    @Data
    public static class DailyGroup {
        private String date;
        private BigDecimal amount;
        private List<WorkReportRecordVO> records;
    }
}
