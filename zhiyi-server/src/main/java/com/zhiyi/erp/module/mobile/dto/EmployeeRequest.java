package com.zhiyi.erp.module.mobile.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class EmployeeRequest {
    @NotBlank(message = "姓名不能为空")
    private String name;

    private String phone;

    @NotNull(message = "角色不能为空")
    private Long roleId;

    private String loginAccount;
    private String loginPassword;
    private Integer status;
    private String remark;
}
