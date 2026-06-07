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
        checkNameDuplicate(entity.getName(), entity.getId());
        entity.setFactoryId(TenantContext.get());
        return super.save(entity);
    }

    @Override
    public boolean updateById(Part entity) {
        checkNameDuplicate(entity.getName(), entity.getId());
        return super.updateById(entity);
    }

    private void checkNameDuplicate(String name, Long excludeId) {
        LambdaQueryWrapper<Part> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Part::getFactoryId, TenantContext.get());
        wrapper.eq(Part::getName, name);
        wrapper.eq(Part::getDeleted, 0);
        if (excludeId != null) {
            wrapper.ne(Part::getId, excludeId);
        }
        if (count(wrapper) > 0) {
            throw new BizException("部位名称已存在");
        }
    }
}
