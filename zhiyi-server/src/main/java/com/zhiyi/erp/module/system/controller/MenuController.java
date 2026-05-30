package com.zhiyi.erp.module.system.controller;

import com.zhiyi.erp.common.result.R;
import com.zhiyi.erp.module.system.entity.Menu;
import com.zhiyi.erp.module.system.service.MenuService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "菜单管理", description = "菜单CRUD、树形结构")
@RestController
@RequestMapping("/api/system/menus")
public class MenuController {

    @Autowired
    private MenuService menuService;

    @Operation(summary = "查询所有菜单")
    @GetMapping
    public R<List<Menu>> list() {
        return R.ok(menuService.listAll());
    }

    @Operation(summary = "获取菜单树形结构")
    @GetMapping("/tree")
    public R<List<Map<String, Object>>> tree() {
        return R.ok(menuService.getTree());
    }

    @Operation(summary = "根据ID查询菜单详情")
    @GetMapping("/{id}")
    public R<Menu> getById(@Parameter(description = "菜单ID") @PathVariable Long id) {
        return R.ok(menuService.getById(id));
    }

    @Operation(summary = "创建菜单")
    @PostMapping
    public R<Void> create(@RequestBody Menu menu) {
        menuService.save(menu);
        return R.ok();
    }

    @Operation(summary = "更新菜单信息")
    @PutMapping("/{id}")
    public R<Void> update(@Parameter(description = "菜单ID") @PathVariable Long id, @RequestBody Menu menu) {
        menu.setId(id);
        menuService.updateById(menu);
        return R.ok();
    }

    @Operation(summary = "删除菜单")
    @DeleteMapping("/{id}")
    public R<Void> delete(@Parameter(description = "菜单ID") @PathVariable Long id) {
        menuService.deleteMenu(id);
        return R.ok();
    }
}
