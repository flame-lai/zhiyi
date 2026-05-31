package com.zhiyi.erp.module.base.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhiyi.erp.common.exception.BizException;
import com.zhiyi.erp.common.tenant.TenantContext;
import com.zhiyi.erp.module.base.entity.Material;
import com.zhiyi.erp.module.base.mapper.MaterialMapper;
import com.zhiyi.erp.module.order.entity.OrderMaterial;
import com.zhiyi.erp.module.order.mapper.OrderMaterialMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MaterialService extends ServiceImpl<MaterialMapper, Material> {

    @Autowired
    private OrderMaterialMapper orderMaterialMapper;

    public Page<Material> page(Page<Material> page, String keyword, Integer type) {
        LambdaQueryWrapper<Material> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Material::getFactoryId, TenantContext.get());
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(Material::getMaterialName, keyword);
        }
        if (type != null) {
            wrapper.eq(Material::getType, type);
        }
        wrapper.orderByDesc(Material::getCreatedAt);
        return page(page, wrapper);
    }

    @Override
    public boolean save(Material entity) {
        // 自动生成物料编号
        if (entity.getMaterialCode() == null || entity.getMaterialCode().isEmpty()) {
            entity.setMaterialCode(generateCode());
        }
        // 设置工厂ID
        entity.setFactoryId(TenantContext.get());
        return super.save(entity);
    }

    @Override
    public boolean removeById(java.io.Serializable id) {
        // 检查物料是否被订单使用（通过物料名称匹配）
        Material material = getById(id);
        if (material != null) {
            Long count = orderMaterialMapper.selectCount(
                    new LambdaQueryWrapper<OrderMaterial>().eq(OrderMaterial::getMaterialName, material.getMaterialName()));
            if (count > 0) {
                throw new BizException("该物料已被订单使用，无法删除");
            }
        }
        return super.removeById(id);
    }

    private String generateCode() {
        // 查询当前最大编号
        Long factoryId = TenantContext.get();
        LambdaQueryWrapper<Material> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Material::getFactoryId, factoryId);
        wrapper.likeRight(Material::getMaterialCode, "M");
        wrapper.orderByDesc(Material::getMaterialCode);
        wrapper.last("LIMIT 1");
        Material last = getOne(wrapper);

        int nextNum = 1;
        if (last != null && last.getMaterialCode() != null) {
            String lastCode = last.getMaterialCode();
            if (lastCode.startsWith("M")) {
                try {
                    nextNum = Integer.parseInt(lastCode.substring(1)) + 1;
                } catch (NumberFormatException e) {
                    // ignore
                }
            }
        }
        return String.format("M%03d", nextNum);
    }
}
