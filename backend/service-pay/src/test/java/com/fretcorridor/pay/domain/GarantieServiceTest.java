package com.fretcorridor.pay.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** EF-PAY-06 (terme contractuel) : souscription de la garantie tierce. */
class GarantieServiceTest {

    private final GarantieService service = new GarantieService(new FakeGarantiePort());

    @Test
    void souscrit_une_garantie_pour_une_mission() {
        Garantie garantie = service.souscrire("tenant-1", "mission-1", "garant-bnp", new BigDecimal("500"), "ref-garantie-1");

        assertThat(garantie.missionId()).isEqualTo("mission-1");
        assertThat(garantie.garantId()).isEqualTo("garant-bnp");
        assertThat(garantie.montant()).isEqualByComparingTo("500");
    }

    @Test
    void refuses_a_second_garantie_for_the_same_mission() {
        service.souscrire("tenant-1", "mission-1", "garant-bnp", new BigDecimal("500"), "ref-garantie-1");

        assertThatThrownBy(() -> service.souscrire("tenant-1", "mission-1", "garant-bnp", new BigDecimal("500"), "ref-garantie-2"))
                .isInstanceOf(GarantieInvalideException.class);
    }
}
