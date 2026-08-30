package com.fretcorridor.opt.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class OptEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(OptEventPublisher.class);
    private static final String TOPIC_PROPOSITION_EMISE = "proposition-emise";
    private static final String TOPIC_AFFECTATION_CONFIRMEE = "affectation-confirmee";
    private static final String TOPIC_PROPOSITION_RETOUR_A_VIDE = "proposition-retour-a-vide";
    private static final String TOPIC_TOURNEE_CONSTITUEE = "tournee-constituee";
    private static final String TOPIC_PLAN_CHARGEMENT_CONFIRME = "plan-chargement-confirme";
    private static final String TOPIC_REPARTITION_CONVENTIONNELLE = "repartition-conventionnelle-appliquee";
    private static final String TOPIC_PROPOSITION_DIFFUSEE_CHAUFFEUR = "proposition-diffusee-chauffeur";
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public OptEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publierPropositionEmise(PropositionEmiseEvent event) {
        // ATTENTION (piege classique KafkaTemplate) : send() N'EST PAS purement
        // asynchrone - l'appel bloque le thread appelant jusqu'a max.block.ms
        // (defaut 60s) si les metadonnees du topic ne sont pas encore en cache,
        // et LANCE l'exception directement (avant meme de retourner un Future)
        // si ce delai expire. Sans ce try/catch, une simple absence de topic
        // cote broker ferait remonter un 500 sur tout le cycle L1 - contraire a
        // ENF-DIS-04 (une notification qui echoue ne doit jamais bloquer le
        // moteur). La degradation gracieuse doit donc englober l'appel send()
        // lui-meme, pas seulement son whenComplete().
        try {
            String cle = event.demandeId().toString();
            kafkaTemplate.send(TOPIC_PROPOSITION_EMISE, cle, event)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            log.debug("PropositionEmise publiee - demande={}, rang={}, offset={}",
                                    event.demandeId(), event.rang(),
                                    result.getRecordMetadata().offset());
                        } else {
                            log.error("Echec publication PropositionEmise (callback async) - demande={}",
                                    event.demandeId(), ex);
                        }
                    });
        } catch (Exception exceptionBlocante) {
            log.error("Echec publication PropositionEmise (send() bloquant, ex. metadonnees "
                    + "topic indisponibles) - demande={} - cycle L1 non interrompu (ENF-DIS-04)",
                    event.demandeId(), exceptionBlocante);
        }
    }

    public void publierPropositionDiffuseeChauffeur(PropositionDiffuseeChauffeurEvent event) {
        try {
            // Cle de partition = transporteurId : un chauffeur recoit toutes
            // ses propositions dans l'ordre, et la perte d'une cle n'eparpille
            // pas ses propositions sur plusieurs partitions non triees.
            String cle = event.transporteurId() != null ? event.transporteurId().toString() : event.affectationId().toString();
            kafkaTemplate.send(TOPIC_PROPOSITION_DIFFUSEE_CHAUFFEUR, cle, event)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            log.debug("PropositionDiffuseeChauffeur publiee - transporteur={}, affectation={}, offset={}",
                                    event.transporteurId(), event.affectationId(),
                                    result.getRecordMetadata().offset());
                        } else {
                            log.error("Echec publication PropositionDiffuseeChauffeur (callback async) - "
                                    + "transporteur={}, affectation={}",
                                    event.transporteurId(), event.affectationId(), ex);
                        }
                    });
        } catch (Exception exceptionBlocante) {
            log.error("Echec publication PropositionDiffuseeChauffeur (send() bloquant) - transporteur={}, "
                    + "affectation={} - cycle L1 non interrompu (ENF-DIS-04)",
                    event.transporteurId(), event.affectationId(), exceptionBlocante);
        }
    }

    public void publierAffectationConfirmee(AffectationConfirmeeEvent event) {
        // Meme piege/meme remede que publierPropositionEmise ci-dessus.

        try {
            String cle = event.missionId().toString();
            kafkaTemplate.send(TOPIC_AFFECTATION_CONFIRMEE, cle, event)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            log.info("AffectationConfirmee publiee - mission={}, offset={}",
                                    event.missionId(), result.getRecordMetadata().offset());
                        } else {
                            log.error("Echec publication AffectationConfirmee (callback async) - mission={}",
                                    event.missionId(), ex);
                        }
                    });
        } catch (Exception exceptionBlocante) {
            log.error("Echec publication AffectationConfirmee (send() bloquant, ex. metadonnees "
                    + "topic indisponibles) - mission={} - cycle L1 non interrompu (ENF-DIS-04)",
                    event.missionId(), exceptionBlocante);
        }
    }

    /** EF-MAT-08 (Sprint 12) : meme pattern de degradation gracieuse que ci-dessus. */
    public void publierPropositionRetourAVide(PropositionRetourAVideEvent event) {
        // tourneeId et affectationId sont mutuellement exclusifs (cf javadoc du
        // record) - la cle Kafka de partitionnement doit suivre celui des deux
        // qui est reellement renseigne, jamais supposer tourneeId toujours
        // present (regression corrigee suite a test manuel du 2026-08-17 :
        // NPE sur tourneeId() null pour le cas FTL simple).
        String cle = event.tourneeId() != null
                ? event.tourneeId().toString()
                : event.affectationId().toString();
        String identifiantLog = event.tourneeId() != null
                ? "tournee=" + event.tourneeId()
                : "affectation=" + event.affectationId();
        try {
            kafkaTemplate.send(TOPIC_PROPOSITION_RETOUR_A_VIDE, cle, event)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            log.info("PropositionRetourAVide publiee - {}, offset={}",
                                    identifiantLog, result.getRecordMetadata().offset());
                        } else {
                            log.error("Echec publication PropositionRetourAVide (callback async) - {}",
                                    identifiantLog, ex);
                        }
                    });
        } catch (Exception exceptionBlocante) {
            log.error("Echec publication PropositionRetourAVide (send() bloquant) - {} "
                    + "- non bloquant (ENF-DIS-04)", identifiantLog, exceptionBlocante);
        }
    }

    /** EF-MAT-05/06 (Sprint 11) : meme pattern de degradation gracieuse que ci-dessus. */
    public void publierTourneeConstituee(TourneeConstitueeEvent event) {
        try {
            String cle = event.tourneeId().toString();
            kafkaTemplate.send(TOPIC_TOURNEE_CONSTITUEE, cle, event)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            log.info("TourneeConstituee publiee - tournee={}, {} etape(s), offset={}",
                                    event.tourneeId(), event.etapes().size(), result.getRecordMetadata().offset());
                        } else {
                            log.error("Echec publication TourneeConstituee (callback async) - tournee={}",
                                    event.tourneeId(), ex);
                        }
                    });
        } catch (Exception exceptionBlocante) {
            log.error("Echec publication TourneeConstituee (send() bloquant) - tournee={} "
                    + "- non bloquant (ENF-DIS-04)", event.tourneeId(), exceptionBlocante);
        }
    }

    /** EF-MAT-13 (Sprint 16, priorite S) : meme pattern de degradation gracieuse que ci-dessus. */
    public void publierPlanChargementConfirme(PlanChargementConfirmeEvent event) {
        try {
            String cle = event.tourneeId().toString();
            kafkaTemplate.send(TOPIC_PLAN_CHARGEMENT_CONFIRME, cle, event)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            log.info("PlanChargementConfirme publie - tournee={}, {} etat(s), offset={}",
                                    event.tourneeId(), event.etats().size(), result.getRecordMetadata().offset());
                        } else {
                            log.error("Echec publication PlanChargementConfirme (callback async) - tournee={}",
                                    event.tourneeId(), ex);
                        }
                    });
        } catch (Exception exceptionBlocante) {
            log.error("Echec publication PlanChargementConfirme (send() bloquant) - tournee={} "
                    + "- non bloquant (ENF-DIS-04)", event.tourneeId(), exceptionBlocante);
        }
    }

    /**
     * EF-GEO-05/RG-052 (Phase 4) : meme pattern de degradation gracieuse que
     * ci-dessus. Appele UNIQUEMENT quand Axe.parametres.conventionRepartition
     * est present (cf javadoc RepartitionConventionnelleAppliqueeEvent) -
     * jamais pour un axe intra-camerounais normal.
     */
    public void publierRepartitionConventionnelleAppliquee(RepartitionConventionnelleAppliqueeEvent event) {
        try {
            String cle = event.missionId().toString();
            kafkaTemplate.send(TOPIC_REPARTITION_CONVENTIONNELLE, cle, event)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            log.info("RepartitionConventionnelleAppliquee publiee - mission={}, convention={}, offset={}",
                                    event.missionId(), event.conventionCode(), result.getRecordMetadata().offset());
                        } else {
                            log.error("Echec publication RepartitionConventionnelleAppliquee (callback async) - mission={}",
                                    event.missionId(), ex);
                        }
                    });
        } catch (Exception exceptionBlocante) {
            log.error("Echec publication RepartitionConventionnelleAppliquee (send() bloquant) - mission={} "
                    + "- non bloquant (ENF-DIS-04)", event.missionId(), exceptionBlocante);
        }
    }
}