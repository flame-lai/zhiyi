package com.zhiyi.erp.module.cutting.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhiyi.erp.common.exception.BizException;
import com.zhiyi.erp.common.tenant.TenantContext;
import com.zhiyi.erp.module.base.entity.StylePart;
import com.zhiyi.erp.module.base.mapper.StyleMapper;
import com.zhiyi.erp.module.base.service.StylePartService;
import com.zhiyi.erp.module.cutting.dto.BundleDetailDTO;
import com.zhiyi.erp.module.cutting.dto.BundleWorkDetailDTO;
import com.zhiyi.erp.module.cutting.dto.CuttingPlanDetailDTO;
import com.zhiyi.erp.module.cutting.dto.CuttingProgressDTO;
import com.zhiyi.erp.module.cutting.entity.Bundle;
import com.zhiyi.erp.module.cutting.entity.CuttingPlan;
import com.zhiyi.erp.module.cutting.entity.CuttingPlanDetail;
import com.zhiyi.erp.module.cutting.mapper.BundleMapper;
import com.zhiyi.erp.module.cutting.mapper.CuttingPlanDetailMapper;
import com.zhiyi.erp.module.cutting.mapper.CuttingPlanMapper;
import com.zhiyi.erp.module.order.entity.Order;
import com.zhiyi.erp.module.order.entity.OrderColorSize;
import com.zhiyi.erp.module.order.mapper.OrderColorSizeMapper;
import com.zhiyi.erp.module.order.mapper.OrderMapper;
import com.zhiyi.erp.module.base.entity.Style;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CuttingService extends ServiceImpl<CuttingPlanMapper, CuttingPlan> {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Autowired
    private CuttingPlanMapper cuttingPlanMapper;
    @Autowired
    private CuttingPlanDetailMapper cuttingPlanDetailMapper;
    @Autowired
    private BundleMapper bundleMapper;
    @Autowired
    private StylePartService stylePartService;
    @Autowired
    private OrderColorSizeMapper orderColorSizeMapper;
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private StyleMapper styleMapper;

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

        // 计算总计划数（使用每行明细的planQty，如果没有则按层数×配比计算）
        int totalPlanQty = 0;
        for (CuttingPlanDetail detail : details) {
            if (detail.getPlanQty() == null || detail.getPlanQty() <= 0) {
                detail.setPlanQty(plan.getLayers() * detail.getRatio());
            }
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
    public void inputActualQty(Long planId, List<CuttingPlanDetail> details, String cuttingDate) {
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
        // 更新裁床日期（如果提供了新日期）
        if (cuttingDate != null && !cuttingDate.isEmpty()) {
            plan.setCuttingDate(LocalDate.parse(cuttingDate));
        }
        updateById(plan);
    }

    /**
     * 开菲：按部位+颜色+尺码独立分扎，一扎多部位时按部位分别生成菲仔
     */
    @Transactional
    public List<Bundle> openBundle(Long planId) {
        CuttingPlan plan = getById(planId);
        if (plan == null) throw new BizException("裁床计划不存在");
        if (plan.getStatus() >= 2) throw new BizException("已开菲");

        // 获取明细
        List<CuttingPlanDetail> details = getPlanDetails(planId);
        if (details.isEmpty()) throw new BizException("没有配比明细");

        // 检查是否已录入实际数
        boolean hasActual = details.stream().anyMatch(d -> d.getActualQty() != null && d.getActualQty() > 0);
        if (!hasActual) throw new BizException("请先录入实际件数");

        int physicalBundleCount = 0;
        List<Bundle> allBundles = new ArrayList<>();

        // 获取款号部位配置（用于计算裁片片数）
        Order order = orderMapper.selectById(plan.getOrderId());
        Long styleId = order != null ? order.getStyleId() : null;
        List<StylePart> styleParts = styleId != null ? stylePartService.getByStyleId(styleId) : new ArrayList<>();

        // 按配比明细独立分扎，一扎多部位时按部位分别生成菲仔
        for (CuttingPlanDetail detail : details) {
            if (detail.getActualQty() == null || detail.getActualQty() <= 0) continue;

            int qtyPerBundle = detail.getQtyPerBundle() != null ? detail.getQtyPerBundle() : detail.getActualQty();
            int actualQty = detail.getActualQty();

            // 解析部位列表
            String[] partNames = detail.getPart() != null ? detail.getPart().split(",") : new String[]{""};

            // 先计算物理扎数（同色同码的扎数，不含部位拆分）
            int physicalZhaCount = (int) Math.ceil((double) actualQty / qtyPerBundle);

            // 为每个部位分别生成 bundle 记录
            for (String partName : partNames) {
                String trimmedPart = partName.trim();
                int piecesPerPart = calcPiecesForSinglePart(trimmedPart, styleParts);

                for (int i = 1; i <= physicalZhaCount; i++) {
                    int qty = (i < physicalZhaCount) ? qtyPerBundle : (actualQty - (physicalZhaCount - 1) * qtyPerBundle);

                    // 物理扎号只在第一个部位时递增
                    if (partNames.length > 0 && partName.equals(partNames[0])) {
                        physicalBundleCount++;
                    }

                    Bundle bundle = new Bundle();
                    bundle.setFactoryId(TenantContext.get());
                    bundle.setCuttingPlanId(planId);
                    bundle.setOrderId(plan.getOrderId());
                    bundle.setPart(trimmedPart);          // 单个部位
                    bundle.setColor(detail.getColor());
                    bundle.setSize(detail.getSize());
                    bundle.setFabric(plan.getFabric());
                    bundle.setBundleNo(plan.getBedNo() + "-" + String.format("%03d", physicalBundleCount));
                    bundle.setGlobalBundleNo(generateBundleBarcode());  // 每条独立，用于二维码
                    bundle.setQty(qty);
                    bundle.setCuttingPieces(qty * piecesPerPart);  // 按部位单独计算裁片片数
                    bundle.setCuttingDate(plan.getCuttingDate() != null ? plan.getCuttingDate() : LocalDate.now());
                    bundleMapper.insert(bundle);
                    allBundles.add(bundle);
                }
            }
        }

        // 更新裁床计划状态，bundleCount 为物理扎数
        plan.setBundleCount(physicalBundleCount);
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

    /**
     * 查询裁床计划的扎号详情（含订单号、款号）
     */
    public List<BundleDetailDTO> getBundleDetails(Long cuttingPlanId) {
        LambdaQueryWrapper<Bundle> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Bundle::getCuttingPlanId, cuttingPlanId);
        wrapper.orderByAsc(Bundle::getBundleNo);
        List<Bundle> bundles = bundleMapper.selectList(wrapper);

        if (bundles.isEmpty()) {
            return new ArrayList<>();
        }

        // 获取订单信息
        Long orderId = bundles.get(0).getOrderId();
        Order order = orderMapper.selectById(orderId);
        String orderNo = order != null ? order.getOrderNo() : "";
        Long styleId = order != null ? order.getStyleId() : null;

        // 获取款号信息
        Style style = styleId != null ? styleMapper.selectById(styleId) : null;
        String styleCode = style != null ? style.getStyleCode() : "";
        String styleName = style != null ? style.getStyleName() : "";

        // 获取裁床计划信息（获取床次）
        CuttingPlan cuttingPlan = cuttingPlanMapper.selectById(cuttingPlanId);
        Integer bedNo = cuttingPlan != null ? cuttingPlan.getBedNo() : null;

        // 转换为BundleDetailDTO
        List<BundleDetailDTO> result = new ArrayList<>();
        for (Bundle bundle : bundles) {
            BundleDetailDTO dto = new BundleDetailDTO();
            dto.setId(bundle.getId());
            dto.setFactoryId(bundle.getFactoryId());
            dto.setCuttingPlanId(bundle.getCuttingPlanId());
            dto.setOrderId(bundle.getOrderId());
            dto.setOrderNo(orderNo);
            dto.setStyleId(styleId);
            dto.setStyleCode(styleCode);
            dto.setStyleName(styleName);
            dto.setColor(bundle.getColor());
            dto.setSize(bundle.getSize());
            dto.setFabric(bundle.getFabric());
            dto.setPart(bundle.getPart());
            dto.setBedNo(bedNo);
            dto.setBundleNo(bundle.getBundleNo());
            dto.setGlobalBundleNo(bundle.getGlobalBundleNo());
            dto.setQty(bundle.getQty());
            dto.setCuttingPieces(bundle.getCuttingPieces());
            dto.setCreatedAt(bundle.getCreatedAt());
            result.add(dto);
        }
        return result;
    }

    public Page<BundleDetailDTO> pageBundles(Page<BundleDetailDTO> page, String bundleNo, String orderNo, String styleCode, String color, String size) {
        List<BundleDetailDTO> records = bundleMapper.selectBundleDetailsPage(
                page, TenantContext.get(), bundleNo, orderNo, styleCode, color, size);
        page.setRecords(records);
        return page;
    }

    public BundleDetailDTO getBundleDetailById(Long id) {
        return bundleMapper.selectBundleDetailById(id);
    }

    /**
     * 查询扎号的工序完成明细
     */
    public List<BundleWorkDetailDTO> getBundleWorkDetails(Long bundleId) {
        Bundle bundle = bundleMapper.selectById(bundleId);
        if (bundle == null) return new ArrayList<>();
        return bundleMapper.selectBundleWorkDetails(bundleId, bundle.getOrderId());
    }

    public List<Bundle> getBundlesByOrderId(Long orderId) {
        return bundleMapper.selectList(
                new LambdaQueryWrapper<Bundle>()
                        .eq(Bundle::getOrderId, orderId)
                        .orderByAsc(Bundle::getBundleNo));
    }

    /**
     * 查询订单的裁剪进度
     */
    public List<CuttingProgressDTO> getCuttingProgress(Long orderId, Long styleId) {
        // 1. 获取款号部位BOM
        List<StylePart> styleParts = stylePartService.getByStyleId(styleId);
        if (styleParts.isEmpty()) {
            return new ArrayList<>();
        }

        // 2. 获取订单的颜色尺码明细（获取订单总件数）
        List<OrderColorSize> orderColorSizes = orderColorSizeMapper.selectList(
                new LambdaQueryWrapper<OrderColorSize>().eq(OrderColorSize::getOrderId, orderId));

        // 3. 获取该订单所有裁床计划的明细（已裁数量）
        List<CuttingPlan> plans = cuttingPlanMapper.selectList(
                new LambdaQueryWrapper<CuttingPlan>()
                        .eq(CuttingPlan::getOrderId, orderId)
                        .eq(CuttingPlan::getDeleted, 0));
        Set<Long> planIds = plans.stream().map(CuttingPlan::getId).collect(Collectors.toSet());

        Map<String, Integer> alreadyCutMap = new HashMap<>();
        if (!planIds.isEmpty()) {
            List<CuttingPlanDetail> allDetails = cuttingPlanDetailMapper.selectList(
                    new LambdaQueryWrapper<CuttingPlanDetail>().in(CuttingPlanDetail::getCuttingPlanId, planIds));
            for (CuttingPlanDetail detail : allDetails) {
                String key = detail.getPart() + "|" + detail.getColor() + "|" + detail.getSize();
                alreadyCutMap.merge(key, detail.getActualQty() != null ? detail.getActualQty() : 0, Integer::sum);
            }
        }

        // 4. 计算每个部位+颜色+尺码的进度
        List<CuttingProgressDTO> result = new ArrayList<>();
        for (StylePart part : styleParts) {
            for (OrderColorSize ocs : orderColorSizes) {
                CuttingProgressDTO dto = new CuttingProgressDTO();
                dto.setPart(part.getPartName());
                dto.setColor(ocs.getColor());
                dto.setSize(ocs.getSize());

                // 需裁总数 = 订单该颜色尺码数量 × 单件用量
                int totalNeeded = (int) Math.ceil(ocs.getQty() * part.getQtyPerPiece().doubleValue());
                dto.setTotalNeeded(totalNeeded);

                // 已裁数量
                String key = part.getPartName() + "|" + ocs.getColor() + "|" + ocs.getSize();
                int alreadyCut = alreadyCutMap.getOrDefault(key, 0);
                dto.setAlreadyCut(alreadyCut);

                // 待裁数量
                dto.setRemaining(Math.max(0, totalNeeded - alreadyCut));

                result.add(dto);
            }
        }

        return result;
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

    /**
     * 计算单个部位的单件裁片数
     */
    private int calcPiecesForSinglePart(String partName, List<StylePart> styleParts) {
        if (partName == null || partName.isEmpty() || styleParts.isEmpty()) return 0;
        for (StylePart sp : styleParts) {
            if (partName.equals(sp.getPartName()) && sp.getQtyPerPiece() != null) {
                return sp.getQtyPerPiece().intValue();
            }
        }
        return 0;
    }

    /**
     * 计算单件裁片数（根据部位名称汇总款号部位配置的单件用量）
     */
    private int calcPiecesPerGarment(String partStr, List<StylePart> styleParts) {
        if (partStr == null || partStr.isEmpty() || styleParts.isEmpty()) return 0;
        String[] parts = partStr.split(",");
        int total = 0;
        for (String partName : parts) {
            String trimmed = partName.trim();
            for (StylePart sp : styleParts) {
                if (trimmed.equals(sp.getPartName()) && sp.getQtyPerPiece() != null) {
                    total += sp.getQtyPerPiece().intValue();
                    break;
                }
            }
        }
        return total;
    }
}
