package com.zhiyi.erp.module.system.controller;

import com.zhiyi.erp.common.result.R;
import com.zhiyi.erp.module.system.entity.Menu;
import com.zhiyi.erp.module.system.service.MenuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/system/menus")
public class MenuController {

    @Autowired
    private MenuService menuService;

    @GetMapping
    public R<List<Menu>> list() {
        return R.ok(menuService.listAll());
    }

    @GetMapping("/tree")
    public R<List<Map<String, Object>>> tree() {
        return R.ok(menuService.getTree());
    }

    @GetMapping("/{id}")
    public R<Menu> getById(@PathVariable Long id) {
        return R.ok(menuService.getById(id));
    }

    @PostMapping
    public R<Void> create(@RequestBody Menu menu) {
        menuService.save(menu);
        return R.ok();
    }

    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @RequestBody Menu menu) {
        menu.setId(id);
        menuService.updateById(menu);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        menuService.deleteMenu(id);
        return R.ok();
    }
}
