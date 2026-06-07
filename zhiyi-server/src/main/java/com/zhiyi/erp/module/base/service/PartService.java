package com.zhiyi.erp.module.base.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhiyi.erp.common.exception.BizException;
import com.zhiyi.erp.common.tenant.TenantContext;
import com.zhiyi.erp.module.base.entity.Part;
import com.zhiyi.erp.module.base.mapper.PartMapper;
import org.springframework.stereotype.Service;

@Service
public class PartService extends ServiceImpl<PartMapper, Part> {

    public Page<Part> page(Page<Part> page, String keyword) {
        LambdaQueryWrapper<Part> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Part::getFactoryId, TenantContext.get());
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(Part::getName, keyword);
        }
        wrapper.orderByAsc(Part::getSort);
        return page(page, wrapper);
    }

    @Override
    public boolean save(Part entity) {
        // 检查名称是否重复
        LambdaQueryWrapper<Part> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Part::getFactoryId, TenantContext.get());
        wrapper.eq(Part::getName, entity.getName());
        if (entity.getId() != null) {
            wrapper.ne(Part::getId, entity.getId());
        }
        if (count(wrapper) > 0) {
            throw new BizException("部位名称已存在");
        }
        entity.setFactoryId(TenantContext.get());
        return super.save(entity);
    }

    @Override
    public boolean updateById(Part entity) {
        // 检查名称是否重复
        LambdaQueryWrapper<Part> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Part::getFactoryId, TenantContext.get());
        wrapper.eq(Part::getName, entity.getName());
        wrapper.ne(Part::getId, entity.getId());
        if (count(wrapper) > 0) {
            throw new BizException("部位名称已存在");
        }
        return super.updateById(entity);
    }
}
