package com.zhiyi.erp.module.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("role_menu")
public class RoleMenu {
    private Long roleId;
    private Long menuId;
}
