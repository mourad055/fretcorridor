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

    @Test
    void an_encaissement_is_persisted_and_readable_back() {
        String missionId = "mission-test-" + System.nanoTime();

        grandLivreService.enregistrerEncaissement("tenant-1", missionId, new BigDecimal("500"), "ref-prestataire-1", ModePaiement.VIREMENT);

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
        Sequestre libere = sequestreService.liberer(missionId);

        assertThat(libere.etat()).isEqualTo(SequestreEtat.LIBERE);
    }
}
