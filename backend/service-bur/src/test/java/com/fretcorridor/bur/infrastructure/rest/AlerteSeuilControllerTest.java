package com.fretcorridor.bur.infrastructure.rest;

import com.fretcorridor.bur.domain.AlerteSeuil;
import com.fretcorridor.bur.domain.AlerteSeuilService;
import com.fretcorridor.bur.domain.Comparateur;
import com.fretcorridor.bur.domain.EtatAlerte;
import com.fretcorridor.bur.domain.IndicateurObservatoire;
import com.fretcorridor.bur.infrastructure.config.SecurityConfig;
import com.fretcorridor.bur.infrastructure.security.JwtAuthenticationFilter;
import com.fretcorridor.bur.infrastructure.security.JwtService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class})
@WebMvcTest(AlerteSeuilController.class)
class AlerteSeuilControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Value("${fretcorridor.jwt.secret}")
    private String jwtSecret;

    private String token(String tenantId) {
        return token(tenantId, UUID.randomUUID().toString());
    }

    private String token(String tenantId, String acteurId) {
        return Jwts.builder()
                .subject(acteurId)
                .claim("roles", List.of("BUREAU"))
                .claim("tenantId", tenantId)
                .signWith(Keys.hmacShaKeyFor(jwtSecret.getBytes()))
                .compact();
    }

    @MockBean
    private AlerteSeuilService service;

    @Test
    void configure_une_alerte() throws Exception {
        UUID axeId = UUID.randomUUID();
        AlerteSeuil alerte = new AlerteSeuil("alerte-1", "tenant-bgft-douala", axeId, IndicateurObservatoire.PRIX_MEDIANE,
                Comparateur.SUPERIEUR, new BigDecimal("25000"), "actor-bureau-1", Instant.now());
        when(service.configurer("tenant-bgft-douala", axeId, IndicateurObservatoire.PRIX_MEDIANE,
                Comparateur.SUPERIEUR, new BigDecimal("25000"), "actor-bureau-1")).thenReturn(alerte);

        mockMvc.perform(post("/api/v1/bur/alertes")
                        .header("Authorization", "Bearer " + token("tenant-bgft-douala", "actor-bureau-1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"axeId": "%s", "indicateur": "PRIX_MEDIANE",
                                 "comparateur": "SUPERIEUR", "seuil": 25000}
                                """.formatted(axeId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("alerte-1"))
                .andExpect(jsonPath("$.indicateur").value("PRIX_MEDIANE"));
    }

    @Test
    void liste_l_etat_des_alertes_du_tenant() throws Exception {
        UUID axeId = UUID.randomUUID();
        AlerteSeuil alerte = new AlerteSeuil("alerte-1", "tenant-bgft-douala", axeId, IndicateurObservatoire.NOMBRE_MISSIONS,
                Comparateur.INFERIEUR, new BigDecimal("5"), "actor-bureau-1", Instant.now());
        when(service.evaluer("tenant-bgft-douala")).thenReturn(List.of(
                new EtatAlerte(alerte, true, true, new BigDecimal("3"))
        ));

        mockMvc.perform(get("/api/v1/bur/alertes/etat")
                        .header("Authorization", "Bearer " + token("tenant-bgft-douala")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].evaluable").value(true))
                .andExpect(jsonPath("$[0].declenchee").value(true))
                .andExpect(jsonPath("$[0].valeurActuelle").value(3));
    }

    @Test
    void supprime_une_alerte() throws Exception {
        mockMvc.perform(delete("/api/v1/bur/alertes/alerte-1")
                        .header("Authorization", "Bearer " + token("tenant-bgft-douala")))
                .andExpect(status().isNoContent());

        verify(service).supprimer("alerte-1", "tenant-bgft-douala");
    }

    @Test
    void refuse_une_configuration_sans_indicateur() throws Exception {
        mockMvc.perform(post("/api/v1/bur/alertes")
                        .header("Authorization", "Bearer " + token("tenant-bgft-douala"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"axeId": "%s",
                                 "comparateur": "SUPERIEUR", "seuil": 25000}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isBadRequest());
    }
}
