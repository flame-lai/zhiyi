package com.zhiyi.erp.module.admin.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhiyi.erp.common.exception.BizException;
import com.zhiyi.erp.common.tenant.TenantContext;
import com.zhiyi.erp.module.admin.dto.TimedWageQueryDTO;
import com.zhiyi.erp.module.admin.entity.TimedWage;
import com.zhiyi.erp.module.admin.mapper.TimedWageMapper;
import com.zhiyi.erp.module.admin.mapper.WageSummaryMapper;
import com.zhiyi.erp.module.admin.vo.TimedWageVO;
import com.zhiyi.erp.module.admin.entity.WageSummary;
import com.zhiyi.erp.module.system.entity.Employee;
import com.zhiyi.erp.module.system.mapper.EmployeeMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class TimedWageService extends ServiceImpl<TimedWageMapper, TimedWage> {

    @Autowired
    private EmployeeMapper employeeMapper;
    @Autowired
    private WageSummaryMapper wageSummaryMapper;

    public IPage<TimedWageVO> pageVO(Page<TimedWage> page, TimedWageQueryDTO query) {
        LambdaQueryWrapper<TimedWage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TimedWage::getFactoryId, TenantContext.get());
        if (query.getEmployeeId() != null) {
            wrapper.eq(TimedWage::getEmployeeId, query.getEmployeeId());
        }
        if (query.getYearMonth() != null && !query.getYearMonth().isBlank()) {
            wrapper.apply("DATE_FORMAT(work_date, '%Y-%m') = {0}", query.getYearMonth());
        }
        if (query.getStartDate() != null && !query.getStartDate().isBlank()) {
            wrapper.ge(TimedWage::getWorkDate, LocalDate.parse(query.getStartDate()));
        }
        if (query.getEndDate() != null && !query.getEndDate().isBlank()) {
            wrapper.le(TimedWage::getWorkDate, LocalDate.parse(query.getEndDate()));
        }
        wrapper.orderByDesc(TimedWage::getWorkDate);
        Page<TimedWage> result = page(page, wrapper);
        List<TimedWageVO> records = buildVOList(result.getRecords());
        Page<TimedWageVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(records);
        return voPage;
    }

    @Transactional
    public void create(TimedWage wage) {
        prepareForSave(wage);
        save(wage);
    }

    @Transactional
    public void createBatch(List<TimedWage> wages) {
        for (TimedWage wage : wages) {
            prepareForSave(wage);
            save(wage);
        }
    }

    @Transactional
    public void update(Long id, TimedWage wage) {
        TimedWage existing = getById(id);
        if (existing == null || !Objects.equals(existing.getFactoryId(), TenantContext.get())) {
            throw new BizException("计时工资记录不存在");
        }
        wage.setId(id);
        wage.setFactoryId(existing.getFactoryId());
        wage.setCreatedBy(existing.getCreatedBy());
        prepareAmount(wage);
        validateEmployee(wage.getEmployeeId());
        assertNotPaid(existing.getEmployeeId(), existing.getWorkDate());
        assertNotPaid(wage.getEmployeeId(), wage.getWorkDate());
        updateById(wage);
    }

    @Transactional
    public void delete(Long id) {
        TimedWage existing = getById(id);
        if (existing == null || !Objects.equals(existing.getFactoryId(), TenantContext.get())) {
            throw new BizException("计时工资记录不存在");
        }
        assertNotPaid(existing.getEmployeeId(), existing.getWorkDate());
        removeById(id);
    }

    public List<TimedWageVO> buildVOList(List<TimedWage> wages) {
        Set<Long> employeeIds = wages.stream()
                .map(TimedWage::getEmployeeId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        employeeIds.addAll(wages.stream()
                .map(TimedWage::getCreatedBy)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()));

        Map<Long, Employee> employeeMap = employeeIds.isEmpty()
                ? Map.of()
                : employeeMapper.selectBatchIds(employeeIds).stream()
                .collect(Collectors.toMap(Employee::getId, e -> e));

        return wages.stream().map(wage -> {
            TimedWageVO vo = new TimedWageVO();
            BeanUtils.copyProperties(wage, vo);
            Employee employee = employeeMap.get(wage.getEmployeeId());
            if (employee != null) {
                vo.setEmployeeNo(employee.getEmployeeNo());
                vo.setEmployeeName(employee.getName());
            }
            Employee creator = employeeMap.get(wage.getCreatedBy());
            if (creator != null) {
                vo.setCreatedByName(creator.getName());
            }
            return vo;
        }).collect(Collectors.toList());
    }

    private void prepareForSave(TimedWage wage) {
        wage.setFactoryId(TenantContext.get());
        if (wage.getWorkDate() == null) {
            wage.setWorkDate(LocalDate.now());
        }
        wage.setCreatedBy(StpUtil.getLoginIdAsLong());
        prepareAmount(wage);
        validateEmployee(wage.getEmployeeId());
        assertNotPaid(wage.getEmployeeId(), wage.getWorkDate());
    }

    private void prepareAmount(TimedWage wage) {
        if (wage.getEmployeeId() == null) {
            throw new BizException("请选择工人");
        }
        if (wage.getHours() == null || wage.getHourlyRate() == null) {
            throw new BizException("请填写工时和时薪");
        }
        if (wage.getHours().compareTo(BigDecimal.ZERO) <= 0 || wage.getHourlyRate().compareTo(BigDecimal.ZERO) < 0) {
            throw new BizException("工时和时薪填写不正确");
        }
        wage.setAmount(wage.getHours().multiply(wage.getHourlyRate()));
    }

    private void validateEmployee(Long employeeId) {
        Employee employee = employeeMapper.selectById(employeeId);
        if (employee == null || !Objects.equals(employee.getFactoryId(), TenantContext.get())) {
            throw new BizException("工人不存在");
        }
    }

    private void assertNotPaid(Long employeeId, LocalDate workDate) {
        if (employeeId == null || workDate == null) {
            return;
        }
        WageSummary summary = wageSummaryMapper.selectOne(new LambdaQueryWrapper<WageSummary>()
                .eq(WageSummary::getFactoryId, TenantContext.get())
                .eq(WageSummary::getEmployeeId, employeeId)
                .eq(WageSummary::getYearMonth, java.time.YearMonth.from(workDate).toString()));
        if (summary != null && Objects.equals(summary.getStatus(), 2)) {
            throw new BizException("已发放工资不可修改计时记录");
        }
    }
}
