package com.fretcorridor.pay.infrastructure;

import com.fretcorridor.pay.domain.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Preuve que le grand livre miroir et le séquestre persistent réellement en
 * Postgres, et que l'invariant ENF-FIN-02 tient aussi à travers la
 * persistance (pas seulement en mémoire).
 */
@SpringBootTest
@Testcontainers
class GrandLivrePersistenceIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    private GrandLivreService grandLivreService;

    @Autowired
    private SequestreService sequestreService;

    @Autowired
    private GarantieService garantieService;

    @Autowired
    private PaiementEspecesService paiementEspecesService;

    @Autowired
    private LitigeMissionPort litigeMissionPort;

    @Autowired
    private SequestrePort sequestrePort;

    @Autowired
    private ReversementAutomatiqueService reversementAutomatiqueService;

    @Test
    void an_encaissement_is_persisted_and_readable_back() {
        String missionId = "mission-test-" + System.nanoTime();

        grandLivreService.enregistrerEncaissement("tenant-1", missionId, new BigDecimal("500"), "ref-prestataire-1", ModePaiement.VIREMENT);
        sequestreService.declencher(missionId);
        sequestreService.liberer(missionId, "tenant-1", "actor-transporteur-1", "preuve-1");

        EcritureMiroir reversement = grandLivreService.enregistrerReversement("tenant-1", missionId, "actor-transporteur-1", new BigDecimal("450"), "ref-prestataire-2");

        assertThat(reversement.statut()).isEqualTo(StatutEcriture.VALIDE);
        assertThat(reversement.typeCompte()).isEqualTo(TypeCompte.COMPTE_TRANSPORTEUR);
    }

    @Test
    void le_mode_de_paiement_de_l_encaissement_survit_a_un_aller_retour_base_reelle() {
        String missionId = "mission-test-" + System.nanoTime();

        grandLivreService.enregistrerEncaissement("tenant-1", missionId, new BigDecimal("500"), "ref-prestataire-1", ModePaiement.MONNAIE_ELECTRONIQUE);

        EcritureMiroir encaissement = grandLivreService.ecrituresDuTenant("tenant-1").stream()
                .filter(e -> e.missionId().equals(missionId))
                .findFirst().orElseThrow();
        assertThat(encaissement.modePaiement()).isEqualTo(ModePaiement.MONNAIE_ELECTRONIQUE);
    }

    @Test
    void enf_fin_02_holds_across_a_real_database_round_trip() {
        String missionId = "mission-test-" + System.nanoTime();
        grandLivreService.enregistrerEncaissement("tenant-1", missionId, new BigDecimal("100"), "ref-1", ModePaiement.VIREMENT);

        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                        grandLivreService.enregistrerReversement("tenant-1", missionId, "actor-transporteur-1", new BigDecimal("200"), "ref-2"))
                .isInstanceOf(ReversementSansEncaissementException.class);
    }

    @Test
    void a_sequestre_lifecycle_persists_across_the_real_database() {
        String missionId = "mission-test-" + System.nanoTime();

        sequestreService.declencher(missionId);
        Sequestre libere = sequestreService.liberer(missionId, "tenant-1", "actor-transporteur-1", "preuve-1");

        assertThat(libere.etat()).isEqualTo(SequestreEtat.LIBERE);
    }

    /** EF-PAY-06 (terme contractuel) : le reversement sur garantie tient à travers une vraie base, sans encaissement. */
    @Test
    void a_reversement_against_a_persisted_garantie_holds_across_the_real_database() {
        String missionId = "mission-test-" + System.nanoTime();

        garantieService.souscrire("tenant-1", missionId, "garant-bnp", new BigDecimal("300"), "ref-garantie-1");
        sequestreService.declencher(missionId);
        sequestreService.liberer(missionId, "tenant-1", "actor-transporteur-1", "preuve-1");
        EcritureMiroir reversement = grandLivreService.enregistrerReversement("tenant-1", missionId, "actor-transporteur-1", new BigDecimal("300"), "ref-rev-terme");

        assertThat(reversement.nature()).isEqualTo(NatureEcriture.REVERSEMENT);
    }

    /** EF-PAY-07 (S) : la déclaration espèces persiste, et ne contribue jamais au pool de reversement. */
    @Test
    void a_cash_declaration_persists_and_never_unlocks_a_reversement() {
        String missionId = "mission-test-" + System.nanoTime();

        paiementEspecesService.declarer("tenant-1", missionId, new BigDecimal("150"));

        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                        grandLivreService.enregistrerReversement("tenant-1", missionId, "actor-transporteur-1", new BigDecimal("150"), "ref-rev"))
                .isInstanceOf(ReversementSansEncaissementException.class);
    }

    /** EF-PAY-08 : un litige actif persisté suspend le reversement à travers une vraie base. */
    @Test
    void a_persisted_active_litige_suspends_the_reversement_across_the_real_database() {
        String missionId = "mission-test-" + System.nanoTime();
        grandLivreService.enregistrerEncaissement("tenant-1", missionId, new BigDecimal("100"), "ref-1", ModePaiement.VIREMENT);
        litigeMissionPort.enregistrerSiPlusRecent(new LitigeMission(missionId, "tenant-1", true, Instant.now()));

        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                        grandLivreService.enregistrerReversement("tenant-1", missionId, "actor-transporteur-1", new BigDecimal("100"), "ref-rev"))
                .isInstanceOf(ReversementSuspenduPourLitigeException.class);
    }

    /** EF-PAY-08 : l'ordonnanceur reverse réellement une mission éligible, à travers une vraie base (config par défaut : 48h). */
    @Test
    void the_ordonnanceur_reverses_an_eligible_mission_across_the_real_database() {
        String missionId = "mission-test-" + System.nanoTime();
        Instant maintenant = Instant.now();
        grandLivreService.enregistrerEncaissement("tenant-1", missionId, new BigDecimal("100"), "ref-1", ModePaiement.VIREMENT);
        sequestrePort.sauvegarder(new Sequestre(missionId, SequestreEtat.LIBERE,
                maintenant.minus(50, java.time.temporal.ChronoUnit.HOURS), maintenant.minus(49, java.time.temporal.ChronoUnit.HOURS),
                "tenant-1", "actor-transporteur-1", "preuve-1"));

        var reversements = reversementAutomatiqueService.detecterEtReverser(maintenant);

        assertThat(reversements).anyMatch(e -> e.missionId().equals(missionId));
        assertThat(grandLivreService.soldeDisponiblePourReversement(missionId)).isEqualByComparingTo("0");
    }
}
