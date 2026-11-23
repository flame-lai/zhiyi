package com.zhiyi.erp.module.order.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhiyi.erp.module.order.entity.Shipment;
import com.zhiyi.erp.module.order.service.ShipmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ShipmentControllerTest {

    @Mock
    private ShipmentService shipmentService;

    @InjectMocks
    private ShipmentController shipmentController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(shipmentController).build();
    }

    @Test
    void pageWithoutOrderIdUsesDefaultPagingAndReturnsRecords() throws Exception {
        Shipment shipment = new Shipment();
        shipment.setId(1L);
        shipment.setFactoryId(1L);
        shipment.setOrderId(1001L);
        shipment.setOrderItemId(31L);
        shipment.setQty(200);
        shipment.setShipmentDate(LocalDate.of(2024, 1, 15));

        Page<Shipment> result = new Page<>(1, 10);
        result.setRecords(List.of(shipment));
        result.setTotal(1);
        when(shipmentService.pageShipments(any(), isNull())).thenReturn(result);

        mockMvc.perform(get("/api/shipments/page"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records[0].orderId").value(1001))
                .andExpect(jsonPath("$.data.total").value(1));

        ArgumentCaptor<Page<Shipment>> pageCaptor = ArgumentCaptor.forClass(Page.class);
        verify(shipmentService).pageShipments(pageCaptor.capture(), isNull());
        assertThat(pageCaptor.getValue().getCurrent()).isEqualTo(1);
        assertThat(pageCaptor.getValue().getSize()).isEqualTo(10);
    }

    @Test
    void pageWithOrderIdPassesFilterAndPaging() throws Exception {
        Page<Shipment> empty = new Page<>(2, 5);
        empty.setRecords(List.of());
        empty.setTotal(0);
        when(shipmentService.pageShipments(any(), any())).thenReturn(empty);

        mockMvc.perform(get("/api/shipments/page")
                        .param("page", "2")
                        .param("size", "5")
                        .param("orderId", "1001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(0))
                .andExpect(jsonPath("$.data.records").isEmpty());

        ArgumentCaptor<Page<Shipment>> pageCaptor = ArgumentCaptor.forClass(Page.class);
        ArgumentCaptor<Long> orderIdCaptor = ArgumentCaptor.forClass(Long.class);
        verify(shipmentService).pageShipments(pageCaptor.capture(), orderIdCaptor.capture());
        assertThat(pageCaptor.getValue().getCurrent()).isEqualTo(2);
        assertThat(pageCaptor.getValue().getSize()).isEqualTo(5);
        assertThat(orderIdCaptor.getValue()).isEqualTo(1001L);
    }
}
