package com.zhiyi.erp.module.system.controller;

import com.zhiyi.erp.common.result.R;
import com.zhiyi.erp.module.system.entity.Role;
import com.zhiyi.erp.module.system.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "角色管理", description = "角色CRUD、菜单分配")
@RestController
@RequestMapping("/api/system/roles")
public class RoleController {

    @Autowired
    private RoleService roleService;

    @Operation(summary = "查询所有角色")
    @GetMapping
    public R<List<Map<String, Object>>> list(
            @Parameter(description = "角色名称关键词")
            @RequestParam(required = false) String keyword) {
        return R.ok(roleService.listWithEmployeeCount(keyword));
    }

    @Operation(summary = "根据ID查询角色详情")
    @GetMapping("/{id}")
    public R<Role> getById(@Parameter(description = "角色ID") @PathVariable Long id) {
        return R.ok(roleService.getById(id));
    }

    @Operation(summary = "创建角色")
    @PostMapping
    public R<Void> create(@RequestBody Role role) {
        roleService.save(role);
        return R.ok();
    }

    @Operation(summary = "更新角色信息")
    @PutMapping("/{id}")
    public R<Void> update(@Parameter(description = "角色ID") @PathVariable Long id, @RequestBody Role role) {
        role.setId(id);
        roleService.updateById(role);
        return R.ok();
    }

    @Operation(summary = "删除角色")
    @DeleteMapping("/{id}")
    public R<Void> delete(@Parameter(description = "角色ID") @PathVariable Long id) {
        roleService.deleteRole(id);
        return R.ok();
    }

    @Operation(summary = "查询角色关联菜单ID列表")
    @GetMapping("/{id}/menus")
    public R<List<Long>> getRoleMenus(@Parameter(description = "角色ID") @PathVariable Long id) {
        return R.ok(roleService.getRoleMenuIds(id));
    }

    @Operation(summary = "为角色分配菜单")
    @PostMapping("/{id}/menus")
    public R<Void> assignMenus(@Parameter(description = "角色ID") @PathVariable Long id, @RequestBody List<Long> menuIds) {
        roleService.assignMenus(id, menuIds);
        return R.ok();
    }
}
