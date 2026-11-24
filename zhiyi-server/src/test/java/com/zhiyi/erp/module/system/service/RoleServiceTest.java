package com.zhiyi.erp.module.system.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.zhiyi.erp.common.tenant.TenantContext;
import com.zhiyi.erp.module.system.entity.Employee;
import com.zhiyi.erp.module.system.entity.Role;
import com.zhiyi.erp.module.system.mapper.EmployeeMapper;
import com.zhiyi.erp.module.system.mapper.RoleMapper;
import com.zhiyi.erp.module.system.mapper.RoleMenuMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoleServiceTest {

    private static final long FACTORY_ID = 1L;

    @Mock
    private RoleMapper roleMapper;

    @Mock
    private EmployeeMapper employeeMapper;

    @Mock
    private RoleMenuMapper roleMenuMapper;

    private RoleService roleService;

    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant =
                new MapperBuilderAssistant(new MybatisConfiguration(), "role-service-test");
        assistant.setCurrentNamespace("com.zhiyi.erp.module.system.service.RoleServiceTest");
        TableInfoHelper.initTableInfo(assistant, Role.class);
        TableInfoHelper.initTableInfo(assistant, Employee.class);
    }

    @BeforeEach
    void setUp() {
        roleService = new RoleService();
        ReflectionTestUtils.setField(roleService, "baseMapper", roleMapper);
        ReflectionTestUtils.setField(roleService, "roleMapper", roleMapper);
        ReflectionTestUtils.setField(roleService, "employeeMapper", employeeMapper);
        ReflectionTestUtils.setField(roleService, "roleMenuMapper", roleMenuMapper);
        TenantContext.set(FACTORY_ID);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void noKeywordKeepsTenantAndSortConditionsOnly() {
        when(roleMapper.selectList(any())).thenReturn(List.of(role(1L, "管理员", 1)));

        List<Role> roles = roleService.listAll();

        assertThat(roles).hasSize(1);
        String sql = captureRoleQuerySql();
        assertThat(sql).contains("factory_id =");
        assertThat(sql).doesNotContain("name LIKE");
        assertThat(sql).matches("(?s).*ORDER BY\\s+sort\\s+ASC.*");
        assertThat(captureRoleQuery(1).getParamNameValuePairs().values())
                .contains(FACTORY_ID);
    }

    @Test
    void keywordFiltersByNameWithTrimmedLikeValue() {
        when(roleMapper.selectList(any())).thenReturn(List.of(role(1L, "管理员", 1)));

        List<Role> roles = roleService.listAll(" 管理 ");

        assertThat(roles).hasSize(1);
        AbstractWrapper<Role, ?, ?> wrapper = captureRoleQuery(1);
        String sql = wrapper.getSqlSegment();
        assertThat(sql).contains("name LIKE");
        assertThat(sql).matches("(?s).*ORDER BY\\s+sort\\s+ASC.*");
        assertThat(sql).doesNotContain("code LIKE");
        assertThat(sql).doesNotContain("description LIKE");
        assertThat(wrapper.getParamNameValuePairs().values())
                .contains(FACTORY_ID, "%管理%");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void blankKeywordDoesNotAddNameCondition(String keyword) {
        when(roleMapper.selectList(any())).thenReturn(List.of(role(1L, "管理员", 1)));

        roleService.listAll(keyword);

        String sql = captureRoleQuery(1).getSqlSegment();
        assertThat(sql).contains("factory_id =");
        assertThat(sql).doesNotContain("name LIKE");
        assertThat(sql).matches("(?s).*ORDER BY\\s+sort\\s+ASC.*");
    }

    @Test
    void tenantConditionUsesCurrentTenantEvenWithKeyword() {
        TenantContext.set(2L);
        when(roleMapper.selectList(any())).thenReturn(List.of(role(9L, "管理二", 2)));

        roleService.listAll("管理");

        AbstractWrapper<Role, ?, ?> wrapper = captureRoleQuery(1);
        assertThat(wrapper.getSqlSegment()).contains("name LIKE");
        assertThat(wrapper.getParamNameValuePairs().values())
                .contains(2L, "%管理%")
                .doesNotContain(1L);
    }

    @Test
    void listWithEmployeeCountPreservesFieldsAndCountsEachReturnedRole() {
        when(roleMapper.selectList(any())).thenReturn(List.of(
                role(1L, "管理员", 1),
                role(2L, "生产管理", 2)
        ));
        when(employeeMapper.selectCount(any())).thenReturn(3L, 5L);

        List<Map<String, Object>> result = roleService.listWithEmployeeCount("管理");

        assertThat(result).hasSize(2);
        assertThat(result.get(0))
                .containsEntry("id", 1L)
                .containsEntry("name", "管理员")
                .containsEntry("description", "角色说明")
                .containsEntry("code", "role_1")
                .containsEntry("sort", 1)
                .containsEntry("status", 1)
                .containsEntry("employeeCount", 3L)
                .containsKey("createdAt");
        assertThat(result.get(1)).containsEntry("employeeCount", 5L);

        AbstractWrapper<Role, ?, ?> roleWrapper = captureRoleQuery(1);
        assertThat(roleWrapper.getSqlSegment()).contains("name LIKE");
        assertThat(roleWrapper.getParamNameValuePairs().values()).contains("%管理%");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Wrapper<Employee>> employeeCaptor = ArgumentCaptor.forClass(Wrapper.class);
        verify(employeeMapper, times(2)).selectCount(employeeCaptor.capture());
        AbstractWrapper<Employee, ?, ?> firstEmployeeQuery =
                (AbstractWrapper<Employee, ?, ?>) employeeCaptor.getAllValues().get(0);
        AbstractWrapper<Employee, ?, ?> secondEmployeeQuery =
                (AbstractWrapper<Employee, ?, ?>) employeeCaptor.getAllValues().get(1);
        assertThat(firstEmployeeQuery.getSqlSegment()).contains("role_id =");
        assertThat(secondEmployeeQuery.getSqlSegment()).contains("role_id =");
        assertThat(firstEmployeeQuery.getParamNameValuePairs().values()).contains(1L);
        assertThat(secondEmployeeQuery.getParamNameValuePairs().values()).contains(2L);
    }

    @Test
    void noMatchReturnsEmptyListWithoutCountingEmployees() {
        when(roleMapper.selectList(any())).thenReturn(Collections.emptyList());

        List<Map<String, Object>> result = roleService.listWithEmployeeCount("不存在");

        assertThat(result).isEmpty();
        verify(employeeMapper, never()).selectCount(any());
    }

    @Test
    void noArgOverloadsStillReturnFullList() {
        when(roleMapper.selectList(any())).thenReturn(List.of(role(1L, "管理员", 1)));
        when(employeeMapper.selectCount(any())).thenReturn(0L);

        assertThat(roleService.listAll()).hasSize(1);
        assertThat(roleService.listWithEmployeeCount()).hasSize(1);

        ArgumentCaptor<Wrapper<Role>> captor = ArgumentCaptor.forClass(Wrapper.class);
        verify(roleMapper, times(2)).selectList(captor.capture());
        assertThat(captor.getAllValues()).allSatisfy(wrapper ->
                assertThat(wrapper.getSqlSegment()).doesNotContain("name LIKE"));
    }

    private String captureRoleQuerySql() {
        ArgumentCaptor<Wrapper<Role>> captor = ArgumentCaptor.forClass(Wrapper.class);
        verify(roleMapper, times(1)).selectList(captor.capture());
        return captor.getValue().getSqlSegment();
    }

    private AbstractWrapper<Role, ?, ?> captureRoleQuery(int times) {
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Wrapper<Role>> captor = ArgumentCaptor.forClass(Wrapper.class);
        verify(roleMapper, times(times)).selectList(captor.capture());
        return (AbstractWrapper<Role, ?, ?>) captor.getValue();
    }

    private Role role(Long id, String name, Integer sort) {
        Role role = new Role();
        role.setId(id);
        role.setFactoryId(FACTORY_ID);
        role.setName(name);
        role.setDescription("角色说明");
        role.setCode("role_" + id);
        role.setSort(sort);
        role.setStatus(1);
        role.setCreatedAt(LocalDateTime.of(2024, 1, 1, 10, 0));
        return role;
    }
}
