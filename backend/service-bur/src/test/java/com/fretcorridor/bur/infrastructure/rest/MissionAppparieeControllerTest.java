package com.fretcorridor.bur.infrastructure.rest;

import com.fretcorridor.bur.domain.MissionAppariee;
import com.fretcorridor.bur.domain.MissionAppparieeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MissionAppparieeController.class)
class MissionAppparieeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MissionAppparieeService service;

    @Test
    void returns_the_missions_of_the_requested_tenant() throws Exception {
        MissionAppariee mission = new MissionAppariee(
                UUID.randomUUID(), "tenant-bgft-douala", UUID.randomUUID(), UUID.randomUUID(),
                "Douala", "Yaoundé", new BigDecimal("50000"), "XAF", Instant.now());
        when(service.listerParTenant("tenant-bgft-douala")).thenReturn(List.of(mission));

        mockMvc.perform(get("/api/v1/bur/missions-appariees").param("tenantId", "tenant-bgft-douala"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].origineNom").value("Douala"))
                .andExpect(jsonPath("$[0].destinationNom").value("Yaoundé"));
    }

    @Test
    void returns_an_empty_list_when_the_tenant_has_no_mission() throws Exception {
        when(service.listerParTenant("tenant-inconnu")).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/bur/missions-appariees").param("tenantId", "tenant-inconnu"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
