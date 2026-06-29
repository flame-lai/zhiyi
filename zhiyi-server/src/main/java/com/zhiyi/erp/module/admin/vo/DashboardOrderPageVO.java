package com.zhiyi.erp.module.admin.vo;

import lombok.Data;

import java.util.List;

@Data
public class DashboardOrderPageVO {
    private List<DashboardOrderProgressVO> records;
    private Long total;
    private Integer page;
    private Integer size;
}
