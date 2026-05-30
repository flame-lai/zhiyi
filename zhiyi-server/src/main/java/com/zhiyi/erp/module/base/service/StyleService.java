package com.zhiyi.erp.module.base.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhiyi.erp.common.exception.BizException;
import com.zhiyi.erp.common.tenant.TenantContext;
import com.zhiyi.erp.module.base.entity.Style;
import com.zhiyi.erp.module.base.entity.StyleMaterial;
import com.zhiyi.erp.module.base.entity.StyleProcess;
import com.zhiyi.erp.module.base.mapper.StyleMapper;
import com.zhiyi.erp.module.base.mapper.StyleMaterialMapper;
import com.zhiyi.erp.module.base.mapper.StyleProcessMapper;
import com.zhiyi.erp.module.order.entity.OrderItem;
import com.zhiyi.erp.module.order.mapper.OrderItemMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class StyleService extends ServiceImpl<StyleMapper, Style> {

    @Autowired
    private StyleProcessMapper styleProcessMapper;
    @Autowired
    private StyleMaterialMapper styleMaterialMapper;
    @Autowired
    private OrderItemMapper orderItemMapper;

    public Page<Style> page(Page<Style> page, String keyword) {
        LambdaQueryWrapper<Style> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Style::getFactoryId, TenantContext.get());
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(Style::getStyleCode, keyword).or().like(Style::getStyleName, keyword));
        }
        wrapper.orderByDesc(Style::getCreatedAt);
        return page(page, wrapper);
    }

    @Override
    public boolean save(Style entity) {
        // 校验款号唯一性
        Long count = count(
                new LambdaQueryWrapper<Style>()
                        .eq(Style::getFactoryId, TenantContext.get())
                        .eq(Style::getStyleCode, entity.getStyleCode()));
        if (count > 0) {
            throw new BizException("款号已存在，请使用其他款号");
        }
        return super.save(entity);
    }

    @Override
    public boolean removeById(java.io.Serializable id) {
        // 检查款号是否被订单使用
        Long count = orderItemMapper.selectCount(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getStyleId, id));
        if (count > 0) {
            throw new BizException("该款号已被订单使用，无法删除");
        }
        return super.removeById(id);
    }

    public List<StyleProcess> getStyleProcesses(Long styleId) {
        return styleProcessMapper.selectList(
                new LambdaQueryWrapper<StyleProcess>()
                        .eq(StyleProcess::getStyleId, styleId)
                        .orderByAsc(StyleProcess::getSort));
    }

    public List<StyleMaterial> getStyleMaterials(Long styleId) {
        return styleMaterialMapper.selectList(
                new LambdaQueryWrapper<StyleMaterial>()
                        .eq(StyleMaterial::getStyleId, styleId));
    }

    @Transactional
    public void saveStyleProcesses(Long styleId, List<StyleProcess> processes) {
        styleProcessMapper.delete(
                new LambdaQueryWrapper<StyleProcess>().eq(StyleProcess::getStyleId, styleId));
        for (int i = 0; i < processes.size(); i++) {
            StyleProcess sp = processes.get(i);
            sp.setStyleId(styleId);
            sp.setSort(i + 1);
            styleProcessMapper.insert(sp);
        }
    }

    @Transactional
    public void saveStyleMaterials(Long styleId, List<StyleMaterial> materials) {
        styleMaterialMapper.delete(
                new LambdaQueryWrapper<StyleMaterial>().eq(StyleMaterial::getStyleId, styleId));
        for (StyleMaterial sm : materials) {
            sm.setStyleId(styleId);
            styleMaterialMapper.insert(sm);
        }
    }
}
