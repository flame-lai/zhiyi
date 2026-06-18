package com.zhiyi.erp.module.mobile.vo;

import lombok.Data;

@Data
public class MobileMenuVO {
    private String code;
    private String title;
    private String path;
    private String group;  // employee / admin
    private String icon;
    private Integer sort;
}
