package com.zhiyi.erp.module.mobile.vo;

import lombok.Data;
import java.util.List;

@Data
public class ReportRecordsVO {
    private List<WorkReportRecordVO> records;
    private long total;
    private int page;
    private int size;
    private boolean hasMore;
}
