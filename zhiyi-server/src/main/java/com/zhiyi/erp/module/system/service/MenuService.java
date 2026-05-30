package com.zhiyi.erp.module.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhiyi.erp.common.exception.BizException;
import com.zhiyi.erp.module.system.entity.Menu;
import com.zhiyi.erp.module.system.entity.RoleMenu;
import com.zhiyi.erp.module.system.mapper.MenuMapper;
import com.zhiyi.erp.module.system.mapper.RoleMenuMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class MenuService extends ServiceImpl<MenuMapper, Menu> {

    @Autowired
    private RoleMenuMapper roleMenuMapper;

    public List<Menu> listAll() {
        return list(new LambdaQueryWrapper<Menu>().orderByAsc(Menu::getSort));
    }

    public List<Map<String, Object>> getTree() {
        List<Menu> allMenus = listAll();
        return buildTree(allMenus, 0L);
    }

    private List<Map<String, Object>> buildTree(List<Menu> menus, Long parentId) {
        return menus.stream()
                .filter(m -> parentId.equals(m.getParentId()))
                .map(m -> {
                    Map<String, Object> node = new java.util.LinkedHashMap<>();
                    node.put("id", m.getId());
                    node.put("name", m.getName());
                    node.put("code", m.getCode());
                    node.put("path", m.getPath());
                    node.put("component", m.getComponent());
                    node.put("icon", m.getIcon());
                    node.put("type", m.getType());
                    node.put("platform", m.getPlatform());
                    node.put("permission", m.getPermission());
                    node.put("sort", m.getSort());
                    node.put("status", m.getStatus());
                    node.put("children", buildTree(menus, m.getId()));
                    return node;
                })
                .toList();
    }

    public void deleteMenu(Long id) {
        // 检查是否有子菜单
        long count = count(new LambdaQueryWrapper<Menu>().eq(Menu::getParentId, id));
        if (count > 0) {
            throw new BizException("该菜单下有子菜单，请先删除子菜单");
        }
        removeById(id);
    }

    public List<Menu> getMenusByRoleId(Long roleId) {
        List<RoleMenu> roleMenus = roleMenuMapper.selectList(
                new LambdaQueryWrapper<RoleMenu>().eq(RoleMenu::getRoleId, roleId));
        if (roleMenus.isEmpty()) {
            return List.of();
        }
        List<Long> menuIds = roleMenus.stream().map(RoleMenu::getMenuId).toList();
        return listByIds(menuIds);
    }

    public List<Map<String, Object>> getMenuTreeByRoleId(Long roleId) {
        List<Menu> menus = getMenusByRoleId(roleId);
        return buildTree(menus, 0L);
    }
}
