package com.fretcorridor.bur.infrastructure.rest;

import com.fretcorridor.bur.domain.MissionAppariee;
import com.fretcorridor.bur.domain.MissionAppparieeService;
import com.fretcorridor.bur.domain.ObservatoireAxe;
import com.fretcorridor.bur.domain.ObservatoireService;
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

    @MockBean
    private ObservatoireService observatoireService;

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

    @Test
    void returns_observatoire_indicators_once_the_threshold_is_reached() throws Exception {
        UUID axeId = UUID.randomUUID();
        ObservatoireAxe observatoire = ObservatoireAxe.calcule(axeId, 3, 5, new BigDecimal("30000"),
                new BigDecimal("20000"), "XAF", 0.6);
        when(observatoireService.indicateursPourAxe("tenant-bgft-douala", axeId)).thenReturn(observatoire);

        mockMvc.perform(get("/api/v1/bur/observatoire")
                        .param("tenantId", "tenant-bgft-douala")
                        .param("axeId", axeId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.seuilAtteint").value(true))
                .andExpect(jsonPath("$.nombreMissions").value(5))
                .andExpect(jsonPath("$.prixMediane").value(30000))
                .andExpect(jsonPath("$.prixDispersion").value(20000))
                .andExpect(jsonPath("$.tauxDesequilibreDirectionnel").value(0.6));
    }

    @Test
    void hides_indicators_below_the_aggregation_threshold() throws Exception {
        UUID axeId = UUID.randomUUID();
        when(observatoireService.indicateursPourAxe("tenant-bgft-douala", axeId))
                .thenReturn(ObservatoireAxe.sousLeSeuil(axeId, 3));

        mockMvc.perform(get("/api/v1/bur/observatoire")
                        .param("tenantId", "tenant-bgft-douala")
                        .param("axeId", axeId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.seuilAtteint").value(false))
                .andExpect(jsonPath("$.nombreMissions").doesNotExist())
                .andExpect(jsonPath("$.prixMediane").doesNotExist());
    }
}
