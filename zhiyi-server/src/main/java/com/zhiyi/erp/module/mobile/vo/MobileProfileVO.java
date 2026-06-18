package com.zhiyi.erp.module.mobile.vo;

import lombok.Data;
import java.util.List;

@Data
public class MobileProfileVO {
    private EmployeeInfo employee;
    private RoleInfo role;
    private List<MobileMenuVO> menus;
    private List<String> permissions;
    private String defaultHomeType;

    @Data
    public static class EmployeeInfo {
        private Long id;
        private String code;
        private String name;
    }

    @Data
    public static class RoleInfo {
        private Long id;
        private String name;
    }
}
