package com.fretcorridor.pay.domain;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * ENF-FIN-01 (bloquant) : aucun chemin de code ne peut créer une écriture de
 * trésorerie sur un compte FretCorridor (PRD §0, §8.1). Ce test existe avant
 * toute fonctionnalité de paiement (PRD §10.4, règle 3) et échoue la build
 * s'il est violé.
 */
class EnfFin01Test {

    private static final String PACKAGE = "com.fretcorridor.pay";

    @Test
    void no_class_represents_a_fretcorridor_owned_treasury_account() {
        ArchRule regle = noClasses()
                .that().resideInAPackage(PACKAGE + "..")
                .should().haveNameMatching(".*FretCorridor(Compte|Solde|Tresorerie).*")
                .because("FretCorridor n'est jamais dépositaire des fonds (ENF-FIN-01)");

        regle.check(new ClassFileImporter().importPackages(PACKAGE));
    }

    @Test
    void the_type_compte_enum_has_no_fretcorridor_owned_variant() {
        assertThat(TypeCompte.values())
                .as("TypeCompte ne doit jamais exposer de compte propriété de FretCorridor")
                .noneMatch(type -> type.name().contains("FRETCORRIDOR"));
    }

    @Test
    void every_type_compte_variant_designates_a_third_party_account() {
        // Ancrage explicite : si cette liste devient obsolète après l'ajout d'une
        // variante, EnfFin01Test doit être mis à jour consciemment, pas silencieusement.
        assertThat(TypeCompte.values()).containsExactlyInAnyOrder(
                TypeCompte.COMPTE_TRANSPORTEUR,
                TypeCompte.COMPTE_CHARGEUR,
                TypeCompte.COMPTE_SEQUESTRE_PRESTATAIRE
        );
    }
}
