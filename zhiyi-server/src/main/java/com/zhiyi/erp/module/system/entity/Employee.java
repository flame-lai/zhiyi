package com.zhiyi.erp.module.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("employee")
public class Employee {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long factoryId;
    private String employeeNo;
    private String name;
    private String phone;
    private Long roleId;
    private String loginAccount;
    private String loginPassword;
    private String loginCode;
    private Integer status;
    private String remark;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}
