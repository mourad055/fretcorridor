package com.flysoft.fretcorridor.cap.web;

import com.flysoft.fretcorridor.cap.domain.Capacite;
import com.flysoft.fretcorridor.cap.domain.CapaciteService;
import com.flysoft.fretcorridor.cap.domain.ModeDeclaration;
import com.flysoft.fretcorridor.cap.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * IDOR corrigé (audit de suivi du 20 août, périmètre Mobile) :
 * GET /api/cap/capacites/{id} ne vérifiait jamais son appelant (permitAll
 * nu côté Spring Security, aucun JWT disponible dans le flux Kafka
 * service-not -> service-cap). Désormais protégé par une clé interne
 * partagée (X-Internal-Service-Key).
 */
class CapaciteControllerTest {

    private static final String CLE = "cle-interne-test";

    @Mock private CapaciteService capaciteService;
    @Mock private JwtService jwtService;

    private CapaciteController controller;
    private UUID capaciteId;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        controller = new CapaciteController(capaciteService, jwtService, CLE);
        capaciteId = UUID.randomUUID();
    }

    @Test
    void obtenir_without_the_internal_key_is_refused() {
        assertThatThrownBy(() -> controller.obtenir(capaciteId, null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("401");
    }

    @Test
    void obtenir_with_the_wrong_internal_key_is_refused() {
        assertThatThrownBy(() -> controller.obtenir(capaciteId, "mauvaise-cle"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("401");
    }

    @Test
    void obtenir_with_the_correct_internal_key_succeeds() {
        Capacite capacite = new Capacite(UUID.randomUUID(), UUID.randomUUID(), "tenant-bgft-douala", null,
                ModeDeclaration.TOTALE, java.math.BigDecimal.valueOf(1000), null, null,
                java.math.BigDecimal.valueOf(1000), 4.05, 9.7, "FOURGON",
                null, null, null, null, null, null, false, null);
        when(capaciteService.obtenir(capaciteId)).thenReturn(capacite);

        var reponse = controller.obtenir(capaciteId, CLE);

        assertThat(reponse).isNotNull();
    }
}
