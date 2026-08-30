package com.fretcorridor.opt.domain;

import com.fretcorridor.opt.oracle.LotDemande;
import com.fretcorridor.opt.oracle.LotDemandeRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests de CompatibiliteMarchandisesService (point 5 : matrice
 * d'incompatibilite marchandises). Met l'accent sur le fait que la REVLUE
 * est pilotee par configuration (jamais codee en dur) et que la regle est
 * dure (false = exclusion), pas une penalite.
 */
class CompatibiliteMarchandisesServiceTest {

    private final AffectationRepository affectationRepository = mock(AffectationRepository.class);
    private final LotDemandeRepository lotDemandeRepository = mock(LotDemandeRepository.class);

    private LotDemande lot(String typeCatalogue, String classeDanger) {
        return new LotDemande(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                typeCatalogue, 1, BigDecimal.TEN, 1.0, 1.0, 1.0, true, false, classeDanger);
    }

    @Test
    void sans_deux_lots_dangereux_la_demande_est_compatible() {
        CompatibiliteMarchandisesService service = new CompatibiliteMarchandisesService(
                affectationRepository, lotDemandeRepository, true, List.of());

        UUID candidate = UUID.randomUUID();
        UUID existante = UUID.randomUUID();
        when(lotDemandeRepository.findByDemandeId(candidate)).thenReturn(List.of(lot("bois", null)));
        when(lotDemandeRepository.findByDemandeId(existante)).thenReturn(List.of(lot("bois", null)));

        assertTrue(service.compatible(candidate, List.of(existante)));
    }

    @Test
    void deux_lots_dangereux_differents_sont_incompatibles_anti_groupage() {
        CompatibiliteMarchandisesService service = new CompatibiliteMarchandisesService(
                affectationRepository, lotDemandeRepository, true, List.of());

        UUID candidate = UUID.randomUUID();
        UUID existante = UUID.randomUUID();
        when(lotDemandeRepository.findByDemandeId(candidate)).thenReturn(List.of(lot("carton", "6 - toxique")));
        when(lotDemandeRepository.findByDemandeId(existante)).thenReturn(List.of(lot("fut", "3 - inflammable")));

        assertFalse(service.compatible(candidate, List.of(existante)));
    }

    @Test
    void anti_groupage_desactive_laisse_cohabiter_les_dangereux() {
        CompatibiliteMarchandisesService service = new CompatibiliteMarchandisesService(
                affectationRepository, lotDemandeRepository, false, List.of());

        UUID candidate = UUID.randomUUID();
        UUID existante = UUID.randomUUID();
        when(lotDemandeRepository.findByDemandeId(candidate)).thenReturn(List.of(lot("carton", "6")));
        when(lotDemandeRepository.findByDemandeId(existante)).thenReturn(List.of(lot("fut", "3")));

        assertTrue(service.compatible(candidate, List.of(existante)));
    }

    @Test
    void paire_de_la_matrice_est_incompatible_dans_les_deux_sens() {
        CompatibiliteMarchandisesService service = new CompatibiliteMarchandisesService(
                affectationRepository, lotDemandeRepository, true, List.of("miroirs,graviers"));

        UUID candidate = UUID.randomUUID();
        UUID existante = UUID.randomUUID();
        when(lotDemandeRepository.findByDemandeId(candidate)).thenReturn(List.of(lot("miroirs", null)));
        when(lotDemandeRepository.findByDemandeId(existante)).thenReturn(List.of(lot("graviers", null)));

        assertFalse(service.compatible(candidate, List.of(existante)));

        // Sens inverse (matrice symetrique) : meme exclusion.
        UUID candidate2 = UUID.randomUUID();
        UUID existante2 = UUID.randomUUID();
        when(lotDemandeRepository.findByDemandeId(candidate2)).thenReturn(List.of(lot("graviers", null)));
        when(lotDemandeRepository.findByDemandeId(existante2)).thenReturn(List.of(lot("Miroirs", null)));
        assertFalse(service.compatible(candidate2, List.of(existante2)));
    }

    @Test
    void sans_config_aucune_paire_nexclut() {
        // Matrice vide = permissif : "bois" et "miroirs" ne sont pas lies par
        // defaut (leur incompatibilite n'existe QUE si config) - comportement
        // historique preserve quand le deployement ne configure rien.
        CompatibiliteMarchandisesService service = new CompatibiliteMarchandisesService(
                affectationRepository, lotDemandeRepository, true, List.of());

        UUID candidate = UUID.randomUUID();
        UUID existante = UUID.randomUUID();
        when(lotDemandeRepository.findByDemandeId(candidate)).thenReturn(List.of(lot("miroirs", null)));
        when(lotDemandeRepository.findByDemandeId(existante)).thenReturn(List.of(lot("bois", null)));

        assertTrue(service.compatible(candidate, List.of(existante)));
    }

    @Test
    void demande_sans_detail_lot_est_permissive() {
        CompatibiliteMarchandisesService service = new CompatibiliteMarchandisesService(
                affectationRepository, lotDemandeRepository, true, List.of("miroirs,bois"));

        UUID candidate = UUID.randomUUID();
        UUID existante = UUID.randomUUID();
        when(lotDemandeRepository.findByDemandeId(candidate)).thenReturn(List.of());
        when(lotDemandeRepository.findByDemandeId(existante)).thenReturn(List.of(lot("bois", null)));

        assertTrue(service.compatible(candidate, List.of(existante)));

        // compatibleAvecDemandesDeLaCapacite : verifie la vue confirmee.
        when(affectationRepository.findByCapaciteIdAndStatut(any(), any())).thenReturn(List.of());
        assertTrue(service.compatibleAvecDemandesDeLaCapacite(candidate, UUID.randomUUID()));
    }
}