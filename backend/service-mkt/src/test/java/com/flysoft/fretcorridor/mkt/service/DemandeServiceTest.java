package com.flysoft.fretcorridor.mkt.service;

import com.flysoft.fretcorridor.mkt.client.ServiceGeoClient;
import com.flysoft.fretcorridor.mkt.dto.DemandeDto;
import com.flysoft.fretcorridor.mkt.entity.CatalogueEmballage;
import com.flysoft.fretcorridor.mkt.entity.Demande;
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

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DemandeServiceTest {

    @Mock private DemandeRepository demandeRepository;
    @Mock private CatalogueEmballageRepository catalogueRepository;
    @Mock private MktEventPublisher eventPublisher;
    @Mock private PropositionRepository propositionRepository;
    @Mock private ServiceGeoClient serviceGeoClient;

    private DemandeService service;
    private UUID clientActeurId;
    private UUID typeEmballageId;
    private static final String TENANT = "tenant-bgft-douala";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new DemandeService(demandeRepository, catalogueRepository, eventPublisher, propositionRepository, serviceGeoClient);
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
        when(serviceGeoClient.resoudreAxe("Douala", "Yaounde")).thenReturn(Optional.of(axeId));

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
}
