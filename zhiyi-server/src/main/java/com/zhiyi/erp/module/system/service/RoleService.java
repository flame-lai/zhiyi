package com.zhiyi.erp.module.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhiyi.erp.common.exception.BizException;
import com.zhiyi.erp.common.tenant.TenantContext;
import com.zhiyi.erp.module.system.entity.Employee;
import com.zhiyi.erp.module.system.entity.Role;
import com.zhiyi.erp.module.system.entity.RoleMenu;
import com.zhiyi.erp.module.system.mapper.EmployeeMapper;
import com.zhiyi.erp.module.system.mapper.RoleMapper;
import com.zhiyi.erp.module.system.mapper.RoleMenuMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class RoleService extends ServiceImpl<RoleMapper, Role> {

    @Autowired
    private RoleMapper roleMapper;

    @Autowired
    private RoleMenuMapper roleMenuMapper;

    @Autowired
    private EmployeeMapper employeeMapper;

    @Override
    public boolean save(Role role) {
        // 自动生成code
        if (role.getCode() == null || role.getCode().isEmpty()) {
            role.setCode("ROLE_" + UUID.randomUUID().toString().substring(0, 8));
        }
        return super.save(role);
    }

    public List<Role> listAll() {
        return listAll(null);
    }

    public List<Role> listAll(String keyword) {
        LambdaQueryWrapper<Role> wrapper = new LambdaQueryWrapper<Role>()
                .eq(Role::getFactoryId, TenantContext.get());
        String normalizedKeyword = keyword == null ? null : keyword.trim();
        if (normalizedKeyword != null && !normalizedKeyword.isEmpty()) {
            wrapper.like(Role::getName, normalizedKeyword);
        }
        wrapper.orderByAsc(Role::getSort);
        return list(wrapper);
    }

    public List<Map<String, Object>> listWithEmployeeCount() {
        return listWithEmployeeCount(null);
    }

    public List<Map<String, Object>> listWithEmployeeCount(String keyword) {
        List<Role> roles = listAll(keyword);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Role role : roles) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", role.getId());
            item.put("name", role.getName());
            item.put("description", role.getDescription());
            item.put("code", role.getCode());
            item.put("sort", role.getSort());
            item.put("status", role.getStatus());
            item.put("createdAt", role.getCreatedAt());
            // 统计该角色下的员工数
            long count = employeeMapper.selectCount(
                    new LambdaQueryWrapper<Employee>().eq(Employee::getRoleId, role.getId()));
            item.put("employeeCount", count);
            result.add(item);
        }
        return result;
    }

    @Transactional
    public void deleteRole(Long id) {
        Role role = getById(id);
        if (role == null) {
            throw new BizException("角色不存在");
        }
        // 检查是否有员工关联
        long count = employeeMapper.selectCount(
                new LambdaQueryWrapper<Employee>().eq(Employee::getRoleId, id));
        if (count > 0) {
            throw new BizException("该角色下有" + count + "名员工，请先转移员工");
        }
        removeById(id);
        // 删除角色菜单关联
        roleMenuMapper.delete(
                new LambdaQueryWrapper<RoleMenu>().eq(RoleMenu::getRoleId, id));
    }

    @Transactional
    public void assignMenus(Long roleId, List<Long> menuIds) {
        // 先删除原有权限
        roleMenuMapper.delete(
                new LambdaQueryWrapper<RoleMenu>().eq(RoleMenu::getRoleId, roleId));
        // 添加新权限
        for (Long menuId : menuIds) {
            RoleMenu rm = new RoleMenu();
            rm.setRoleId(roleId);
            rm.setMenuId(menuId);
            roleMenuMapper.insert(rm);
        }
    }

    public List<Long> getRoleMenuIds(Long roleId) {
        List<RoleMenu> list = roleMenuMapper.selectList(
                new LambdaQueryWrapper<RoleMenu>().eq(RoleMenu::getRoleId, roleId));
        return list.stream().map(RoleMenu::getMenuId).toList();
    }
}
