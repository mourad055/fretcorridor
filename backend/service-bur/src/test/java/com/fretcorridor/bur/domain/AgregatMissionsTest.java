package com.fretcorridor.bur.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AgregatMissionsTest {

    @Test
    void hides_the_count_when_below_the_threshold() {
        AgregatMissions agregat = AgregatMissions.depuisComptage("axe-1", 2, 3);

        assertThat(agregat.seuilAtteint()).isFalse();
        assertThat(agregat.nombreMissions()).isEmpty();
    }

    @Test
    void exposes_the_count_when_the_threshold_is_reached() {
        AgregatMissions agregat = AgregatMissions.depuisComptage("axe-1", 3, 3);

        assertThat(agregat.seuilAtteint()).isTrue();
        assertThat(agregat.nombreMissions()).contains(3L);
    }

    @Test
    void exposes_the_count_when_above_the_threshold() {
        AgregatMissions agregat = AgregatMissions.depuisComptage("axe-1", 10, 3);

        assertThat(agregat.nombreMissions()).contains(10L);
    }
}
