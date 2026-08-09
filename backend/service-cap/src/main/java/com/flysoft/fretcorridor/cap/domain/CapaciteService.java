package com.flysoft.fretcorridor.cap.domain;

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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @PersistenceContext
    private EntityManager entityManager;

    public CapaciteService(CapaciteRepository capaciteRepository,
                            CalculateurPoidsTaxable calculateurPoidsTaxable,
                            CapEventPublisher eventPublisher) {
        this.capaciteRepository = capaciteRepository;
        this.calculateurPoidsTaxable = calculateurPoidsTaxable;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Capacite declarer(CapaciteCreationRequest requete) {
        BigDecimal poidsTaxable = calculateurPoidsTaxable.calculer(requete.poidsKg(), requete.volumeM3());

        Capacite capacite = new Capacite(
                requete.vehiculeId(), requete.axeId(), requete.modeDeclaration(),
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
                UUID.randomUUID(), capacite.getId(), capacite.getAxeId(), valeursCriteres,
                new PointGeoDto(capacite.getOrigineLatitude(), capacite.getOrigineLongitude()),
                profil, capacite.getTypeVehicule());

        eventPublisher.publierCapaciteDeclaree(event);
    }

    /**
     * EF-CAP-07 : decrement atomique et idempotent. Le verrou optimiste
     * (@Version sur Capacite) garantit l'atomicite contre les acces
     * concurrents ; la table decrement_log (contrainte unique capacite+cle)
     * garantit l'idempotence contre un retry reseau qui rejouerait la meme
     * requete.
     */
    @Transactional
    public Capacite decrementer(UUID capaciteId, BigDecimal montantKg, String cleIdempotence) {
        Capacite capacite = capaciteRepository.findById(capaciteId)
                .orElseThrow(() -> new IllegalArgumentException("Capacite introuvable : " + capaciteId));

        try {
            // INSERT immediat plutot qu'un SELECT prealable : la contrainte
            // unique fait le travail d'exclusion mutuelle, evite une fenetre
            // de course entre "verifier" et "inserer".
            entityManager.createNativeQuery(
                            "INSERT INTO cap.decrement_log (capacite_id, cle_idempotence, montant_kg) "
                                    + "VALUES (?1, ?2, ?3)")
                    .setParameter(1, capaciteId)
                    .setParameter(2, cleIdempotence)
                    .setParameter(3, montantKg)
                    .executeUpdate();
        } catch (DataIntegrityViolationException doublon) {
            log.info("Decrement deja applique (idempotence) - capacite={}, cle={}",
                    capaciteId, cleIdempotence);
            return capacite; // deja decremente lors du premier appel, aucun changement
        }

        capacite.decrementer(montantKg);
        return capaciteRepository.save(capacite); // verrou optimiste applique ici
    }

    private BigDecimal nz(Double valeur) {
        return valeur == null ? null : BigDecimal.valueOf(valeur);
    }

    private Double toDouble(BigDecimal valeur) {
        return valeur == null ? null : valeur.doubleValue();
    }
}
