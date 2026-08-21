package com.fretcorridor.opt.domain;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires du fix RG-105 (fenetre adaptative par axe, audit 21/08) -
 * logique pure extraite en methodes statiques package-private pour etre
 * testable sans contexte Spring ni mocks (premier test de MatchingCycleService,
 * signale sans couverture par l'audit).
 */
class MatchingCycleServiceTest {

    // ---- extraireFenetreParametreAxe : parametre d'axe RG-105 -------------

    @Test
    void fenetreAxe_absente_retourneNull_leDefautGlobalPrendLeRelais() {
        assertNull(MatchingCycleService.extraireFenetreParametreAxe(null));
        assertNull(MatchingCycleService.extraireFenetreParametreAxe(Map.of()));
        assertNull(MatchingCycleService.extraireFenetreParametreAxe(
                Map.of("rayonAppariementKm", 25)));
    }

    @Test
    void fenetreAxe_presenteNumerique_estLue() {
        assertEquals(120.0, MatchingCycleService.extraireFenetreParametreAxe(
                Map.of("fenetreTraitementSecondes", 120)));
        assertEquals(90.5, MatchingCycleService.extraireFenetreParametreAxe(
                Map.of("fenetreTraitementSecondes", 90.5)));
    }

    @Test
    void fenetreAxe_typeInattendu_retourneNull_pasDException() {
        assertNull(MatchingCycleService.extraireFenetreParametreAxe(
                Map.of("fenetreTraitementSecondes", "deux-minutes")));
    }

    // ---- ageFenetreAtteint : eligibilite d'un element a la file -----------

    @Test
    void elementFraichementArrive_pasEligible_siFenetreNonEcoulee() {
        Instant maintenant = Instant.now();
        Instant arriveIlYA10s = maintenant.minus(Duration.ofSeconds(10));
        assertFalse(MatchingCycleService.ageFenetreAtteint(arriveIlYA10s, maintenant, 60.0),
                "Un element de 10 s d'age ne doit pas matcher sur un axe a fenetre de 60 s");
    }

    @Test
    void elementAssezAge_eligible() {
        Instant maintenant = Instant.now();
        Instant arriveIlYA61s = maintenant.minus(Duration.ofSeconds(61));
        assertTrue(MatchingCycleService.ageFenetreAtteint(arriveIlYA61s, maintenant, 60.0));
    }

    @Test
    void fenetreZero_ouDateNulle_toujoursEligible_comportementHistorique() {
        Instant maintenant = Instant.now();
        assertTrue(MatchingCycleService.ageFenetreAtteint(maintenant, maintenant, 0.0),
                "Fenetre 0 = comportement historique : rien n'attend");
        assertTrue(MatchingCycleService.ageFenetreAtteint(null, maintenant, 60.0),
                "Donnee manquante : on n'exclut jamais defensivement");
    }

    // ---- ajusterFenetre : adaptation selon le volume observe (RG-105) -----

    @Test
    void lotDUnElement_fenetreAllongee_dispatchGloutonDeguiseAEviter() {
        double ajustee = MatchingCycleService.ajusterFenetre(30.0, 1, 2.0, 0.0, 3600.0);
        assertEquals(60.0, ajustee,
                "Lot d'un element = 'dispatch glouton deguise' (CDC S8.5.2) : la fenetre doit doubler");
    }

    @Test
    void lotRiche_fenetreRaccourcie() {
        double ajustee = MatchingCycleService.ajusterFenetre(30.0, 3, 2.0, 0.0, 3600.0);
        assertEquals(15.0, ajustee);
    }

    @Test
    void bornesToujoursRespectees_memeApresPlusieursAjustements() {
        double fenetre = 1800.0;
        for (int i = 0; i < 20; i++) {
            fenetre = MatchingCycleService.ajusterFenetre(fenetre, 1, 2.0, 0.0, 3600.0);
        }
        assertTrue(fenetre <= 3600.0, "Borne max doit plafonner l'allongement");

        fenetre = 10.0;
        for (int i = 0; i < 20; i++) {
            fenetre = MatchingCycleService.ajusterFenetre(fenetre, 4, 2.0, 0.0, 3600.0);
        }
        assertTrue(fenetre >= 0.0, "Borne min doit plancher le raccourcissement");
    }
}
