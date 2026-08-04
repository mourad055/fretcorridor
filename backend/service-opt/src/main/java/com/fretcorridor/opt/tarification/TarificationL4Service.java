package com.fretcorridor.opt.tarification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Couche L4 du moteur (CDC S8.9) : produit un prix ferme decomposable,
 * apres L1-L3 (S8.10, budget cible 20ms/100ms). Jamais de barème code en
 * dur (RG-112) - toutes les valeurs viennent de BaremeTarification /
 * ComposantCoutService, resolues en base a chaque appel.
 */
@Service
public class TarificationL4Service {

    private static final Logger log = LoggerFactory.getLogger(TarificationL4Service.class);

    private final BaremeTarificationRepository baremeRepository;
    private final ComposantCoutServiceRepository composantRepository;

    public TarificationL4Service(BaremeTarificationRepository baremeRepository,
                                  ComposantCoutServiceRepository composantRepository) {
        this.baremeRepository = baremeRepository;
        this.composantRepository = composantRepository;
    }

    /**
     * Calcule le prix pour une affectation retenue. Retourne un resultat en
     * mode degrade (modeDegrade=true, prixTransport=null) plutot que de
     * lancer une exception ou d'inventer un prix - conforme ENF-DIS-04 et
     * au meme principe deja applique a ValhallaClient : l'appelant
     * (AffectationL1Service) doit gerer ce cas explicitement.
     *
     * @param axeId              axe de la demande/capacite appariees (peut etre null -> bareme par defaut)
     * @param poidsTaxableKg     poids taxable de la demande (regime POIDS_TAXABLE, RG-101 "regle du maximum" deja applique en amont cote service-cap)
     * @param distanceMetres     distance Valhalla si disponible, null sinon (regime POIDS_TAXABLE en a besoin pour COUT_BASE)
     * @param facteurTensionBrut signal de tension marche non borne, 0 si absent (Phase 3 observatoire - pas encore alimente en V0)
     */
    public TarificationResultat calculer(UUID axeId, String typeVehicule, BigDecimal poidsTaxableKg,
                                          Double distanceMetres, BigDecimal facteurTensionBrut) {

        Optional<BaremeTarification> baremeOpt = resoudreBareme(axeId, typeVehicule);
        if (baremeOpt.isEmpty()) {
            log.warn("Aucun bareme actif trouve (axe={}, ni bareme specifique ni bareme par defaut) - "
                    + "tarification en mode degrade.", axeId);
            return modeDegrade(null, null);
        }
        BaremeTarification bareme = baremeOpt.get();

        BigDecimal coutBase;
        BigDecimal coutVariable;

        if ("FORFAITAIRE_VEHICULE".equals(bareme.getRegime())) {
            // S8.9.1 : "en regime forfaitaire au camion, seul le socle
            // s'applique, indexe sur l'axe et le type de vehicule" - pas
            // besoin de distance ici.
            coutBase = nvl(bareme.getCoutSocleForfaitaire());
            coutVariable = BigDecimal.ZERO;
        } else {
            // Regime POIDS_TAXABLE : necessite la distance Valhalla pour
            // COUT_BASE(axe, distance, type_vehicule). Sans distance,
            // aucun prix fiable ne peut etre produit - mode degrade plutot
            // qu'une distance a vol d'oiseau devinee (meme principe que
            // ValhallaClient, ne jamais improviser silencieusement).
            if (distanceMetres == null) {
                log.warn("Regime POIDS_TAXABLE mais distance Valhalla indisponible (bareme {} v{}) - "
                        + "tarification en mode degrade.", bareme.getId(), bareme.getVersion());
                return modeDegrade(bareme.getId(), bareme.getVersion());
            }
            BigDecimal distanceKm = BigDecimal.valueOf(distanceMetres / 1000.0);
            coutBase = nvl(bareme.getCoutBaseParKm()).multiply(distanceKm);
            coutVariable = nvl(bareme.getCoutUnitairePoidsTaxable()).multiply(nvl(poidsTaxableKg));
        }

        BigDecimal coutServices = composantRepository.findByBaremeId(bareme.getId()).stream()
                .map(ComposantCoutService::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal facteurTensionApplique = borner(nvl(facteurTensionBrut),
                bareme.getTensionMinFraction(), bareme.getTensionMaxFraction());

        BigDecimal sousTotal = coutBase.add(coutVariable).add(coutServices);
        BigDecimal prixAvantPlancher = sousTotal.add(sousTotal.multiply(facteurTensionApplique))
                .setScale(4, RoundingMode.HALF_UP);

        boolean plancherApplique = bareme.isPrixPlancherActif()
                && bareme.getPrixPlancher() != null
                && prixAvantPlancher.compareTo(bareme.getPrixPlancher()) < 0;
        BigDecimal prixTransport = plancherApplique ? bareme.getPrixPlancher() : prixAvantPlancher;

        // RG-116 : commission distincte, prelevee sur le transporteur (cf
        // javadoc TarificationResultat pour l'hypothese a confirmer avec PAY).
        BigDecimal commission = prixTransport.multiply(bareme.getCommissionTauxFraction())
                .setScale(4, RoundingMode.HALF_UP);
        BigDecimal montantTransporteur = prixTransport.subtract(commission);

        return new TarificationResultat(
                bareme.getId(), bareme.getVersion(), bareme.getRegime(),
                coutBase, coutVariable, coutServices, facteurTensionApplique,
                prixAvantPlancher, plancherApplique, prixTransport,
                commission, montantTransporteur, false);
    }

    /**
     * Cascade a 4 niveaux (CDC S8.9.1, COUT_BASE indexe axe ET type de
     * vehicule) : le plus specifique gagne, repli progressif. Un axe donne
     * peut n'avoir de bareme que pour certains types de vehicule - dans ce
     * cas on retombe sur le bareme generique de l'axe avant de retomber sur
     * le defaut global, jamais l'inverse (l'axe prime toujours sur le type
     * quand les deux offrent un choix).
     */
    private Optional<BaremeTarification> resoudreBareme(UUID axeId, String typeVehicule) {
        if (axeId != null && typeVehicule != null) {
            Optional<BaremeTarification> axeEtType =
                    baremeRepository.findFirstByAxeIdAndTypeVehiculeAndActifTrue(axeId, typeVehicule);
            if (axeEtType.isPresent()) {
                return axeEtType;
            }
        }
        if (axeId != null) {
            Optional<BaremeTarification> axeSeul = baremeRepository.findFirstByAxeIdAndTypeVehiculeIsNullAndActifTrue(axeId);
            if (axeSeul.isPresent()) {
                return axeSeul;
            }
        }
        if (typeVehicule != null) {
            Optional<BaremeTarification> typeSeul = baremeRepository.findFirstByAxeIdIsNullAndTypeVehiculeAndActifTrue(typeVehicule);
            if (typeSeul.isPresent()) {
                return typeSeul;
            }
        }
        return baremeRepository.findFirstByAxeIdIsNullAndTypeVehiculeIsNullAndActifTrue();
    }

    /** RG-114 - clamp strict, jamais de facteur hors bornes meme si la source en amont derape. */
    private BigDecimal borner(BigDecimal valeur, BigDecimal min, BigDecimal max) {
        if (valeur.compareTo(min) < 0) return min;
        if (valeur.compareTo(max) > 0) return max;
        return valeur;
    }

    private BigDecimal nvl(BigDecimal valeur) {
        return valeur == null ? BigDecimal.ZERO : valeur;
    }

    private TarificationResultat modeDegrade(UUID baremeId, Integer baremeVersion) {
        return new TarificationResultat(baremeId, baremeVersion, null,
                null, null, null, null, null, false, null, null, null, true);
    }
}
