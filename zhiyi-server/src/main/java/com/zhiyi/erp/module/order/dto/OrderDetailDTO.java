package com.zhiyi.erp.module.order.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class OrderDetailDTO {
    private Long id;
    private Long factoryId;
    private String orderNo;
    private Long customerId;
    private String customerName;
    private Long styleId;
    private String styleCode;
    private String styleName;
    private BigDecimal totalAmount;
    private Integer totalQty;
    private LocalDate deliveryDate;
    private Integer status;
    private String remark;
    private LocalDateTime createdAt;
}
