package com.zhiyi.erp.module.admin.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class WagePieceDetailVO {
    private Long id;
    private Long employeeId;
    private LocalDateTime reportTime;
    private String orderNo;
    private String bundleNo;
    private String processName;
    private Integer qty;
    private BigDecimal price;
    private BigDecimal amount;
}
