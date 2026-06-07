package com.zhiyi.erp.module.cutting.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhiyi.erp.common.exception.BizException;
import com.zhiyi.erp.common.tenant.TenantContext;
import com.zhiyi.erp.module.cutting.dto.BundleDetailDTO;
import com.zhiyi.erp.module.cutting.dto.CuttingPlanDetailDTO;
import com.zhiyi.erp.module.cutting.entity.Bundle;
import com.zhiyi.erp.module.cutting.entity.CuttingPlan;
import com.zhiyi.erp.module.cutting.entity.CuttingPlanDetail;
import com.zhiyi.erp.module.cutting.mapper.BundleMapper;
import com.zhiyi.erp.module.cutting.mapper.CuttingPlanDetailMapper;
import com.zhiyi.erp.module.cutting.mapper.CuttingPlanMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class CuttingService extends ServiceImpl<CuttingPlanMapper, CuttingPlan> {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Autowired
    private CuttingPlanMapper cuttingPlanMapper;
    @Autowired
    private CuttingPlanDetailMapper cuttingPlanDetailMapper;
    @Autowired
    private BundleMapper bundleMapper;

    public Page<CuttingPlanDetailDTO> page(Page<CuttingPlanDetailDTO> page, String orderNo, String styleCode, Integer status) {
        List<CuttingPlanDetailDTO> records = cuttingPlanMapper.selectCuttingPlanDetailsPage(
                page, TenantContext.get(), orderNo, styleCode, status);
        page.setRecords(records);
        return page;
    }

    public CuttingPlanDetailDTO getPlanDetailById(Long id) {
        return cuttingPlanMapper.selectCuttingPlanDetailById(id);
    }

    /**
     * 获取裁床计划的明细列表（配比矩阵）
     */
    public List<CuttingPlanDetail> getPlanDetails(Long cuttingPlanId) {
        return cuttingPlanDetailMapper.selectList(
                new LambdaQueryWrapper<CuttingPlanDetail>()
                        .eq(CuttingPlanDetail::getCuttingPlanId, cuttingPlanId));
    }

    /**
     * 保存裁床计划（含配比矩阵明细）
     */
    @Transactional
    public Long savePlanWithDetails(CuttingPlan plan, List<CuttingPlanDetail> details) {
        if (details == null || details.isEmpty()) {
            throw new BizException("请添加颜色尺码配比明细");
        }

        plan.setFactoryId(TenantContext.get());

        // 计算总计划数
        int totalPlanQty = 0;
        for (CuttingPlanDetail detail : details) {
            detail.setPlanQty(plan.getLayers() * detail.getRatio());
            totalPlanQty += detail.getPlanQty();
        }
        plan.setPlanQty(totalPlanQty);
        plan.setActualQty(0);
        plan.setBundleCount(0);

        // 生成裁床条形码ID
        if (plan.getBarcode() == null || plan.getBarcode().isEmpty()) {
            plan.setBarcode(generateCuttingBarcode());
        }

        // 保存主表
        if (plan.getId() != null) {
            updateById(plan);
            // 删除旧明细
            cuttingPlanDetailMapper.delete(
                    new LambdaQueryWrapper<CuttingPlanDetail>()
                            .eq(CuttingPlanDetail::getCuttingPlanId, plan.getId()));
        } else {
            plan.setStatus(0);
            save(plan);
        }

        // 保存明细
        for (CuttingPlanDetail detail : details) {
            detail.setId(null);
            detail.setCuttingPlanId(plan.getId());
            cuttingPlanDetailMapper.insert(detail);
        }

        return plan.getId();
    }

    /**
     * 录入实际件数（按颜色尺码明细录入）
     */
    @Transactional
    public void inputActualQty(Long planId, List<CuttingPlanDetail> details) {
        CuttingPlan plan = getById(planId);
        if (plan == null) throw new BizException("裁床计划不存在");

        int totalActualQty = 0;
        for (CuttingPlanDetail detail : details) {
            CuttingPlanDetail existing = cuttingPlanDetailMapper.selectById(detail.getId());
            if (existing == null) throw new BizException("明细记录不存在");
            existing.setActualQty(detail.getActualQty());
            cuttingPlanDetailMapper.updateById(existing);
            totalActualQty += detail.getActualQty();
        }

        plan.setActualQty(totalActualQty);
        updateById(plan);
    }

    /**
     * 开菲：按每个颜色尺码独立分扎
     */
    @Transactional
    public List<Bundle> openBundle(Long planId) {
        CuttingPlan plan = getById(planId);
        if (plan == null) throw new BizException("裁床计划不存在");
        if (plan.getStatus() >= 2) throw new BizException("已开菲");

        // 获取明细
        List<CuttingPlanDetail> details = getPlanDetails(planId);
        if (details.isEmpty()) throw new BizException("没有颜色尺码明细");

        // 检查是否已录入实际数
        boolean hasActual = details.stream().anyMatch(d -> d.getActualQty() != null && d.getActualQty() > 0);
        if (!hasActual) throw new BizException("请先录入实际件数");

        int qtyPerBundle = plan.getQtyPerBundle();
        int totalBundleCount = 0;
        List<Bundle> allBundles = new ArrayList<>();

        // 按每个颜色尺码独立分扎
        for (CuttingPlanDetail detail : details) {
            if (detail.getActualQty() == null || detail.getActualQty() <= 0) continue;

            int actualQty = detail.getActualQty();
            int bundleCount = (int) Math.ceil((double) actualQty / qtyPerBundle);

            for (int i = 1; i <= bundleCount; i++) {
                int qty = (i < bundleCount) ? qtyPerBundle : (actualQty - (bundleCount - 1) * qtyPerBundle);
                totalBundleCount++;

                Bundle bundle = new Bundle();
                bundle.setFactoryId(TenantContext.get());
                bundle.setCuttingPlanId(planId);
                bundle.setOrderId(plan.getOrderId());
                bundle.setColor(detail.getColor());
                bundle.setSize(detail.getSize());
                bundle.setFabric(plan.getFabric());
                bundle.setPart(plan.getPart());
                bundle.setBundleNo(plan.getBedNo() + "-" + String.format("%03d", totalBundleCount));
                bundle.setGlobalBundleNo(generateBundleBarcode());
                bundle.setQty(qty);
                bundle.setStatus(0);
                bundleMapper.insert(bundle);
                allBundles.add(bundle);
            }
        }

        // 更新裁床计划状态
        plan.setBundleCount(totalBundleCount);
        plan.setStatus(2);
        updateById(plan);

        return allBundles;
    }

    public List<Bundle> getBundles(Long cuttingPlanId) {
        return bundleMapper.selectList(
                new LambdaQueryWrapper<Bundle>()
                        .eq(Bundle::getCuttingPlanId, cuttingPlanId)
                        .orderByAsc(Bundle::getBundleNo));
    }

    public Page<BundleDetailDTO> pageBundles(Page<BundleDetailDTO> page, String orderNo, String styleCode, String color, String size, Integer status) {
        List<BundleDetailDTO> records = bundleMapper.selectBundleDetailsPage(
                page, TenantContext.get(), orderNo, styleCode, color, size, status);
        page.setRecords(records);
        return page;
    }

    public BundleDetailDTO getBundleDetailById(Long id) {
        return bundleMapper.selectBundleDetailById(id);
    }

    public List<Bundle> getBundlesByOrderId(Long orderId) {
        return bundleMapper.selectList(
                new LambdaQueryWrapper<Bundle>()
                        .eq(Bundle::getOrderId, orderId)
                        .orderByAsc(Bundle::getBundleNo));
    }

    /**
     * 生成裁床条形码ID：CUT + 年月日 + 4位流水号
     */
    private String generateCuttingBarcode() {
        String today = LocalDate.now().format(DATE_FMT);
        String prefix = "CUT" + today;
        // 查询当天最大编号
        LambdaQueryWrapper<CuttingPlan> wrapper = new LambdaQueryWrapper<>();
        wrapper.likeRight(CuttingPlan::getBarcode, prefix);
        wrapper.orderByDesc(CuttingPlan::getBarcode);
        wrapper.last("LIMIT 1");
        CuttingPlan last = cuttingPlanMapper.selectOne(wrapper);
        int nextSeq = 1;
        if (last != null && last.getBarcode() != null) {
            String lastNo = last.getBarcode();
            if (lastNo.startsWith(prefix) && lastNo.length() == prefix.length() + 4) {
                try {
                    nextSeq = Integer.parseInt(lastNo.substring(prefix.length())) + 1;
                } catch (NumberFormatException e) {}
            }
        }
        return String.format("%s%04d", prefix, nextSeq);
    }

    /**
     * 生成扎号码ID：BUN + 年月日 + 5位流水号
     */
    private String generateBundleBarcode() {
        String today = LocalDate.now().format(DATE_FMT);
        String prefix = "BUN" + today;
        // 查询当天最大编号
        LambdaQueryWrapper<Bundle> wrapper = new LambdaQueryWrapper<>();
        wrapper.likeRight(Bundle::getGlobalBundleNo, prefix);
        wrapper.orderByDesc(Bundle::getGlobalBundleNo);
        wrapper.last("LIMIT 1");
        Bundle last = bundleMapper.selectOne(wrapper);
        int nextSeq = 1;
        if (last != null && last.getGlobalBundleNo() != null) {
            String lastNo = last.getGlobalBundleNo();
            if (lastNo.startsWith(prefix) && lastNo.length() == prefix.length() + 5) {
                try {
                    nextSeq = Integer.parseInt(lastNo.substring(prefix.length())) + 1;
                } catch (NumberFormatException e) {}
            }
        }
        return String.format("%s%05d", prefix, nextSeq);
    }
}
