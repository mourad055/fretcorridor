package com.fretcorridor.pay.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** FE-PAY-02 : déclenché à la prise en charge, libéré à la clôture — jamais l'inverse. */
class SequestreServiceTest {

    private final SequestreService service = new SequestreService(new FakeSequestrePort());

    @Test
    void declenching_then_liberating_follows_the_expected_lifecycle() {
        service.declencher("mission-1");
        Sequestre libere = service.liberer("mission-1", "tenant-1", "actor-transporteur-1", "preuve-1");

        assertThat(libere.etat()).isEqualTo(SequestreEtat.LIBERE);
    }

    @Test
    void liberating_stamps_the_tenant_and_transporteur_known_only_at_cloture() {
        service.declencher("mission-1");
        Sequestre libere = service.liberer("mission-1", "tenant-1", "actor-transporteur-1", "preuve-1");

        assertThat(libere.tenantId()).isEqualTo("tenant-1");
        assertThat(libere.transporteurId()).isEqualTo("actor-transporteur-1");
    }

    @Test
    void refuses_to_liberate_a_sequestre_that_was_never_declenched() {
        assertThatThrownBy(() -> service.liberer("mission-jamais-declenchee", "tenant-1", "actor-transporteur-1", "preuve-1"))
                .isInstanceOf(SequestreInvalideException.class);
    }

    /** RG-078 : garanti au niveau du record {@link Sequestre} lui-même, pas seulement par convention à l'appel. */
    @Test
    void refuses_to_liberate_without_a_preuve_de_livraison() {
        service.declencher("mission-1");

        assertThatThrownBy(() -> service.liberer("mission-1", "tenant-1", "actor-transporteur-1", ""))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void refuses_to_declench_the_same_mission_twice() {
        service.declencher("mission-1");

        assertThatThrownBy(() -> service.declencher("mission-1"))
                .isInstanceOf(SequestreInvalideException.class);
    }

    @Test
    void refuses_to_reopen_a_liberated_sequestre() {
        service.declencher("mission-1");
        service.liberer("mission-1", "tenant-1", "actor-transporteur-1", "preuve-1");

        assertThatThrownBy(() -> service.declencher("mission-1"))
                .isInstanceOf(SequestreInvalideException.class);
        assertThatThrownBy(() -> service.liberer("mission-1", "tenant-1", "actor-transporteur-1", "preuve-1"))
                .isInstanceOf(SequestreInvalideException.class);
    }
}
