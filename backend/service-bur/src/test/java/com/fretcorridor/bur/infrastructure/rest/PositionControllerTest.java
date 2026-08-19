package com.fretcorridor.bur.infrastructure.rest;

import com.fretcorridor.bur.domain.PositionService;
import com.fretcorridor.bur.domain.PositionVehicule;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PositionController.class)
class PositionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PositionService service;

    @Test
    void returns_the_positions_of_the_requested_tenant() throws Exception {
        PositionVehicule position = new PositionVehicule(
                UUID.randomUUID(), "tenant-bgft-douala", UUID.randomUUID(), 4.05, 9.76, Instant.now());
        when(service.listerParTenant("tenant-bgft-douala")).thenReturn(List.of(position));

        mockMvc.perform(get("/api/v1/bur/positions").param("tenantId", "tenant-bgft-douala"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].latitude").value(4.05))
                .andExpect(jsonPath("$[0].longitude").value(9.76));
    }

    @Test
    void returns_an_empty_list_when_the_tenant_has_no_position() throws Exception {
        when(service.listerParTenant("tenant-inconnu")).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/bur/positions").param("tenantId", "tenant-inconnu"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
