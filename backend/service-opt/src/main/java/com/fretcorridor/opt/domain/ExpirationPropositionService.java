package com.fretcorridor.opt.domain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Diffusion-course (timeout compte a rebours Mobile) : prolonge le cycle de
 * vie des Affectation pour couvrir "personne n'a accepte a temps". Avant ce
 * service, EXPIREE ne couvrait que "course perdue" (une autre affectation de
 * la meme demande confirmee) ou "refus explicite" d'un chauffeur - une
 * proposition jamais acceptee restait PROPOSEE indefiniment, sans compte a
 * rebours cote app chauffeur (maquette "expire 5 min").
 *
 * A chaque passe (meme cadence que le matching, 15s) :
 *   1. Passe EXPIREE toute Affectation PROPOSEE dont expireA est depasse
 *      (posse a la creation = dateCreation + proposition-expiration-ms,
 *      defaut 15 min). Idempotent (marquerExpireeSiProposee ne touche que
 *      l'etat PROPOSEE).
 *   2. Si une demande n'a plus AUCUNE proposition PROPOSEE active ET n'est
 *      pas CONFIRMEE ailleurs, la remet en file (traitee=false) pour qu'un
 *      prochain cycle la re-diffuse a d'autres chauffeurs/capacites.
 *      Garde anti-regression : une demande CONFIRMEE a sa proposition en etat
 *      CONFIRMEE - on ne la remet jamais en file.
 *
 * REMARQUE capacite : une PROPOSEE ne reserve jamais de capacite (la
 * reservation reelle n'a lieu qu'a la confirmation, ServiceCapClient.reserver
 * dans AffectationConfirmationService). L'expiration par timeout ne libere
 * donc rien : la capacite reste simplement proposable aux cycles suivants.
 */
@Service
public class ExpirationPropositionService {

    private static final Logger log = LoggerFactory.getLogger(ExpirationPropositionService.class);

    private final AffectationRepository affectationRepository;
    private final DemandeEnAttenteRepository demandeEnAttenteRepository;

    public ExpirationPropositionService(AffectationRepository affectationRepository,
                                        DemandeEnAttenteRepository demandeEnAttenteRepository) {
        this.affectationRepository = affectationRepository;
        this.demandeEnAttenteRepository = demandeEnAttenteRepository;
    }

    @Scheduled(fixedDelayString = "${spring.matching.cycle-interval-ms:15000}")
    @Transactional
    public void expirerPropositionsDepassees() {
        Instant maintenant = Instant.now();
        var aExpirer = affectationRepository.findByStatutAndExpireALessThan(StatutAffectation.PROPOSEE, maintenant);
        if (aExpirer.isEmpty()) {
            return;
        }

        aExpirer.forEach(Affectation::marquerExpireeSiProposee);
        affectationRepository.saveAll(aExpirer);
        log.info("Expiration par timeout : {} proposition(s) PROPOSEE -> EXPIREE.", aExpirer.size());

        Set<UUID> demandes = aExpirer.stream()
                .map(Affectation::getDemandeId)
                .collect(Collectors.toSet());
        demandes.forEach(this::remettreEnFileSiEligible);
    }

    private void remettreEnFileSiEligible(UUID demandeId) {
        boolean encoreProposee = !affectationRepository
                .findByDemandeIdAndStatut(demandeId, StatutAffectation.PROPOSEE).isEmpty();
        boolean confirmee = !affectationRepository
                .findByDemandeIdAndStatut(demandeId, StatutAffectation.CONFIRMEE).isEmpty();
        if (encoreProposee || confirmee) {
            return;
        }
        demandeEnAttenteRepository.findByDemandeId(demandeId).ifPresent(demande -> {
            demande.remettreEnFile();
            demandeEnAttenteRepository.save(demande);
            log.info("Demande {} remise en file apres expiration de toutes ses propositions (timeout).",
                    demandeId);
        });
    }
}
