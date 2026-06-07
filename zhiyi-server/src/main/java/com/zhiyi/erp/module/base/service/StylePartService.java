package com.zhiyi.erp.module.base.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhiyi.erp.module.base.entity.StylePart;
import com.zhiyi.erp.module.base.mapper.StylePartMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StylePartService extends ServiceImpl<StylePartMapper, StylePart> {

    public List<StylePart> getByStyleId(Long styleId) {
        return list(new LambdaQueryWrapper<StylePart>()
                .eq(StylePart::getStyleId, styleId)
                .orderByAsc(StylePart::getSort));
    }

    public void saveStyleParts(Long styleId, List<StylePart> parts) {
        // 删除旧数据
        remove(new LambdaQueryWrapper<StylePart>().eq(StylePart::getStyleId, styleId));
        // 保存新数据
        if (parts != null) {
            for (int i = 0; i < parts.size(); i++) {
                StylePart part = parts.get(i);
                part.setId(null);
                part.setStyleId(styleId);
                part.setSort(i + 1);
                save(part);
            }
        }
    }
}
