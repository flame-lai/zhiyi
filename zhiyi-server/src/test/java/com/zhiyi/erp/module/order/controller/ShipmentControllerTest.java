package com.zhiyi.erp.module.order.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhiyi.erp.common.exception.BizException;
import com.zhiyi.erp.common.config.JacksonConfig;
import com.zhiyi.erp.common.exception.GlobalExceptionHandler;
import com.zhiyi.erp.module.order.entity.Shipment;
import com.zhiyi.erp.module.order.service.ShipmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ShipmentControllerTest {

    @Mock
    private ShipmentService shipmentService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ShipmentController controller = new ShipmentController();
        org.springframework.test.util.ReflectionTestUtils.setField(controller, "shipmentService", shipmentService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(new JacksonConfig().objectMapper()))
                .build();
    }

    @Test
    void oldListEndpointStillReturnsArrayAndDoesNotUsePageQuery() throws Exception {
        Shipment shipment = new Shipment();
        shipment.setId(1L);
        shipment.setOrderId(9L);
        shipment.setShipmentDate(LocalDate.of(2026, 11, 20));
        when(shipmentService.listByOrder(9L)).thenReturn(List.of(shipment));

        mockMvc.perform(get("/api/shipments").param("orderId", "9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].shipmentDate").value("2026-11-20"));

        verify(shipmentService).listByOrder(9L);
        verify(shipmentService, never()).pageByDate(any(Page.class), any(), any());
    }

    @Test
    void pageEndpointUsesDefaultPaginationAndReturnsPagePayload() throws Exception {
        Page<Shipment> result = new Page<>(1, 10);
        result.setTotal(1);
        Shipment shipment = new Shipment();
        shipment.setId(2L);
        shipment.setShipmentDate(LocalDate.of(2026, 11, 20));
        result.setRecords(List.of(shipment));
        when(shipmentService.pageByDate(any(Page.class), isNull(), isNull())).thenReturn(result);

        mockMvc.perform(get("/api/shipments/page"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records[0].id").value(2))
                .andExpect(jsonPath("$.data.total").value(1));

        ArgumentCaptor<Page<Shipment>> pageCaptor = ArgumentCaptor.forClass(Page.class);
        verify(shipmentService).pageByDate(pageCaptor.capture(), isNull(), isNull());
        assertThat(pageCaptor.getValue().getCurrent()).isEqualTo(1);
        assertThat(pageCaptor.getValue().getSize()).isEqualTo(10);
    }

    @Test
    void pageEndpointPassesDateRangeAndPagination() throws Exception {
        Page<Shipment> result = new Page<>(2, 20);
        when(shipmentService.pageByDate(any(Page.class), any(), any())).thenReturn(result);

        mockMvc.perform(get("/api/shipments/page")
                        .param("page", "2")
                        .param("size", "20")
                        .param("startDate", "2026-11-10")
                        .param("endDate", "2026-11-20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<Page<Shipment>> pageCaptor = ArgumentCaptor.forClass(Page.class);
        verify(shipmentService).pageByDate(pageCaptor.capture(), org.mockito.ArgumentMatchers.eq("2026-11-10"),
                org.mockito.ArgumentMatchers.eq("2026-11-20"));
        assertThat(pageCaptor.getValue().getCurrent()).isEqualTo(2);
        assertThat(pageCaptor.getValue().getSize()).isEqualTo(20);
    }

    @Test
    void pageEndpointReturnsBusinessErrorForInvalidDate() throws Exception {
        when(shipmentService.pageByDate(any(Page.class), any(), any()))
                .thenThrow(new BizException(400, "日期格式必须为 YYYY-MM-DD"));

        mockMvc.perform(get("/api/shipments/page").param("startDate", "2026-13-99"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("日期格式必须为 YYYY-MM-DD"));
    }
}
