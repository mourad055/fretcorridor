package com.fretcorridor.pay.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * EF-PAY-07 (S) : mode espèces, mode dégradé sans séquestre ni garantie
 * (CDC §7.6, UC-PAY-01 A3) — signalé explicitement, jamais confondu avec un
 * encaissement réel qui autoriserait un reversement.
 */
class EfPay07Test {

    private final FakeDeclarationEspecesPort declarationEspecesPort = new FakeDeclarationEspecesPort();
    private final PaiementEspecesService service = new PaiementEspecesService(declarationEspecesPort);

    @Test
    void declares_a_cash_payment_for_a_mission() {
        DeclarationEspeces declaration = service.declarer("tenant-1", "mission-1", new BigDecimal("150"));

        assertThat(declaration.missionId()).isEqualTo("mission-1");
        assertThat(declaration.montant()).isEqualByComparingTo("150");
    }

    @Test
    void refuses_a_second_declaration_for_the_same_mission() {
        service.declarer("tenant-1", "mission-1", new BigDecimal("150"));

        assertThatThrownBy(() -> service.declarer("tenant-1", "mission-1", new BigDecimal("150")))
                .isInstanceOf(DeclarationEspecesInvalideException.class);
    }

    @Test
    void lists_all_cash_payments_of_a_tenant() {
        service.declarer("tenant-1", "mission-1", new BigDecimal("150"));
        service.declarer("tenant-1", "mission-2", new BigDecimal("200"));
        service.declarer("tenant-2", "mission-3", new BigDecimal("300"));

        assertThat(service.paiementsDuTenant("tenant-1")).hasSize(2);
    }

    /**
     * Anti-régression : une déclaration espèces n'est ni une écriture de
     * grand livre ni une garantie — elle ne doit jamais autoriser un
     * reversement, contrairement à l'encaissement réel (EF-PAY-06) ou à la
     * garantie tierce (EF-PAY-06 terme). Le transporteur payé en espèces à
     * l'enlèvement n'a rien à recevoir de FretCorridor pour cette mission.
     */
    @Test
    void a_cash_declaration_never_authorizes_a_reversement_through_the_grand_livre() {
        service.declarer("tenant-1", "mission-1", new BigDecimal("150"));

        GrandLivreService grandLivreService = new GrandLivreService(new FakeGrandLivrePort(), new FakeGarantiePort(), new FakeLitigeMissionPort());

        assertThatThrownBy(() -> grandLivreService.enregistrerReversement(
                "tenant-1", "mission-1", "actor-transporteur-1", new BigDecimal("150"), "ref-rev"))
                .isInstanceOf(ReversementSansEncaissementException.class);
    }
}
