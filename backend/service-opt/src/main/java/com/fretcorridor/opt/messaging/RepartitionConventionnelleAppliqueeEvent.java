package com.fretcorridor.opt.messaging;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Payload Kafka de RepartitionConventionnelleAppliquee (EF-GEO-05/RG-052,
 * Phase 4, CDC S9.9 et S3.3 correction C2). Publie par OPT quand une
 * Affectation confirmee porte sur un axe transfrontalier soumis a
 * convention bilaterale - consomme par service-pay (Web).
 * BROUILLON - contrat non encore valide avec Personne 2, cf
 * shared-contracts/asyncapi/events/repartition-conventionnelle-appliquee.yaml.
 *
 * DETECTION PHASE 4 ACTUELLE (limitation connue, README Phase 4 S3.3) :
 * uniquement via la presence de Axe.parametres.conventionRepartition (lu
 * par ServiceGeoClient.axeParId) - Hub n'a pas encore de champ pays. Un
 * axe VOULU transfrontalier mais dont la convention n'a pas ete renseignee
 * est aujourd'hui indiscernable d'un axe intra-camerounais normal (les
 * deux cas : conventionRepartition absent). Consequence assumee : cette
 * version ne publie JAMAIS modeDegrade=true - seulement l'evenement quand
 * la convention est presente, ou rien du tout sinon (RG-052 : "en trafic
 * intra-camerounais, aucune cle ne s'applique" est le comportement attendu
 * par defaut, pas un mode degrade). A revoir des que Hub.pays existe.
 */
public record RepartitionConventionnelleAppliqueeEvent(
        UUID eventId,
        UUID missionId,
        UUID axeId,
        String conventionCode,
        Map<String, Double> partsPourcent,
        Instant dateApplication,
        boolean modeDegrade
) {
}
