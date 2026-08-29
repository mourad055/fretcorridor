package com.fretcorridor.opt.domain;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Diffusion-course (plan de reorientation, partie Chauffeur point 2) :
 * liste d'exclusion des transporteurs ayant refuse une demande sur
 * DemandeEnAttente - cumulative, sans doublon, consommee par
 * MatchingCycleService pour ecarter leurs capacites du prochain cycle.
 */
class DemandeEnAttenteExclusionTest {

    private DemandeEnAttente nouvelleDemande() {
        return new DemandeEnAttente(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), Map.of(),
                null, null, null, null, null,
                null, null, null, null, null, null,
                null, null);
    }

    @Test
    void sansRefus_pasDeTransporteurExclu() {
        assertNull(nouvelleDemande().getTransporteursExclus());
    }

    @Test
    void exclureTransporteur_cumuleSansDoublon() {
        DemandeEnAttente demande = nouvelleDemande();
        UUID t1 = UUID.randomUUID();
        UUID t2 = UUID.randomUUID();

        demande.exclureTransporteur(t1);
        demande.exclureTransporteur(t2);
        demande.exclureTransporteur(t1); // doublon ignore

        assertEquals(2, demande.getTransporteursExclus().size());
        assertTrue(demande.getTransporteursExclus().contains(t1));
        assertTrue(demande.getTransporteursExclus().contains(t2));
    }

    @Test
    void exclureTransporteur_nullIgnore_sansCreerListe() {
        DemandeEnAttente demande = nouvelleDemande();
        demande.exclureTransporteur(null);
        assertNull(demande.getTransporteursExclus());
    }

    @Test
    void remettreEnFile_conserveLesExclusionsEtRepasseNonTraitee() {
        DemandeEnAttente demande = nouvelleDemande();
        demande.marquerTraitee();
        UUID t = UUID.randomUUID();
        demande.exclureTransporteur(t);
        demande.remettreEnFile();

        assertFalse(demande.isTraitee(), "La demande doit redevenir eligible au prochain cycle");
        assertTrue(demande.getTransporteursExclus().contains(t),
                "L'exclusion doit survivre a la remise en file (pas de re-diffusion au refuseur)");
    }
}
