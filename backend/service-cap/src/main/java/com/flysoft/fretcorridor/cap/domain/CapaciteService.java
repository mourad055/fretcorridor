package com.flysoft.fretcorridor.cap.domain;

import com.flysoft.fretcorridor.cap.client.ServiceFltClient;
import com.flysoft.fretcorridor.cap.messaging.CapEventPublisher;
import com.flysoft.fretcorridor.cap.messaging.CapaciteDeclareeEvent;
import com.flysoft.fretcorridor.cap.messaging.PointGeoDto;
import com.flysoft.fretcorridor.cap.messaging.ProfilCamionDto;
import com.flysoft.fretcorridor.cap.web.dto.CapaciteCreationRequest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Service
public class CapaciteService {

    private static final Logger log = LoggerFactory.getLogger(CapaciteService.class);

    private final CapaciteRepository capaciteRepository;
    private final CalculateurPoidsTaxable calculateurPoidsTaxable;
    private final CapEventPublisher eventPublisher;
    private final ServiceFltClient serviceFltClient;

    @PersistenceContext
    private EntityManager entityManager;

    public CapaciteService(CapaciteRepository capaciteRepository,
                            CalculateurPoidsTaxable calculateurPoidsTaxable,
                            CapEventPublisher eventPublisher,
                            ServiceFltClient serviceFltClient) {
        this.capaciteRepository = capaciteRepository;
        this.calculateurPoidsTaxable = calculateurPoidsTaxable;
        this.eventPublisher = eventPublisher;
        this.serviceFltClient = serviceFltClient;
    }

    @Transactional
    public Capacite declarer(CapaciteCreationRequest requete) {
        BigDecimal poidsTaxable = calculateurPoidsTaxable.calculer(
                requete.poidsKg(), requete.volumeM3(), requete.longueurPlancherM());

        // Resolution best-effort du transporteur (ferme le bug S7) - jamais
        // bloquant, cf javadoc ServiceFltClient (ENF-DIS-04).
        UUID transporteurId = serviceFltClient.resoudreProprietaire(requete.vehiculeId()).orElse(null);

        Capacite capacite = new Capacite(
                requete.vehiculeId(), requete.axeId(), transporteurId, requete.modeDeclaration(),
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
    public Capacite decrementer(UUID capaciteId, BigDecimal montantKg, String cleIdempotence) {
        Capacite capacite = capaciteRepository.findById(capaciteId)
                .orElseThrow(() -> new IllegalArgumentException("Capacite introuvable : " + capaciteId));

        // INSERT immediat plutot qu'un SELECT prealable : la contrainte unique
        // fait le travail d'exclusion mutuelle, evite une fenetre de course
        // entre "verifier" et "inserer".
        //
        // Isole dans un SAVEPOINT JDBC natif (Session.doWork), PAS dans sa
        // propre transaction physique (REQUIRES_NEW, l'ancien choix - voir
        // bug corrige ci-dessous), et PAS via PROPAGATION_NESTED de Spring
        // (JpaTransactionManager avec le JpaDialect par defaut de ce projet
        // ne supporte pas les savepoints - NestedTransactionNotSupportedException,
        // essaye et abandonne le 20 aout).
        //
        // BUG CORRIGE (audit CDC + investigation du 20 aout,
        // CapaciteServiceConcurrenceTest "deuxDecrementsConcurrentsAvecClesDifferentes"
        // reproduisait une perte d'ecriture silencieuse malgre @Version) :
        // REQUIRES_NEW commit CET INSERT immediatement et independamment de
        // la transaction englobante. Si le decrement lui-meme echoue ensuite
        // (conflit @Version legitime avec un AUTRE appel concurrent, cle
        // differente) et que le CALLER retente (comportement documente et
        // attendu, EF-CAP-07) - le nouvel essai retrouve sa propre cle deja
        // "appliquee" dans decrement_log (le premier essai a bel et bien
        // commit cette ligne, alors meme que son decrement reel a ete
        // annule) et abandonne a tort, croyant le travail deja fait.
        //
        // Un savepoint JDBC (au lieu d'une transaction independante) resout
        // les deux problemes a la fois : un doublon reel (meme cle, deja
        // appliquee et commit par une transaction PRECEDENTE, entierement
        // terminee) annule proprement jusqu'au savepoint sans empoisonner la
        // transaction en cours - mais si le reste de CETTE MEME transaction
        // echoue ensuite, ce savepoint est annule avec elle (contrairement a
        // REQUIRES_NEW, qui aurait deja commit), donc un retry repart bien
        // de zero plutot que de trouver une trace fantome de la tentative
        // precedente.
        AtomicBoolean dejaApplique = new AtomicBoolean(false);
        entityManager.unwrap(Session.class).doWork(connection -> {
            Savepoint savepoint = connection.setSavepoint();
            try (PreparedStatement stmt = connection.prepareStatement(
                    "INSERT INTO cap.decrement_log (capacite_id, cle_idempotence, montant_kg) VALUES (?, ?, ?)")) {
                stmt.setObject(1, capaciteId);
                stmt.setString(2, cleIdempotence);
                stmt.setBigDecimal(3, montantKg);
                stmt.executeUpdate();
                connection.releaseSavepoint(savepoint);
            } catch (SQLException echecInsertion) {
                // SQLState 23505 = unique_violation (Postgres et H2 en mode
                // PostgreSQL) = doublon reel, benin. Tout autre SQLState est
                // une vraie erreur, ne doit jamais etre avale silencieusement.
                if (!"23505".equals(echecInsertion.getSQLState())) {
                    throw echecInsertion;
                }
                connection.rollback(savepoint);
                dejaApplique.set(true);
            }
        });

        if (dejaApplique.get()) {
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
