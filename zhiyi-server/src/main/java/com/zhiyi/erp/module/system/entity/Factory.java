package com.zhiyi.erp.module.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("factory")
public class Factory {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String code;
    private Integer status;
    private String contact;
    private String phone;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
