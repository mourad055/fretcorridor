package com.fretcorridor.pay.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * EF-PAY-05 (bloquant) : une notification entrante non signée correctement
 * n'est jamais traitée, et une notification rejouée (même clé d'idempotence)
 * n'écrit jamais deux fois au grand livre.
 */
class NotificationPrestataireServiceTest {

    private final FakeGrandLivrePort grandLivrePort = new FakeGrandLivrePort();
    private final GrandLivreService grandLivreService = new GrandLivreService(grandLivrePort);
    private final FakeNotificationIdempotencePort idempotencePort = new FakeNotificationIdempotencePort();

    private final NotificationEncaissement notification =
            new NotificationEncaissement("tenant-1", "mission-1", new BigDecimal("100"), "ref-prestataire-1");

    @Test
    void refuses_a_notification_whose_signature_does_not_match() {
        NotificationPrestataireService service = new NotificationPrestataireService(
                grandLivreService, idempotencePort, (corpsBrut, signature) -> false);

        assertThatThrownBy(() -> service.traiter("{...}", "signature-forgee", "idem-1", notification))
                .isInstanceOf(SignatureInvalideException.class);

        assertThat(grandLivrePort.parMission("mission-1")).isEmpty();
    }

    @Test
    void records_an_encaissement_for_a_correctly_signed_new_notification() {
        NotificationPrestataireService service = new NotificationPrestataireService(
                grandLivreService, idempotencePort, (corpsBrut, signature) -> true);

        NotificationPrestataireService.Resultat resultat = service.traiter("{...}", "signature-valide", "idem-1", notification);

        assertThat(resultat).isEqualTo(NotificationPrestataireService.Resultat.TRAITEE);
        assertThat(grandLivrePort.parMission("mission-1")).hasSize(1);
        assertThat(grandLivrePort.parMission("mission-1").get(0).nature()).isEqualTo(NatureEcriture.ENCAISSEMENT);
    }

    @Test
    void a_replayed_notification_with_the_same_idempotency_key_is_not_recorded_twice() {
        NotificationPrestataireService service = new NotificationPrestataireService(
                grandLivreService, idempotencePort, (corpsBrut, signature) -> true);

        service.traiter("{...}", "signature-valide", "idem-1", notification);
        NotificationPrestataireService.Resultat second = service.traiter("{...}", "signature-valide", "idem-1", notification);

        assertThat(second).isEqualTo(NotificationPrestataireService.Resultat.DEJA_TRAITEE);
        assertThat(grandLivrePort.parMission("mission-1")).hasSize(1);
    }

    @Test
    void a_forged_replay_is_rejected_before_the_idempotence_check_even_matters() {
        NotificationPrestataireService service = new NotificationPrestataireService(
                grandLivreService, idempotencePort, (corpsBrut, signature) -> true);
        service.traiter("{...}", "signature-valide", "idem-1", notification);

        NotificationPrestataireService serviceAttaquant = new NotificationPrestataireService(
                grandLivreService, idempotencePort, (corpsBrut, signature) -> false);

        assertThatThrownBy(() -> serviceAttaquant.traiter("{...}", "signature-forgee", "idem-2", notification))
                .isInstanceOf(SignatureInvalideException.class);
        assertThat(grandLivrePort.parMission("mission-1")).hasSize(1);
    }

    private static class FakeNotificationIdempotencePort implements NotificationIdempotencePort {
        private final Set<String> traitees = new HashSet<>();

        @Override
        public boolean dejaTraitee(String idempotenceKey) {
            return traitees.contains(idempotenceKey);
        }

        @Override
        public void marquerTraitee(String idempotenceKey) {
            traitees.add(idempotenceKey);
        }
    }
}
