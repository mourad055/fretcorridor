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
        Sequestre libere = service.liberer("mission-1");

        assertThat(libere.etat()).isEqualTo(SequestreEtat.LIBERE);
    }

    @Test
    void refuses_to_liberate_a_sequestre_that_was_never_declenched() {
        assertThatThrownBy(() -> service.liberer("mission-jamais-declenchee"))
                .isInstanceOf(SequestreInvalideException.class);
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
        service.liberer("mission-1");

        assertThatThrownBy(() -> service.declencher("mission-1"))
                .isInstanceOf(SequestreInvalideException.class);
        assertThatThrownBy(() -> service.liberer("mission-1"))
                .isInstanceOf(SequestreInvalideException.class);
    }
}
