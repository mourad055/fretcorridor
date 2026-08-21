package com.fretcorridor.opt.oracle;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires de l'increment 21/08 de l'oracle (audit EF-MAT-05/13) :
 * geometrie pure (gabarit avec rotation, volumes, detection de donnees
 * manquantes). Methodes statiques package-private - testables sans contexte
 * Spring ni base.
 */
class OracleChargementServiceTest {

    // ---- rentreDansCaisse : gabarit, toutes rotations essayees ------------

    @Test
    void colisDroit_rentre() {
        assertTrue(OracleChargementService.rentreDansCaisse(
                new double[]{1.0, 0.5, 0.4}, new double[]{2.4, 1.2, 2.0}));
    }

    @Test
    void colisTourne_rentreGraceALaRotation() {
        // 2.3 m de long ne passe pas en longueur (caisse 2.0) mais passe sur
        // la hauteur 2.6 apres permutation - le tri des triplets couvre les
        // 6 orientations sans les enumerer.
        assertTrue(OracleChargementService.rentreDansCaisse(
                new double[]{2.3, 1.0, 0.5}, new double[]{2.0, 1.2, 2.6}));
    }

    @Test
    void colisTropGrand_rejete_quelQueSoitLOrdreDesDimensions() {
        assertFalse(OracleChargementService.rentreDansCaisse(
                new double[]{2.7, 1.0, 0.5}, new double[]{2.0, 1.2, 2.6}));
        assertFalse(OracleChargementService.rentreDansCaisse(
                new double[]{0.5, 2.7, 1.0}, new double[]{2.0, 1.2, 2.6}),
                "Meme colis, dimensions permutees : meme verdict");
    }

    @Test
    void colisAuxLimitesExactes_rentre() {
        assertTrue(OracleChargementService.rentreDansCaisse(
                new double[]{2.0, 1.2, 2.6}, new double[]{2.0, 1.2, 2.6}));
    }

    // ---- dimensionsCaisse / dimensionsLot : donnees manquantes ------------

    @Test
    void caisseIncomplete_retourneNull_verificationSauteeMaisTracee() {
        assertNull(OracleChargementService.dimensionsCaisse(null));
        // Profil complet :
        var profil = new com.fretcorridor.opt.client.ProfilCamionDto(
                2.6, 1.2, 2.0, null, null, null, false);
        assertArrayEquals(new double[]{2.0, 1.2, 2.6},
                OracleChargementService.dimensionsCaisse(profil));
        // Une seule dimension manquante :
        var profilPartiel = new com.fretcorridor.opt.client.ProfilCamionDto(
                2.6, null, 2.0, null, null, null, false);
        assertNull(OracleChargementService.dimensionsCaisse(profilPartiel));
    }

    @Test
    void lotSansDimensions_retourneNull_pasDeVolumeDevine() {
        LotDemande complet = new LotDemande(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "SACS_CIMENT", 10, java.math.BigDecimal.valueOf(500), 1.0, 0.5, 0.3,
                true, false, null);
        assertArrayEquals(new double[]{1.0, 0.5, 0.3},
                OracleChargementService.dimensionsLot(complet));

        LotDemande partiel = new LotDemande(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "TONNEAU", 2, java.math.BigDecimal.valueOf(200), 0.6, null, 0.9,
                true, false, null);
        assertNull(OracleChargementService.dimensionsLot(partiel),
                "Une dimension manquante = verification volumique sautee (tracee), jamais un volume suppose");
    }
}
