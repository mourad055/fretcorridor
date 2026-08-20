package com.fretcorridor.pay.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** EF-PAY-06, Item B de docs/DEPENDANCES_MOBILE_PHASE4.md : moyen de paiement choisi par le chargeur, CDC UC-PAY-01 étape 2. */
class ModePaiementChoisiServiceTest {

    private final ModePaiementChoisiService service = new ModePaiementChoisiService(new FakeModePaiementChoisiPort());

    @Test
    void enregistre_le_moyen_choisi_pour_une_mission() {
        ModePaiementChoisi choix = service.choisir("tenant-1", "mission-1", ModePaiement.MONNAIE_ELECTRONIQUE, Instant.now());

        assertThat(choix.missionId()).isEqualTo("mission-1");
        assertThat(choix.modePaiement()).isEqualTo(ModePaiement.MONNAIE_ELECTRONIQUE);
    }

    @Test
    void refuses_a_second_choice_for_the_same_mission() {
        service.choisir("tenant-1", "mission-1", ModePaiement.MONNAIE_ELECTRONIQUE, Instant.now());

        assertThatThrownBy(() -> service.choisir("tenant-1", "mission-1", ModePaiement.VIREMENT, Instant.now()))
                .isInstanceOf(ModePaiementDejaChoisiException.class);
    }

    @Test
    void returns_empty_when_no_choice_was_ever_made() {
        assertThat(service.pour("mission-jamais-choisie")).isEmpty();
    }

    @Test
    void reading_the_choice_never_changes_it() {
        service.choisir("tenant-1", "mission-1", ModePaiement.TERME_CONTRACTUEL, Instant.now());

        assertThat(service.pour("mission-1")).hasValueSatisfying(c ->
                assertThat(c.modePaiement()).isEqualTo(ModePaiement.TERME_CONTRACTUEL));
    }
}
