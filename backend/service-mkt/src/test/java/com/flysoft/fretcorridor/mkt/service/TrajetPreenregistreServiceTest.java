package com.flysoft.fretcorridor.mkt.service;

import com.flysoft.fretcorridor.mkt.dto.TrajetPreenregistreDto;
import com.flysoft.fretcorridor.mkt.entity.CatalogueEmballage;
import com.flysoft.fretcorridor.mkt.entity.Demande;
import com.flysoft.fretcorridor.mkt.entity.TrajetPreenregistre;
import com.flysoft.fretcorridor.mkt.repository.CatalogueEmballageRepository;
import com.flysoft.fretcorridor.mkt.repository.TrajetPreenregistreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TrajetPreenregistreServiceTest {

    private static final String TENANT = "tenant-bgft-douala";

    @Mock private TrajetPreenregistreRepository trajetRepository;
    @Mock private CatalogueEmballageRepository catalogueRepository;

    private TrajetPreenregistreService service;
    private UUID clientActeurId;
    private UUID typeEmballageId;
    private CatalogueEmballage emballage;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new TrajetPreenregistreService(trajetRepository, catalogueRepository);
        clientActeurId = UUID.randomUUID();
        typeEmballageId = UUID.randomUUID();
        emballage = CatalogueEmballage.builder()
                .id(typeEmballageId).nom("Sac de ciment").icone("inventory_2")
                .poidsUnitaireKg(50.0).volumeUnitaireM3(0.03).fragileParDefaut(false)
                .build();
        when(catalogueRepository.findById(typeEmballageId)).thenReturn(Optional.of(emballage));
        when(trajetRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private TrajetPreenregistreDto.EnregistrerRequest requeteValide() {
        var requete = new TrajetPreenregistreDto.EnregistrerRequest();
        requete.setNom("Livraison hebdo bureau");
        requete.setVilleDepart("Douala");
        requete.setVilleArrivee("Yaounde");
        requete.setTypeEmballageId(typeEmballageId);
        requete.setQuantite(10);
        requete.setCategorieMarchandise("STANDARD");
        requete.setModeCollecte("DOMICILE");
        requete.setDestinataireNom("Jean Mballa");
        requete.setDestinataireTelephone("+237600000000");
        return requete;
    }

    @Test
    void enregistrer_saves_the_trip_with_the_authenticated_client_and_resolved_package_type() {
        TrajetPreenregistre trajet = service.enregistrer(requeteValide(), clientActeurId, TENANT);

        assertThat(trajet.getClientActeurId()).isEqualTo(clientActeurId);
        assertThat(trajet.getTenantId()).isEqualTo(TENANT);
        assertThat(trajet.getTypeEmballage()).isEqualTo(emballage);
        assertThat(trajet.getCategorieMarchandise()).isEqualTo(Demande.CategorieMarchandise.STANDARD);
        assertThat(trajet.getModeCollecte()).isEqualTo(Demande.ModeCollecte.DOMICILE);
    }

    @Test
    void enregistrer_rejects_an_unknown_package_type() {
        when(catalogueRepository.findById(typeEmballageId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.enregistrer(requeteValide(), clientActeurId, TENANT))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("TYPE_EMBALLAGE_INTROUVABLE");
    }

    @Test
    void listerMesTrajets_returns_only_the_authenticated_client_trips() {
        TrajetPreenregistre trajet = TrajetPreenregistre.builder()
                .clientActeurId(clientActeurId).tenantId(TENANT).nom("x").villeDepart("Douala").villeArrivee("Yaounde")
                .typeEmballage(emballage).quantite(5).modeCollecte(Demande.ModeCollecte.DOMICILE)
                .destinataireNom("Jean").destinataireTelephone("+237600000000").build();
        when(trajetRepository.findByClientActeurIdAndTenantIdOrderByDateCreationDesc(clientActeurId, TENANT))
                .thenReturn(List.of(trajet));

        assertThat(service.listerMesTrajets(clientActeurId, TENANT)).containsExactly(trajet);
    }

    @Test
    void supprimer_rejects_a_trip_belonging_to_another_client() {
        UUID id = UUID.randomUUID();
        when(trajetRepository.findByIdAndClientActeurIdAndTenantId(id, clientActeurId, TENANT))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.supprimer(id, clientActeurId, TENANT))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("TRAJET_INTROUVABLE");

        ArgumentCaptor<TrajetPreenregistre> captor = ArgumentCaptor.forClass(TrajetPreenregistre.class);
        verify(trajetRepository, org.mockito.Mockito.never()).delete(captor.capture());
    }
}
