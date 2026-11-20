package com.zhiyi.erp.module.system.mapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.scripting.xmltags.XMLLanguageDriver;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmployeeMapperSqlTest {

    @Test
    void keywordMatchesNameEmployeeNoAndPhone() throws Exception {
        BoundSql boundSql = buildBoundSql("0013");
        String sql = normalize(boundSql.getSql());

        assertTrue(sql.contains("e.name LIKE CONCAT('%', ?, '%')"), sql);
        assertTrue(sql.contains("e.employee_no LIKE CONCAT('%', ?, '%')"), sql);
        assertTrue(sql.contains("e.phone LIKE CONCAT('%', ?, '%')"), sql);
        assertTrue(sql.contains("OR e.employee_no LIKE CONCAT('%', ?, '%')"), sql);
        assertTrue(sql.contains("OR e.phone LIKE CONCAT('%', ?, '%')"), sql);
        assertTrue(sql.contains("e.deleted = 0"), sql);
        assertTrue(sql.contains("e.factory_id = ?"), sql);
        assertEquals("ORDER BY e.status DESC, e.created_at DESC", sql.substring(sql.indexOf("ORDER BY")));
        assertFalse(sql.contains("0013"), "keyword must be bound as a parameter, not concatenated: " + sql);

        List<ParameterMapping> mappings = boundSql.getParameterMappings();
        assertEquals(4, mappings.size(), "factoryId followed by three keyword bindings: " + mappings);
        assertEquals("factoryId", mappings.get(0).getProperty());
        assertEquals("keyword", mappings.get(1).getProperty());
        assertEquals("keyword", mappings.get(2).getProperty());
        assertEquals("keyword", mappings.get(3).getProperty());
    }

    @Test
    void nullKeywordDoesNotAddFilter() throws Exception {
        assertNoKeywordFilter(buildBoundSql(null));
    }

    @Test
    void emptyKeywordDoesNotAddFilter() throws Exception {
        assertNoKeywordFilter(buildBoundSql(""));
    }

    private void assertNoKeywordFilter(BoundSql boundSql) {
        String sql = normalize(boundSql.getSql());
        assertFalse(sql.contains("e.phone LIKE"), sql);
        assertFalse(sql.contains("e.name LIKE"), sql);
        assertTrue(sql.contains("e.deleted = 0"), sql);
        assertTrue(sql.contains("e.factory_id = ?"), sql);
        assertEquals(1, boundSql.getParameterMappings().size());
        assertEquals("factoryId", boundSql.getParameterMappings().get(0).getProperty());
    }

    private BoundSql buildBoundSql(String keyword) throws Exception {
        Method method = EmployeeMapper.class.getMethod(
                "selectPageWithRoleName", Page.class, Long.class, String.class);
        String script = String.join("", method.getAnnotation(Select.class).value());

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("factoryId", 1L);
        parameters.put("keyword", keyword);

        return new XMLLanguageDriver()
                .createSqlSource(new Configuration(), script, Map.class)
                .getBoundSql(parameters);
    }

    private String normalize(String sql) {
        return sql.replaceAll("\\s+", " ").trim();
    }
}
