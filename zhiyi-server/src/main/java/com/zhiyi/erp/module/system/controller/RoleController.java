package com.zhiyi.erp.module.system.controller;

import com.zhiyi.erp.common.result.R;
import com.zhiyi.erp.module.system.entity.Role;
import com.zhiyi.erp.module.system.service.RoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/system/roles")
public class RoleController {

    @Autowired
    private RoleService roleService;

    @GetMapping
    public R<List<Role>> list() {
        return R.ok(roleService.listAll());
    }

    @GetMapping("/{id}")
    public R<Role> getById(@PathVariable Long id) {
        return R.ok(roleService.getById(id));
    }

    @PostMapping
    public R<Void> create(@RequestBody Role role) {
        roleService.save(role);
        return R.ok();
    }

    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @RequestBody Role role) {
        role.setId(id);
        roleService.updateById(role);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        roleService.deleteRole(id);
        return R.ok();
    }

    @GetMapping("/{id}/menus")
    public R<List<Long>> getRoleMenus(@PathVariable Long id) {
        return R.ok(roleService.getRoleMenuIds(id));
    }

    @PostMapping("/{id}/menus")
    public R<Void> assignMenus(@PathVariable Long id, @RequestBody List<Long> menuIds) {
        roleService.assignMenus(id, menuIds);
        return R.ok();
    }
}
