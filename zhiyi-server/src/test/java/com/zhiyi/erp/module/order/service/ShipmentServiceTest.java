package com.zhiyi.erp.module.order.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhiyi.erp.common.exception.BizException;
import com.zhiyi.erp.module.order.entity.Shipment;
import com.zhiyi.erp.module.order.mapper.ShipmentMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShipmentServiceTest {

    @Mock
    private ShipmentMapper shipmentMapper;

    private ShipmentService shipmentService;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), Shipment.class);
        shipmentService = new ShipmentService();
        ReflectionTestUtils.setField(shipmentService, "baseMapper", shipmentMapper);
    }

    @Test
    void pageByDateBindsClosedIntervalAndKeepsPagination() {
        Page<Shipment> page = new Page<>(2, 20);
        when(shipmentMapper.selectPage(any(Page.class), any(Wrapper.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Page<Shipment> result = shipmentService.pageByDate(page, "2026-11-10", "2026-11-20");

        assertThat(result.getCurrent()).isEqualTo(2);
        assertThat(result.getSize()).isEqualTo(20);

        ArgumentCaptor<Wrapper<Shipment>> wrapperCaptor = ArgumentCaptor.forClass(Wrapper.class);
        verify(shipmentMapper).selectPage(any(Page.class), wrapperCaptor.capture());
        LambdaQueryWrapper<Shipment> wrapper = (LambdaQueryWrapper<Shipment>) wrapperCaptor.getValue();
        String sqlSegment = wrapper.getSqlSegment();
        assertThat(sqlSegment).contains("shipment_date >= #{", "shipment_date <= #{")
                .contains("ORDER BY shipment_date DESC,id DESC");
        Collection<Object> values = wrapper.getParamNameValuePairs().values();
        assertThat(values).contains(LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 20));
    }

    @Test
    void pageByDateWithoutDatesDoesNotBindDateParameters() {
        when(shipmentMapper.selectPage(any(Page.class), any(Wrapper.class))).thenAnswer(invocation -> invocation.getArgument(0));

        shipmentService.pageByDate(new Page<>(1, 10), null, " ");

        ArgumentCaptor<Wrapper<Shipment>> wrapperCaptor = ArgumentCaptor.forClass(Wrapper.class);
        verify(shipmentMapper).selectPage(any(Page.class), wrapperCaptor.capture());
        LambdaQueryWrapper<Shipment> wrapper = (LambdaQueryWrapper<Shipment>) wrapperCaptor.getValue();
        assertThat(wrapper.getSqlSegment()).doesNotContain("shipment_date >=", "shipment_date <=")
                .contains("ORDER BY shipment_date DESC,id DESC");
        assertThat(wrapper.getParamNameValuePairs()).isEmpty();
    }

    @Test
    void pageByDateTreatsSameStartAndEndAsSingleDay() {
        when(shipmentMapper.selectPage(any(Page.class), any(Wrapper.class))).thenAnswer(invocation -> invocation.getArgument(0));

        shipmentService.pageByDate(new Page<>(1, 10), "2026-11-20", "2026-11-20");

        ArgumentCaptor<Wrapper<Shipment>> wrapperCaptor = ArgumentCaptor.forClass(Wrapper.class);
        verify(shipmentMapper).selectPage(any(Page.class), wrapperCaptor.capture());
        LambdaQueryWrapper<Shipment> wrapper = (LambdaQueryWrapper<Shipment>) wrapperCaptor.getValue();
        assertThat(wrapper.getSqlSegment()).contains("shipment_date >= #{", "shipment_date <= #{");
        assertThat(wrapper.getParamNameValuePairs().values())
                .containsOnly(LocalDate.of(2026, 11, 20));
    }

    @Test
    void pageByDateKeepsBothConditionsWhenStartIsAfterEnd() {
        when(shipmentMapper.selectPage(any(Page.class), any(Wrapper.class))).thenAnswer(invocation -> invocation.getArgument(0));

        shipmentService.pageByDate(new Page<>(1, 10), "2026-11-20", "2026-11-10");

        ArgumentCaptor<Wrapper<Shipment>> wrapperCaptor = ArgumentCaptor.forClass(Wrapper.class);
        verify(shipmentMapper).selectPage(any(Page.class), wrapperCaptor.capture());
        LambdaQueryWrapper<Shipment> wrapper = (LambdaQueryWrapper<Shipment>) wrapperCaptor.getValue();
        assertThat(wrapper.getSqlSegment()).contains("shipment_date >= #{", "shipment_date <= #{");
        assertThat(wrapper.getParamNameValuePairs().values())
                .contains(LocalDate.of(2026, 11, 20), LocalDate.of(2026, 11, 10));
    }

    @Test
    void pageByDateRejectsInvalidDateFormat() {
        assertThatThrownBy(() -> shipmentService.pageByDate(new Page<>(1, 10), "2026/11/10", null))
                .isInstanceOf(BizException.class)
                .satisfies(error -> assertThat(((BizException) error).getCode()).isEqualTo(400))
                .hasMessage("日期格式必须为 YYYY-MM-DD");
    }
}
