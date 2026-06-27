package com.zhiyi.erp.module.mobile.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class WorkerDetailVO {
    private EmployeeInfo employee;
    private Integer totalQty;
    private BigDecimal totalAmount;
    private List<OrderSummary> byOrder;
    private List<ProcessSummary> byProcess;
    private List<DailyGroup> dailyReports;

    @Data
    public static class EmployeeInfo {
        private Long id;
        private String employeeNo;
        private String employeeName;
    }

    @Data
    public static class OrderSummary {
        private Long orderId;
        private String orderNo;
        private String styleNo;
        private Integer qty;
        private BigDecimal amount;
    }

    @Data
    public static class ProcessSummary {
        private Long processId;
        private String processName;
        private Integer qty;
        private BigDecimal amount;
    }

    @Data
    public static class DailyGroup {
        private String date;
        private Integer qty;
        private BigDecimal amount;
        private List<ReportRecord> records;
    }

    @Data
    public static class ReportRecord {
        private Long id;
        private String orderNo;
        private String styleNo;
        private String processName;
        private String bundleNo;
        private Integer qty;
        private BigDecimal price;
        private BigDecimal amount;
    }
}
