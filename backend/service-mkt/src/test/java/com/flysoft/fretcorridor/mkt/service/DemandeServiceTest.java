package com.flysoft.fretcorridor.mkt.service;

import com.flysoft.fretcorridor.mkt.client.AxeDto;
import com.flysoft.fretcorridor.mkt.client.ServiceCapClient;
import com.flysoft.fretcorridor.mkt.client.ServiceGeoClient;
import com.flysoft.fretcorridor.mkt.client.ServiceNotClient;
import com.flysoft.fretcorridor.mkt.dto.DemandeDto;
import com.flysoft.fretcorridor.mkt.entity.CatalogueEmballage;
import com.flysoft.fretcorridor.mkt.entity.Demande;
import com.flysoft.fretcorridor.mkt.entity.Proposition;
import com.flysoft.fretcorridor.mkt.messaging.DemandePublieeEvent;
import com.flysoft.fretcorridor.mkt.messaging.MktEventPublisher;
import com.flysoft.fretcorridor.mkt.repository.CatalogueEmballageRepository;
import com.flysoft.fretcorridor.mkt.repository.DemandeRepository;
import com.flysoft.fretcorridor.mkt.repository.PropositionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DemandeServiceTest {

    @Mock private DemandeRepository demandeRepository;
    @Mock private CatalogueEmballageRepository catalogueRepository;
    @Mock private MktEventPublisher eventPublisher;
    @Mock private PropositionRepository propositionRepository;
    @Mock private ServiceGeoClient serviceGeoClient;
    @Mock private ServiceCapClient serviceCapClient;
    @Mock private ServiceNotClient serviceNotClient;

    private DemandeService service;
    private UUID clientActeurId;
    private UUID typeEmballageId;
    private static final String TENANT = "tenant-bgft-douala";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new DemandeService(demandeRepository, catalogueRepository, eventPublisher, propositionRepository, serviceGeoClient, serviceCapClient, serviceNotClient, 333.0);
        clientActeurId = UUID.randomUUID();
        typeEmballageId = UUID.randomUUID();

        CatalogueEmballage emballage = CatalogueEmballage.builder()
                .id(typeEmballageId)
                .nom("Sac de ciment")
                .icone("inventory_2")
                .poidsUnitaireKg(50.0)
                .volumeUnitaireM3(0.03)
                .fragileParDefaut(false)
                .build();
        when(catalogueRepository.findById(typeEmballageId)).thenReturn(Optional.of(emballage));
        when(demandeRepository.save(any(Demande.class))).thenAnswer(inv -> {
            Demande demande = inv.getArgument(0);
            if (demande.getId() == null) demande.setId(UUID.randomUUID());
            return demande;
        });
    }

    private DemandeDto.PublierRequest requeteValide() {
        var requete = new DemandeDto.PublierRequest();
        requete.setVilleDepart("Douala");
        requete.setVilleArrivee("Yaounde");
        requete.setTypeEmballageId(typeEmballageId);
        requete.setQuantite(10);
        requete.setTypeDisponibilite("DES_QUE_POSSIBLE");
        requete.setModeCollecte("DOMICILE");
        requete.setDestinataireNom("Jean Mballa");
        requete.setDestinataireTelephone("+237600000000");
        return requete;
    }

    @Test
    void publishing_a_request_on_a_covered_axis_resolves_the_axis_and_publishes_the_event() {
        UUID axeId = UUID.randomUUID();
        AxeDto axe = new AxeDto(axeId, "Douala", "Yaounde", 4.05, 9.7, 3.87, 11.52, null);
        when(serviceGeoClient.resoudreAxe("Douala", "Yaounde")).thenReturn(Optional.of(axe));

        service.publier(requeteValide(), clientActeurId, TENANT, "NIVEAU_1");

        ArgumentCaptor<Demande> captor = ArgumentCaptor.forClass(Demande.class);
        verify(demandeRepository).save(captor.capture());
        assertThat(captor.getValue().getAxeId()).isEqualTo(axeId);
        assertThat(captor.getValue().getValeursCriteres()).isNotNull();
        assertThat(captor.getValue().getStatut()).isEqualTo(Demande.StatutDemande.PUBLIEE);

        ArgumentCaptor<DemandePublieeEvent> eventCaptor = ArgumentCaptor.forClass(DemandePublieeEvent.class);
        verify(eventPublisher).publierDemandePubliee(eventCaptor.capture());
        assertThat(eventCaptor.getValue().axeId()).isEqualTo(axeId);
    }

    @Test
    void publishing_a_request_on_an_uncovered_axis_is_saved_but_never_published_to_the_engine() {
        when(serviceGeoClient.resoudreAxe("Douala", "Yaounde")).thenReturn(Optional.empty());

        service.publier(requeteValide(), clientActeurId, TENANT, "NIVEAU_1");

        ArgumentCaptor<Demande> captor = ArgumentCaptor.forClass(Demande.class);
        verify(demandeRepository).save(captor.capture());
        assertThat(captor.getValue().getAxeId()).isNull();
        assertThat(captor.getValue().getStatut()).isEqualTo(Demande.StatutDemande.AXE_NON_DESSERVI);

        verify(eventPublisher, never()).publierDemandePubliee(any());
    }

    // RG-039/EF-MKT-08 : accepter une des au plus 3 propositions marque
    // celle-ci ACCEPTEE et les autres de la même demande EXPIREE.
    @Test
    void accepting_a_proposal_marks_the_others_of_the_same_request_as_expired() {
        UUID demandeId = UUID.randomUUID();
        UUID prop1 = UUID.randomUUID();
        UUID prop2 = UUID.randomUUID();
        UUID prop3 = UUID.randomUUID();
        when(demandeRepository.findByIdAndTenantId(demandeId, TENANT))
                .thenReturn(Optional.of(Demande.builder().id(demandeId).tenantId(TENANT).build()));

        Proposition p1 = proposition(prop1, demandeId, 1, new BigDecimal("50000"));
        Proposition p2 = proposition(prop2, demandeId, 2, new BigDecimal("52000"));
        Proposition p3 = proposition(prop3, demandeId, 3, new BigDecimal("55000"));
        when(propositionRepository.findByIdAndDemandeId(prop2, demandeId)).thenReturn(Optional.of(p2));
        when(propositionRepository.findByDemandeIdOrderByRangAsc(demandeId)).thenReturn(List.of(p1, p2, p3));
        when(propositionRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        var reponse = service.accepterProposition(demandeId, prop2, TENANT);

        assertThat(reponse.getStatut()).isEqualTo("ACCEPTEE");
        assertThat(p1.getStatut()).isEqualTo(Proposition.Statut.EXPIREE);
        assertThat(p2.getStatut()).isEqualTo(Proposition.Statut.ACCEPTEE);
        assertThat(p3.getStatut()).isEqualTo(Proposition.Statut.EXPIREE);
    }

    // EF-MKT-08 (audit de suivi Mobile) : accepter une proposition liee a
    // une capacite doit reellement reserver cette capacite cote transporteur,
    // pas seulement marquer un statut local.
    @Test
    void accepting_a_proposal_linked_to_a_capacity_reserves_it_for_real() {
        UUID demandeId = UUID.randomUUID();
        UUID propositionId = UUID.randomUUID();
        UUID capaciteId = UUID.randomUUID();
        UUID missionId = UUID.randomUUID();
        when(demandeRepository.findByIdAndTenantId(demandeId, TENANT))
                .thenReturn(Optional.of(Demande.builder().id(demandeId).tenantId(TENANT).poidsTaxableKg(2500.0).build()));
        Proposition p = proposition(propositionId, demandeId, 1, new BigDecimal("50000"));
        p.setCapaciteId(capaciteId);
        p.setMissionId(missionId);
        when(propositionRepository.findByIdAndDemandeId(propositionId, demandeId)).thenReturn(Optional.of(p));
        when(propositionRepository.findByDemandeIdOrderByRangAsc(demandeId)).thenReturn(List.of(p));
        when(propositionRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        service.accepterProposition(demandeId, propositionId, TENANT);

        verify(serviceCapClient).reserver(capaciteId, new BigDecimal("2500.0"), missionId.toString());
    }

    // Un echec de reservation reelle ne doit jamais laisser croire que la
    // proposition a ete acceptee -- l'exception doit remonter avant tout
    // changement de statut, exactement le bug que ce correctif ferme.
    @Test
    void a_reservation_failure_prevents_the_proposal_from_being_marked_accepted() {
        UUID demandeId = UUID.randomUUID();
        UUID propositionId = UUID.randomUUID();
        UUID capaciteId = UUID.randomUUID();
        when(demandeRepository.findByIdAndTenantId(demandeId, TENANT))
                .thenReturn(Optional.of(Demande.builder().id(demandeId).tenantId(TENANT).poidsTaxableKg(2500.0).build()));
        Proposition p = proposition(propositionId, demandeId, 1, new BigDecimal("50000"));
        p.setCapaciteId(capaciteId);
        when(propositionRepository.findByIdAndDemandeId(propositionId, demandeId)).thenReturn(Optional.of(p));
        doThrow(new com.flysoft.fretcorridor.mkt.client.ReservationCapaciteException("indisponible", null))
                .when(serviceCapClient).reserver(any(), any(), any());

        assertThatThrownBy(() -> service.accepterProposition(demandeId, propositionId, TENANT))
                .isInstanceOf(com.flysoft.fretcorridor.mkt.client.ReservationCapaciteException.class);

        assertThat(p.getStatut()).isEqualTo(Proposition.Statut.EN_ATTENTE);
        verify(propositionRepository, never()).saveAll(any());
    }

    @Test
    void accepting_an_already_treated_proposal_is_refused() {
        UUID demandeId = UUID.randomUUID();
        UUID propositionId = UUID.randomUUID();
        when(demandeRepository.findByIdAndTenantId(demandeId, TENANT))
                .thenReturn(Optional.of(Demande.builder().id(demandeId).tenantId(TENANT).build()));
        Proposition dejaAcceptee = proposition(propositionId, demandeId, 1, new BigDecimal("50000"));
        dejaAcceptee.setStatut(Proposition.Statut.ACCEPTEE);
        when(propositionRepository.findByIdAndDemandeId(propositionId, demandeId)).thenReturn(Optional.of(dejaAcceptee));

        assertThatThrownBy(() -> service.accepterProposition(demandeId, propositionId, TENANT))
                .hasMessage("PROPOSITION_DEJA_TRAITEE");
    }

    private Proposition proposition(UUID id, UUID demandeId, int rang, BigDecimal prix) {
        return Proposition.builder()
                .id(id)
                .eventId(UUID.randomUUID())
                .demandeId(demandeId)
                .rang(rang)
                .motifClassement(rang == 1 ? "Affectation optimale L1 (Kuhn-Munkres)" : rang + "e meilleur prix")
                .prixTransport(prix)
                .horodatageEmission(LocalDateTime.now())
                .build();
    }
}
