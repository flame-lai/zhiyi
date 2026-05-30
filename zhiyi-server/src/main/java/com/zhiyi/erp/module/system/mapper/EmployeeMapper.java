package com.zhiyi.erp.module.system.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhiyi.erp.module.system.entity.Employee;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Map;

@Mapper
public interface EmployeeMapper extends BaseMapper<Employee> {

    @InterceptorIgnore(tenantLine = "1")
    @Select("SELECT * FROM employee WHERE deleted = 0 AND login_account = #{account} LIMIT 1")
    Employee selectByLoginAccount(String account);

    @InterceptorIgnore(tenantLine = "1")
    @Select("SELECT * FROM employee WHERE deleted = 0 AND login_code = #{loginCode} LIMIT 1")
    Employee selectByLoginCode(String loginCode);

    @Select("<script>" +
            "SELECT e.id, e.factory_id as factoryId, e.employee_no as employeeNo, " +
            "e.name, e.phone, e.role_id as roleId, r.name as roleName, " +
            "e.login_account as loginAccount, e.login_code as loginCode, " +
            "e.status, e.remark, e.created_at as createdAt, e.updated_at as updatedAt " +
            "FROM employee e " +
            "LEFT JOIN role r ON e.role_id = r.id " +
            "WHERE e.deleted = 0 AND e.factory_id = #{factoryId} " +
            "<if test='keyword != null and keyword != \"\"'>" +
            "AND (e.name LIKE CONCAT('%', #{keyword}, '%') OR e.employee_no LIKE CONCAT('%', #{keyword}, '%'))" +
            "</if>" +
            "ORDER BY e.created_at DESC" +
            "</script>")
    IPage<Map<String, Object>> selectPageWithRoleName(Page<?> page, @Param("factoryId") Long factoryId, @Param("keyword") String keyword);
}
