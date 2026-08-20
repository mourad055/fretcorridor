package com.fretcorridor.pay.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * EF-PAY-08, CDC UC-PAY-02 A1 : un litige actif sur la mission suspend tout
 * reversement, même intégralement couvert par un encaissement réel ou une
 * garantie. Rétabli automatiquement à la clôture du litige.
 */
class EfPay08Test {

    private final FakeGrandLivrePort grandLivrePort = new FakeGrandLivrePort();
    private final FakeGarantiePort garantiePort = new FakeGarantiePort();
    private final FakeLitigeMissionPort litigeMissionPort = new FakeLitigeMissionPort();
    private final FakeSequestrePort sequestrePort = new FakeSequestrePort();
    private final GrandLivreService service = new GrandLivreService(grandLivrePort, garantiePort, litigeMissionPort, sequestrePort);

    private void libererAvecPreuve(String missionId) {
        sequestrePort.sauvegarder(new Sequestre(missionId, SequestreEtat.LIBERE, Instant.now(), Instant.now(),
                "tenant-1", "actor-transporteur-1", "preuve-1"));
    }

    @Test
    void refuses_a_reversement_when_a_litige_is_active_even_with_full_encaissement() {
        service.enregistrerEncaissement("tenant-1", "mission-1", new BigDecimal("100"), "ref-enc", ModePaiement.VIREMENT);
        litigeMissionPort.enregistrerSiPlusRecent(new LitigeMission("mission-1", "tenant-1", true, Instant.now()));

        assertThatThrownBy(() -> service.enregistrerReversement("tenant-1", "mission-1", "actor-transporteur-1", new BigDecimal("100"), "ref-rev"))
                .isInstanceOf(ReversementSuspenduPourLitigeException.class);
    }

    @Test
    void allows_a_reversement_once_the_litige_is_closed() {
        service.enregistrerEncaissement("tenant-1", "mission-1", new BigDecimal("100"), "ref-enc", ModePaiement.VIREMENT);
        libererAvecPreuve("mission-1");
        Instant ouverture = Instant.now();
        litigeMissionPort.enregistrerSiPlusRecent(new LitigeMission("mission-1", "tenant-1", true, ouverture));
        litigeMissionPort.enregistrerSiPlusRecent(new LitigeMission("mission-1", "tenant-1", false, ouverture.plus(1, ChronoUnit.HOURS)));

        EcritureMiroir reversement = service.enregistrerReversement("tenant-1", "mission-1", "actor-transporteur-1", new BigDecimal("100"), "ref-rev");

        assertThat(reversement.nature()).isEqualTo(NatureEcriture.REVERSEMENT);
    }

    @Test
    void a_stale_late_arriving_litige_event_never_overrides_a_more_recent_cloture() {
        Instant ouverture = Instant.now();
        litigeMissionPort.enregistrerSiPlusRecent(new LitigeMission("mission-1", "tenant-1", true, ouverture));
        litigeMissionPort.enregistrerSiPlusRecent(new LitigeMission("mission-1", "tenant-1", false, ouverture.plus(1, ChronoUnit.HOURS)));
        // Rejeu tardif de l'événement d'ouverture (Kafka ne garantit pas l'ordre) : doit être ignoré.
        litigeMissionPort.enregistrerSiPlusRecent(new LitigeMission("mission-1", "tenant-1", true, ouverture));

        assertThat(litigeMissionPort.parMission("mission-1")).get()
                .extracting(LitigeMission::actif).isEqualTo(false);
    }

    @Test
    void a_mission_with_no_litige_reported_reverses_normally() {
        service.enregistrerEncaissement("tenant-1", "mission-1", new BigDecimal("100"), "ref-enc", ModePaiement.VIREMENT);
        libererAvecPreuve("mission-1");

        EcritureMiroir reversement = service.enregistrerReversement("tenant-1", "mission-1", "actor-transporteur-1", new BigDecimal("100"), "ref-rev");

        assertThat(reversement.nature()).isEqualTo(NatureEcriture.REVERSEMENT);
    }
}
