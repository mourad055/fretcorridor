package com.flysoft.fretcorridor.cap.web;

import com.flysoft.fretcorridor.cap.client.AffectationProposeeDto;
import com.flysoft.fretcorridor.cap.client.ServiceOptClient;
import com.flysoft.fretcorridor.cap.messaging.CapEventPublisher;
import com.flysoft.fretcorridor.cap.messaging.DemandeAccepteeEvent;
import com.flysoft.fretcorridor.cap.messaging.DemandeRefuseeParChauffeurEvent;
import com.flysoft.fretcorridor.cap.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * UC-MAT-02/diffusion-course : "mes propositions" (lecture OPT) + publication
 * des evenements demande-acceptee/demande-refusee-par-chauffeur (ecriture
 * Kafka). transporteurId toujours resolu du JWT, jamais du corps de requete.
 */
class PropositionCapControllerTest {

    private static final String TOKEN = "Bearer un-jwt-quelconque";

    @Mock private ServiceOptClient serviceOptClient;
    @Mock private CapEventPublisher capEventPublisher;
    @Mock private JwtService jwtService;

    private PropositionCapController controller;
    private UUID transporteurId;
    private UUID affectationId;
    private UUID demandeId;
    private UUID capaciteId;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        controller = new PropositionCapController(serviceOptClient, capEventPublisher, jwtService);
        transporteurId = UUID.randomUUID();
        affectationId = UUID.randomUUID();
        demandeId = UUID.randomUUID();
        capaciteId = UUID.randomUUID();
        when(jwtService.extraireActeurId("un-jwt-quelconque")).thenReturn(transporteurId);
    }

    @Test
    void mesPropositions_maps_the_opt_response() {
        AffectationProposeeDto dto = new AffectationProposeeDto(
                affectationId, demandeId, capaciteId, transporteurId, "PROPOSEE",
                4.05, 9.7, 3.86, 11.5, "Douala", "Yaoundé",
                243000.0, 14400.0, 600.0, BigDecimal.valueOf(26500), false,
                java.time.Instant.now().plusSeconds(900), java.time.Instant.now());
        when(serviceOptClient.listerPropositions(transporteurId)).thenReturn(List.of(dto));

        var reponse = controller.mesPropositions(TOKEN);

        assertThat(reponse).hasSize(1);
        assertThat(reponse.get(0).affectationId()).isEqualTo(affectationId);
        assertThat(reponse.get(0).origineNom()).isEqualTo("Douala");
        assertThat(reponse.get(0).prixTransport()).isEqualByComparingTo(BigDecimal.valueOf(26500));
    }

    @Test
    void accepter_publishes_the_event_with_the_transporteurId_from_the_jwt() {
        controller.accepter(affectationId, new PropositionCapController.AffecterRequest(demandeId, capaciteId), TOKEN);

        ArgumentCaptor<DemandeAccepteeEvent> captor = ArgumentCaptor.forClass(DemandeAccepteeEvent.class);
        verify(capEventPublisher).publierDemandeAcceptee(captor.capture());
        assertThat(captor.getValue().affectationId()).isEqualTo(affectationId);
        assertThat(captor.getValue().transporteurId()).isEqualTo(transporteurId);
    }

    @Test
    void refuser_publishes_the_event_with_the_transporteurId_from_the_jwt() {
        controller.refuser(affectationId, new PropositionCapController.AffecterRequest(demandeId, capaciteId), TOKEN);

        ArgumentCaptor<DemandeRefuseeParChauffeurEvent> captor = ArgumentCaptor.forClass(DemandeRefuseeParChauffeurEvent.class);
        verify(capEventPublisher).publierDemandeRefuseeParChauffeur(captor.capture());
        assertThat(captor.getValue().affectationId()).isEqualTo(affectationId);
        assertThat(captor.getValue().transporteurId()).isEqualTo(transporteurId);
    }

    @Test
    void accepter_without_demandeId_or_capaciteId_is_refused() {
        assertThatThrownBy(() -> controller.accepter(affectationId, null, TOKEN))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("400");
    }
}
