package com.zhiyi.erp.module.mobile.service;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.zhiyi.erp.common.exception.BizException;
import com.zhiyi.erp.module.mobile.vo.MobileLoginVO;
import com.zhiyi.erp.module.mobile.vo.MobileMenuVO;
import com.zhiyi.erp.module.mobile.vo.MobileProfileVO;
import com.zhiyi.erp.module.system.entity.Employee;
import com.zhiyi.erp.module.system.entity.Menu;
import com.zhiyi.erp.module.system.entity.Role;
import com.zhiyi.erp.module.system.mapper.EmployeeMapper;
import com.zhiyi.erp.module.system.mapper.RoleMapper;
import com.zhiyi.erp.module.system.service.MenuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MobileService {

    @Autowired
    private EmployeeMapper employeeMapper;

    @Autowired
    private RoleMapper roleMapper;

    @Autowired
    private MenuService menuService;

    public MobileLoginVO loginByCode(String loginCode) {
        Employee employee = employeeMapper.selectByLoginCode(loginCode);
        if (employee == null) {
            throw new BizException("登录码无效");
        }
        if (employee.getStatus() != 1) {
            throw new BizException("账号已停用，请联系管理员");
        }
        return buildLoginVO(employee);
    }

    public MobileLoginVO loginByAccount(String account, String password) {
        Employee employee = employeeMapper.selectByLoginAccount(account);
        if (employee == null) {
            throw new BizException("账号不存在");
        }
        if (employee.getStatus() != 1) {
            throw new BizException("账号已停用，请联系管理员");
        }
        if (!BCrypt.checkpw(password, employee.getLoginPassword())) {
            throw new BizException("密码错误");
        }
        return buildLoginVO(employee);
    }

    public MobileProfileVO getProfile(Long employeeId) {
        Employee employee = employeeMapper.selectById(employeeId);
        if (employee == null) {
            throw new BizException("员工不存在");
        }

        Role role = employee.getRoleId() != null ? roleMapper.selectById(employee.getRoleId()) : null;

        MobileProfileVO vo = new MobileProfileVO();

        MobileProfileVO.EmployeeInfo empInfo = new MobileProfileVO.EmployeeInfo();
        empInfo.setId(employee.getId());
        empInfo.setCode(employee.getEmployeeNo());
        empInfo.setName(employee.getName());
        vo.setEmployee(empInfo);

        MobileProfileVO.RoleInfo roleInfo = new MobileProfileVO.RoleInfo();
        if (role != null) {
            roleInfo.setId(role.getId());
            roleInfo.setName(role.getName());
        }
        vo.setRole(roleInfo);

        vo.setMenus(getMobileMenus(employee.getRoleId(), role));
        vo.setPermissions(getMobilePermissions(employee.getRoleId(), role));
        vo.setDefaultHomeType(determineDefaultHomeType(employee.getRoleId(), role));

        return vo;
    }

    private MobileLoginVO buildLoginVO(Employee employee) {
        StpUtil.login(employee.getId());

        StpUtil.getSession().set("factoryId", employee.getFactoryId());
        StpUtil.getSession().set("employeeId", employee.getId());
        StpUtil.getSession().set("roleId", employee.getRoleId());

        Role role = employee.getRoleId() != null ? roleMapper.selectById(employee.getRoleId()) : null;

        MobileLoginVO vo = new MobileLoginVO();
        vo.setToken(StpUtil.getTokenValue());

        MobileLoginVO.EmployeeInfo empInfo = new MobileLoginVO.EmployeeInfo();
        empInfo.setId(employee.getId());
        empInfo.setCode(employee.getEmployeeNo());
        empInfo.setName(employee.getName());
        vo.setEmployee(empInfo);

        MobileLoginVO.RoleInfo roleInfo = new MobileLoginVO.RoleInfo();
        if (role != null) {
            roleInfo.setId(role.getId());
            roleInfo.setName(role.getName());
        }
        vo.setRole(roleInfo);

        vo.setMenus(getMobileMenus(employee.getRoleId(), role));
        vo.setPermissions(getMobilePermissions(employee.getRoleId(), role));
        vo.setDefaultHomeType(determineDefaultHomeType(employee.getRoleId(), role));

        return vo;
    }

    public List<MobileMenuVO> getMobileMenus(Long roleId, Role role) {
        List<Menu> menus;
        if (role != null && "super_admin".equals(role.getCode())) {
            menus = menuService.listAll();
        } else {
            menus = menuService.getMenusByRoleId(roleId);
        }

        return menus.stream()
                .filter(m -> "mobile".equals(m.getPlatform()) && m.getStatus() != null && m.getStatus() == 1)
                .sorted(Comparator.comparingInt(m -> m.getSort() != null ? m.getSort() : 0))
                .map(m -> {
                    MobileMenuVO menuVO = new MobileMenuVO();
                    menuVO.setCode(m.getCode());
                    menuVO.setTitle(m.getName());
                    menuVO.setPath(m.getPath());
                    menuVO.setGroup(m.getCode() != null && m.getCode().startsWith("mobile:admin") ? "admin" : "employee");
                    menuVO.setIcon(m.getIcon());
                    menuVO.setSort(m.getSort());
                    return menuVO;
                })
                .collect(Collectors.toList());
    }

    public List<String> getMobilePermissions(Long roleId, Role role) {
        List<Menu> menus;
        if (role != null && "super_admin".equals(role.getCode())) {
            menus = menuService.listAll();
        } else {
            menus = menuService.getMenusByRoleId(roleId);
        }

        return menus.stream()
                .filter(m -> "mobile".equals(m.getPlatform()) && m.getStatus() != null && m.getStatus() == 1)
                .map(Menu::getPermission)
                .filter(p -> p != null && !p.isEmpty())
                .distinct()
                .collect(Collectors.toList());
    }

    private String determineDefaultHomeType(Long roleId, Role role) {
        if (role != null && "super_admin".equals(role.getCode())) {
            return "admin";
        }
        List<MobileMenuVO> mobileMenus = getMobileMenus(roleId, role);
        boolean hasAdminMenu = mobileMenus.stream().anyMatch(m -> "admin".equals(m.getGroup()));
        return hasAdminMenu ? "admin" : "employee";
    }
}
