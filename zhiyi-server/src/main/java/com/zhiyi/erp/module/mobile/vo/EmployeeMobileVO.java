package com.zhiyi.erp.module.mobile.vo;

import lombok.Data;

@Data
public class EmployeeMobileVO {
    private Long id;
    private String employeeNo;
    private String name;
    private String phone;
    private Long roleId;
    private String roleName;
    private String loginAccount;
    private Integer status;
    private String remark;
}
