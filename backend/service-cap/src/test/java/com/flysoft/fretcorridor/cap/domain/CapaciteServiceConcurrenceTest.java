package com.flysoft.fretcorridor.cap.domain;

import com.flysoft.fretcorridor.cap.web.dto.CapaciteCreationRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * EF-CAP-07 (Phase 1 MVP, priorite M) : "decrement atomique et idempotent".
 * Rapport d'audit gateway 2026-08-09, point 3 (§3) : aucun test ne couvrait
 * ni le verrou optimiste (@Version sur Capacite) ni l'idempotence
 * (contrainte unique cap.decrement_log) - ce test comble ce manque.
 *
 * Deux scenarios distincts, jamais confondus :
 *  1) Deux decrements CONCURRENTS avec des cles d'idempotence DIFFERENTES :
 *     les deux doivent s'appliquer, le total decremente doit etre exact -
 *     preuve que le verrou optimiste protege contre une perte de mise a jour
 *     (write skew), pas juste qu'aucune exception n'est levee.
 *  2) Le MEME decrement rejoue plusieurs fois EN CONCURRENCE avec la MEME
 *     cle d'idempotence : un seul doit reellement decrementer, tous les
 *     autres doivent retourner sans erreur mais sans double-decrement -
 *     preuve directe de l'idempotence sous concurrence, pas seulement en
 *     sequentiel (un retry reseau peut arriver en parallele du premier appel
 *     encore en cours, pas seulement apres coup).
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase
@EmbeddedKafka(partitions = 1, topics = {"capacite-declaree"})
class CapaciteServiceConcurrenceTest {

    @Autowired
    private CapaciteService capaciteService;

    @Autowired
    private CapaciteRepository capaciteRepository;

    private static final String TENANT = "tenant-bgft-douala";

    @Test
    void deuxDecrementsConcurrentsAvecClesDifferentes_lesDeuxSAppliquent() throws InterruptedException {
        Capacite capacite = capaciteService.declarer(nouvelleRequeteCapacite(BigDecimal.valueOf(10000)), TENANT, "token-test");
        UUID capaciteId = capacite.getId();

        int nbThreads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(nbThreads);
        CountDownLatch latchDepart = new CountDownLatch(1);
        CountDownLatch latchFin = new CountDownLatch(nbThreads);
        AtomicInteger echecsInattendus = new AtomicInteger(0);

        for (int i = 0; i < nbThreads; i++) {
            String cleIdempotence = "cle-distincte-" + i;
            executor.submit(() -> {
                try {
                    latchDepart.await();
                    // Retry manuel : le verrou optimiste peut legitimement rejeter
                    // un des deux appels concurrents (OptimisticLockException) -
                    // le comportement attendu du CALLER (cf. EF-CAP-07) est de
                    // retenter, jamais d'abandonner ou de perdre la mise a jour.
                    dernierEssai:
                    for (int tentative = 0; tentative < 10; tentative++) {
                        try {
                            capaciteService.decrementer(capaciteId, TENANT, BigDecimal.valueOf(1000), cleIdempotence);
                            break dernierEssai;
                        } catch (org.springframework.orm.ObjectOptimisticLockingFailureException retry) {
                            Thread.sleep(10);
                        }
                    }
                } catch (Exception e) {
                    System.err.println("ECHEC INATTENDU (test 1) : " + e.getClass().getName() + " - " + e.getMessage());
                    echecsInattendus.incrementAndGet();
                } finally {
                    latchFin.countDown();
                }
            });
        }

        latchDepart.countDown();
        boolean termine = latchFin.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(termine).as("les deux decrements concurrents doivent se terminer sous 10s").isTrue();
        assertThat(echecsInattendus.get()).as("aucun echec inattendu (hors retry sur verrou optimiste)").isZero();

        Capacite relue = capaciteRepository.findById(capaciteId).orElseThrow();
        // Preuve de l'atomicite : AUCUNE perte de mise a jour malgre la concurrence -
        // 10000 - 1000 - 1000 = 8000, jamais 9000 (ce que donnerait un write skew
        // si le verrou optimiste ne protegeait pas reellement le decrement).
        assertThat(relue.getCapaciteResiduelleKg()).isEqualByComparingTo(BigDecimal.valueOf(8000));
    }

    @Test
    void memeDecrementRejoueEnConcurrenceAvecMemeCle_uneSeuleFoisApplique() throws InterruptedException {
        Capacite capacite = capaciteService.declarer(nouvelleRequeteCapacite(BigDecimal.valueOf(10000)), TENANT, "token-test");
        UUID capaciteId = capacite.getId();
        String memeCleIdempotence = "cle-partagee-unique";

        int nbAppelsConcurrents = 5;
        ExecutorService executor = Executors.newFixedThreadPool(nbAppelsConcurrents);
        CountDownLatch latchDepart = new CountDownLatch(1);
        CountDownLatch latchFin = new CountDownLatch(nbAppelsConcurrents);
        AtomicInteger echecsInattendus = new AtomicInteger(0);

        for (int i = 0; i < nbAppelsConcurrents; i++) {
            executor.submit(() -> {
                try {
                    latchDepart.await();
                    capaciteService.decrementer(capaciteId, TENANT, BigDecimal.valueOf(500), memeCleIdempotence);
                } catch (Exception e) {
                    System.err.println("ECHEC INATTENDU (test 2) : " + e.getClass().getName() + " - " + e.getMessage());
                    echecsInattendus.incrementAndGet();
                } finally {
                    latchFin.countDown();
                }
            });
        }

        latchDepart.countDown();
        boolean termine = latchFin.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(termine).as("les 5 appels concurrents doivent se terminer sous 10s").isTrue();
        assertThat(echecsInattendus.get())
                .as("EF-CAP-07 : un retry avec la meme cle ne doit JAMAIS lever d'erreur, "
                        + "meme en concurrence avec l'appel original encore en cours")
                .isZero();

        Capacite relue = capaciteRepository.findById(capaciteId).orElseThrow();
        // Preuve directe de l'idempotence SOUS CONCURRENCE : un seul des 5 appels
        // (celui qui gagne la course d'insertion dans decrement_log) applique
        // reellement le decrement - 10000 - 500 = 9500, jamais 10000 - 2500.
        assertThat(relue.getCapaciteResiduelleKg()).isEqualByComparingTo(BigDecimal.valueOf(9500));
    }

    private CapaciteCreationRequest nouvelleRequeteCapacite(BigDecimal poidsKg) {
        return new CapaciteCreationRequest(
                UUID.randomUUID(), UUID.randomUUID(), ModeDeclaration.TOTALE,
                poidsKg, BigDecimal.valueOf(20), BigDecimal.valueOf(12),
                4.05, 9.70, "PORTEUR",
                3.5, 2.5, 12.0, 25.0, 10.0, 3, false,
                Instant.now().plusSeconds(3600));
    }
}
