package com.flysoft.fretcorridor.cap.domain;

import com.flysoft.fretcorridor.cap.client.ServiceFltClient;
import com.flysoft.fretcorridor.cap.messaging.CapEventPublisher;
import com.flysoft.fretcorridor.cap.messaging.CapaciteDeclareeEvent;
import com.flysoft.fretcorridor.cap.messaging.PointGeoDto;
import com.flysoft.fretcorridor.cap.messaging.ProfilCamionDto;
import com.flysoft.fretcorridor.cap.web.dto.CapaciteCreationRequest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CapaciteService {

    private static final Logger log = LoggerFactory.getLogger(CapaciteService.class);

    private final CapaciteRepository capaciteRepository;
    private final CalculateurPoidsTaxable calculateurPoidsTaxable;
    private final CapEventPublisher eventPublisher;
    private final ServiceFltClient serviceFltClient;
    // Isole l'INSERT d'idempotence dans SA PROPRE transaction physique
    // (REQUIRES_NEW). Sans ca, une violation de contrainte unique sur cet
    // INSERT marque la transaction Hibernate/JPA EN COURS comme
    // rollback-only en interne - meme si l'exception est attrapee en Java
    // (ce qui empeche seulement l'exception de remonter, pas cet etat
    // interne), le commit final de la methode decrementer() echoue quand
    // meme avec UnexpectedRollbackException. Decouvert par
    // CapaciteServiceConcurrenceTest (4/5 appels concurrents avec la meme
    // cle d'idempotence).
    private final TransactionTemplate transactionTemplateIdempotence;

    @PersistenceContext
    private EntityManager entityManager;

    public CapaciteService(CapaciteRepository capaciteRepository,
                            CalculateurPoidsTaxable calculateurPoidsTaxable,
                            CapEventPublisher eventPublisher,
                            ServiceFltClient serviceFltClient,
                            PlatformTransactionManager transactionManager) {
        this.capaciteRepository = capaciteRepository;
        this.calculateurPoidsTaxable = calculateurPoidsTaxable;
        this.eventPublisher = eventPublisher;
        this.serviceFltClient = serviceFltClient;
        this.transactionTemplateIdempotence = new TransactionTemplate(transactionManager);
        this.transactionTemplateIdempotence.setPropagationBehavior(Propagation.REQUIRES_NEW.value());
    }

    @Transactional
    public Capacite declarer(CapaciteCreationRequest requete, String tenantId) {
        BigDecimal poidsTaxable = calculateurPoidsTaxable.calculer(requete.poidsKg(), requete.volumeM3());

        // Resolution best-effort du transporteur (ferme le bug S7) - jamais
        // bloquant, cf javadoc ServiceFltClient (ENF-DIS-04).
        UUID transporteurId = serviceFltClient.resoudreProprietaire(requete.vehiculeId()).orElse(null);

        Capacite capacite = new Capacite(
                requete.vehiculeId(), requete.axeId(), tenantId, transporteurId, requete.modeDeclaration(),
                requete.poidsKg(), requete.volumeM3(), requete.longueurPlancherM(),
                poidsTaxable, requete.origineLatitude(), requete.origineLongitude(),
                requete.typeVehicule(),
                nz(requete.profilHauteurMetres()), nz(requete.profilLargeurMetres()),
                nz(requete.profilLongueurMetres()), nz(requete.profilPoidsMaxTonnes()),
                nz(requete.profilChargeMaxParEssieuTonnes()), requete.profilNombreEssieux(),
                requete.profilMatieresDangereuses(), requete.dateDepart());

        capacite = capaciteRepository.save(capacite);

        // Publication immediate (V0 : declaration = publication synchrone dans
        // la meme transaction applicative ; une vraie robustesse transactionnelle
        // outbox/CDC reste a envisager en Phase 2, documentee ici comme
        // limitation plutot que cachee).
        publierEvenement(capacite);
        capacite.marquerPubliee();

        return capacite;
    }

    private void publierEvenement(Capacite capacite) {
        // Valeurs neutres en attendant une vraie definition des criteres de
        // matching cote capacite (indice de conformite, historique fiabilite -
        // hors perimetre service-cap V0) : PLACEHOLDER explicite, jamais
        // presente comme une vraie mesure.
        Map<String, Double> valeursCriteres = Map.of(
                "DISTANCE", 0.5, "DELAI", 0.5, "FIABILITE", 0.5, "PRIX", 0.5);

        ProfilCamionDto profil = new ProfilCamionDto(
                toDouble(capacite.getProfilHauteurM()), toDouble(capacite.getProfilLargeurM()),
                toDouble(capacite.getProfilLongueurM()), toDouble(capacite.getProfilPoidsMaxT()),
                toDouble(capacite.getProfilChargeEssieuMaxT()), capacite.getProfilNbEssieux(),
                capacite.isProfilMatieresDangereuses());

        CapaciteDeclareeEvent event = new CapaciteDeclareeEvent(
                UUID.randomUUID(), capacite.getId(), capacite.getAxeId(),
                capacite.getTransporteurId(), capacite.getVehiculeId(), valeursCriteres,
                new PointGeoDto(capacite.getOrigineLatitude(), capacite.getOrigineLongitude()),
                profil, capacite.getTypeVehicule(),
                capacite.getCapaciteResiduelleKg(), capacite.getVolumeM3());

        eventPublisher.publierCapaciteDeclaree(event);
    }

    @Transactional(readOnly = true)
    public Capacite obtenir(UUID capaciteId) {
        return capaciteRepository.findById(capaciteId)
                .orElseThrow(() -> new IllegalArgumentException("Capacite introuvable : " + capaciteId));
    }

    /**
     * EF-CAP-07 : decrement atomique et idempotent. Le verrou optimiste
     * (@Version sur Capacite) garantit l'atomicite contre les acces
     * concurrents ; la table decrement_log (contrainte unique capacite+cle)
     * garantit l'idempotence contre un retry reseau qui rejouerait la meme
     * requete.
     */
    @Transactional
    public Capacite decrementer(UUID capaciteId, String tenantId, BigDecimal montantKg, String cleIdempotence) {
        Capacite capacite = capaciteAppartenantA(capaciteId, tenantId);

        // INSERT immediat plutot qu'un SELECT prealable : la contrainte unique
        // fait le travail d'exclusion mutuelle, evite une fenetre de course
        // entre "verifier" et "inserer". Isole dans REQUIRES_NEW (cf champ
        // transactionTemplateIdempotence) pour qu'un echec ici ne marque
        // jamais la transaction principale comme rollback-only.
        boolean dejaApplique = Boolean.FALSE.equals(transactionTemplateIdempotence.execute(status -> {
            try {
                entityManager.createNativeQuery(
                                "INSERT INTO cap.decrement_log (capacite_id, cle_idempotence, montant_kg) "
                                        + "VALUES (?1, ?2, ?3)")
                        .setParameter(1, capaciteId)
                        .setParameter(2, cleIdempotence)
                        .setParameter(3, montantKg)
                        .executeUpdate();
                return Boolean.TRUE; // insertion reussie, premiere fois
            // DataIntegrityViolationException seule ne suffit pas ici : sur un
            // EntityManager direct (pas un Repository Spring Data), les
            // exceptions Hibernate brutes ne sont pas traduites - il faut
            // aussi attraper org.hibernate.exception.ConstraintViolationException.
            } catch (DataIntegrityViolationException | ConstraintViolationException doublon) {
                status.setRollbackOnly(); // annule PROPREMENT cette sous-transaction isolee
                return Boolean.FALSE; // deja applique par un appel concurrent/precedent
            }
        }));

        if (dejaApplique) {
            log.info("Decrement deja applique (idempotence) - capacite={}, cle={}",
                    capaciteId, cleIdempotence);
            return capacite; // deja decremente lors du premier appel, aucun changement
        }

        capacite.decrementer(montantKg);
        return capaciteRepository.save(capacite); // verrou optimiste applique ici
    }

    // IDOR corrige (audit CDC du 19 aout, §3) : POST /decrement acceptait
    // n'importe quel capaciteId sans verifier de tenant - une seule
    // exception pour "introuvable" et "pas la sienne", meme principe que
    // missionAppartenantA (service-exe) / notificationAppartenantA
    // (service-not).
    private Capacite capaciteAppartenantA(UUID capaciteId, String tenantId) {
        Capacite capacite = capaciteRepository.findById(capaciteId)
                .orElseThrow(() -> new IllegalArgumentException("Capacite introuvable : " + capaciteId));
        if (!java.util.Objects.equals(capacite.getTenantId(), tenantId)) {
            throw new IllegalArgumentException("Capacite introuvable : " + capaciteId);
        }
        return capacite;
    }

    private BigDecimal nz(Double valeur) {
        return valeur == null ? null : BigDecimal.valueOf(valeur);
    }

    private Double toDouble(BigDecimal valeur) {
        return valeur == null ? null : valeur.doubleValue();
    }
}
