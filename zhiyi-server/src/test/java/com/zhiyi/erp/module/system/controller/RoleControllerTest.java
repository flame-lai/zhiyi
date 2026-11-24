package com.zhiyi.erp.module.system.controller;

import com.zhiyi.erp.module.system.service.RoleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class RoleControllerTest {

    @Mock
    private RoleService roleService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        RoleController controller = new RoleController();
        ReflectionTestUtils.setField(controller, "roleService", roleService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void listWithoutKeywordPassesNullAndReturnsUnchangedStructure() throws Exception {
        when(roleService.listWithEmployeeCount((String) null))
                .thenReturn(List.of(roleItem(1L, "管理员", 1, 3L)));

        mockMvc.perform(get("/api/system/roles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].name").value("管理员"))
                .andExpect(jsonPath("$.data[0].description").value("系统管理员"))
                .andExpect(jsonPath("$.data[0].code").value("admin"))
                .andExpect(jsonPath("$.data[0].sort").value(1))
                .andExpect(jsonPath("$.data[0].status").value(1))
                .andExpect(jsonPath("$.data[0].createdAt").exists())
                .andExpect(jsonPath("$.data[0].employeeCount").value(3));

        verify(roleService).listWithEmployeeCount((String) null);
    }

    @Test
    void listPassesKeywordThrough() throws Exception {
        when(roleService.listWithEmployeeCount("管理"))
                .thenReturn(List.of(roleItem(1L, "管理员", 1, 2L)));

        mockMvc.perform(get("/api/system/roles").param("keyword", "管理"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].name").value("管理员"))
                .andExpect(jsonPath("$.data[0].employeeCount").value(2));

        verify(roleService).listWithEmployeeCount("管理");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "  "})
    void listPassesBlankKeywordThroughWithoutNormalizingInController(String keyword) throws Exception {
        when(roleService.listWithEmployeeCount(keyword)).thenReturn(List.of());

        mockMvc.perform(get("/api/system/roles").param("keyword", keyword))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());

        verify(roleService).listWithEmployeeCount(keyword);
    }

    @Test
    void listReturnsEmptyArrayWhenNothingMatches() throws Exception {
        when(roleService.listWithEmployeeCount("不存在的角色片段")).thenReturn(List.of());

        mockMvc.perform(get("/api/system/roles").param("keyword", "不存在的角色片段"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());

        verify(roleService).listWithEmployeeCount("不存在的角色片段");
    }

    private Map<String, Object> roleItem(Long id, String name, Integer sort, Long employeeCount) {
        return new java.util.LinkedHashMap<>(Map.of(
                "id", id,
                "name", name,
                "description", "系统管理员",
                "code", "admin",
                "sort", sort,
                "status", 1,
                "createdAt", LocalDateTime.of(2024, 1, 1, 10, 0),
                "employeeCount", employeeCount
        ));
    }
}
