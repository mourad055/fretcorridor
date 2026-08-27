package com.fretcorridor.bur.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * EF-BUR-03, UC-BUR-02 : observatoire de marché, construit sur les missions
 * appariées déjà matérialisées (Kafka AffectationConfirmee), pas sur le
 * comptage disjoint de {@link AgregationMissionsService} (endpoint temporaire
 * non raccordé au gateway — cf. ROADMAP_INTEGRATION_gateway.md). Domaine pur,
 * sans dépendance à Spring — testable sans mock de framework.
 */
public class ObservatoireService {

    /**
     * EF-BUR-05 : fenêtre de comparaison entre l'échantillon observé et
     * l'estimation mensuelle déclarée (cf. {@link EstimationMarcheAxe}).
     * L'observatoire n'a aujourd'hui aucune notion de période sélectionnée
     * par l'analyste (UC-BUR-02 le prévoit, jamais construit) — sans borne,
     * comparer "toutes les missions depuis le lancement de l'axe" à une
     * estimation mensuelle grimperait indéfiniment et perdrait tout sens.
     */
    private static final int FENETRE_COUVERTURE_JOURS = 30;

    private final MissionAppparieeRepositoryPort repository;
    private final EstimationMarcheAxePort estimationMarcheAxePort;
    private final long seuilAgregation;

    public ObservatoireService(MissionAppparieeRepositoryPort repository, EstimationMarcheAxePort estimationMarcheAxePort,
                                long seuilAgregation) {
        if (seuilAgregation < 1) {
            throw new IllegalArgumentException("Le seuil d'agrégation doit être au moins 1");
        }
        this.repository = repository;
        this.estimationMarcheAxePort = estimationMarcheAxePort;
        this.seuilAgregation = seuilAgregation;
    }

    public ObservatoireAxe indicateursPourAxe(String tenantId, UUID axeId, Instant maintenant) {
        List<MissionAppariee> missions = repository.listerParTenant(tenantId).stream()
                .filter(m -> m.axeId().equals(axeId))
                .toList();

        if (missions.size() < seuilAgregation) {
            return ObservatoireAxe.sousLeSeuil(axeId, seuilAgregation);
        }

        List<BigDecimal> prix = missions.stream()
                .map(MissionAppariee::prixTransport)
                .filter(Objects::nonNull)
                .sorted()
                .toList();
        if (prix.isEmpty()) {
            return ObservatoireAxe.sousLeSeuil(axeId, seuilAgregation);
        }

        String devise = missions.stream()
                .map(MissionAppariee::devise)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse("XAF");
        Optional<EstimationMarcheAxe> estimation = estimationMarcheAxePort.pour(tenantId, axeId);
        BigDecimal couverture = estimation.map(e -> couverturePourcentage(missions, e, maintenant)).orElse(null);
        Instant estimationDefinieLe = estimation.map(EstimationMarcheAxe::definieLe).orElse(null);

        return ObservatoireAxe.calcule(axeId, seuilAgregation, missions.size(), mediane(prix),
                ecartInterquartile(prix), devise, tauxDesequilibreDirectionnel(missions),
                couverture, estimationDefinieLe);
    }

    /** EF-BUR-05, RG-087 : couverture = part du volume mensuel estimé (déclaratif) que représente l'échantillon récent. */
    public EstimationMarcheAxe definirEstimationMarche(String tenantId, UUID axeId, BigDecimal volumeMensuelEstime,
                                                         String source, String acteurId, Instant maintenant) {
        EstimationMarcheAxe estimation = new EstimationMarcheAxe(tenantId, axeId, volumeMensuelEstime, source,
                acteurId, maintenant);
        estimationMarcheAxePort.definir(estimation);
        return estimation;
    }

    private static BigDecimal couverturePourcentage(List<MissionAppariee> missionsAxe, EstimationMarcheAxe estimation,
                                                      Instant maintenant) {
        Instant depuis = maintenant.minus(FENETRE_COUVERTURE_JOURS, ChronoUnit.DAYS);
        long missionsRecentes = missionsAxe.stream().filter(m -> m.confirmeeLe().isAfter(depuis)).count();
        return BigDecimal.valueOf(missionsRecentes)
                .divide(estimation.volumeMensuelEstime(), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }

    /** RG-087 : dispersion mesurée par l'écart interquartile — plus robuste aux valeurs extrêmes que l'écart-type sur un prix. */
    private static BigDecimal ecartInterquartile(List<BigDecimal> prixTries) {
        return percentile(prixTries, 0.75).subtract(percentile(prixTries, 0.25));
    }

    private static BigDecimal mediane(List<BigDecimal> prixTries) {
        return percentile(prixTries, 0.5);
    }

    private static BigDecimal percentile(List<BigDecimal> valeursTriees, double rang) {
        int taille = valeursTriees.size();
        double indice = rang * (taille - 1);
        int indiceBas = (int) Math.floor(indice);
        int indiceHaut = (int) Math.ceil(indice);
        if (indiceBas == indiceHaut) {
            return valeursTriees.get(indiceBas);
        }
        BigDecimal bas = valeursTriees.get(indiceBas);
        BigDecimal haut = valeursTriees.get(indiceHaut);
        BigDecimal poids = BigDecimal.valueOf(indice - indiceBas);
        return bas.add(haut.subtract(bas).multiply(poids)).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Un axe relie deux points ; les missions se répartissent en deux sens
     * (origine de l'une = destination de l'autre). {@code 0.5} = parfaitement
     * équilibré, {@code 1.0} = tout le trafic dans un seul sens.
     */
    private static double tauxDesequilibreDirectionnel(List<MissionAppariee> missions) {
        Map<String, Long> comptageParOrigine = new HashMap<>();
        for (MissionAppariee mission : missions) {
            comptageParOrigine.merge(mission.origineNom(), 1L, Long::sum);
        }
        long maxSens = comptageParOrigine.values().stream().mapToLong(Long::longValue).max().orElse(0);
        return maxSens / (double) missions.size();
    }
}
