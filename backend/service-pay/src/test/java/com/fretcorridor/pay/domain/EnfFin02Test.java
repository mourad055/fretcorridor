package com.fretcorridor.pay.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * ENF-FIN-02 (bloquant) : aucun reversement n'est instruit sans encaissement
 * correspondant enregistré. Existe avant la fonctionnalité de paiement
 * elle-même (PRD §10.4, règle 3).
 */
class EnfFin02Test {

    private final FakeGrandLivrePort grandLivrePort = new FakeGrandLivrePort();
    private final GrandLivreService service = new GrandLivreService(grandLivrePort, new FakeGarantiePort());

    @Test
    void refuses_a_reversement_when_no_encaissement_was_ever_recorded() {
        assertThatThrownBy(() -> service.enregistrerReversement("tenant-1", "mission-1", "actor-transporteur-1", new BigDecimal("100"), "ref-1"))
                .isInstanceOf(ReversementSansEncaissementException.class);
    }

    @Test
    void refuses_a_reversement_exceeding_the_recorded_encaissement() {
        service.enregistrerEncaissement("tenant-1", "mission-1", new BigDecimal("100"), "ref-enc", ModePaiement.VIREMENT);

        assertThatThrownBy(() -> service.enregistrerReversement("tenant-1", "mission-1", "actor-transporteur-1", new BigDecimal("150"), "ref-rev"))
                .isInstanceOf(ReversementSansEncaissementException.class);
    }

    @Test
    void allows_a_reversement_covered_by_a_prior_encaissement() {
        service.enregistrerEncaissement("tenant-1", "mission-1", new BigDecimal("100"), "ref-enc", ModePaiement.VIREMENT);

        EcritureMiroir reversement = service.enregistrerReversement("tenant-1", "mission-1", "actor-transporteur-1", new BigDecimal("90"), "ref-rev");

        assertThat(reversement.nature()).isEqualTo(NatureEcriture.REVERSEMENT);
        assertThat(reversement.montant()).isEqualByComparingTo("90");
        assertThat(reversement.beneficiaireId()).isEqualTo("actor-transporteur-1");
    }

    @Test
    void refuses_a_second_reversement_once_the_encaissement_is_fully_consumed() {
        service.enregistrerEncaissement("tenant-1", "mission-1", new BigDecimal("100"), "ref-enc", ModePaiement.VIREMENT);
        service.enregistrerReversement("tenant-1", "mission-1", "actor-transporteur-1", new BigDecimal("100"), "ref-rev-1");

        assertThatThrownBy(() -> service.enregistrerReversement("tenant-1", "mission-1", "actor-transporteur-1", new BigDecimal("1"), "ref-rev-2"))
                .isInstanceOf(ReversementSansEncaissementException.class);
    }

    @Test
    void an_encaissement_never_targets_a_fretcorridor_account_but_the_provider_escrow() {
        EcritureMiroir encaissement = service.enregistrerEncaissement("tenant-1", "mission-1", new BigDecimal("100"), "ref-enc", ModePaiement.VIREMENT);

        assertThat(encaissement.typeCompte()).isEqualTo(TypeCompte.COMPTE_SEQUESTRE_PRESTATAIRE);
        assertThat(encaissement.beneficiaireId()).isNull();
    }
}
