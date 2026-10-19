package com.flysoft.fretcorridor.cap.domain;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
public class TarifChauffeurService {

    private final TarifChauffeurRepository tarifChauffeurRepository;

    public TarifChauffeurService(TarifChauffeurRepository tarifChauffeurRepository) {
        this.tarifChauffeurRepository = tarifChauffeurRepository;
    }

    @Transactional
    public TarifChauffeur configurer(UUID transporteurId, String tenantId, UUID axeId,
                                      CategorieMarchandise categorie, BigDecimal tarifFcfaParKg) {
        // "Configurer" est idempotent cote mobile (un seul bouton par
        // categorie) : une ligne existante est mise a jour plutot que
        // dupliquee, la contrainte UNIQUE en base reste le garde-fou final
        // contre une course entre deux appels concurrents.
        Optional<TarifChauffeur> existant = tarifChauffeurRepository
                .findByTransporteurIdAndAxeIdAndCategorieMarchandise(transporteurId, axeId, categorie);
        if (existant.isPresent()) {
            TarifChauffeur tarif = existant.get();
            tarif.modifierTarif(tarifFcfaParKg);
            return tarif;
        }
        return tarifChauffeurRepository.save(
                new TarifChauffeur(transporteurId, axeId, categorie, tarifFcfaParKg, tenantId));
    }

    @Transactional(readOnly = true)
    public List<TarifChauffeur> listerMesTarifs(UUID transporteurId, String tenantId) {
        return tarifChauffeurRepository.findByTransporteurIdAndTenantIdOrderByDateCreationDesc(transporteurId, tenantId);
    }

    @Transactional
    public TarifChauffeur modifierTarif(UUID id, String tenantId, BigDecimal nouveauTarif) {
        TarifChauffeur tarif = tarifAppartenantA(id, tenantId);
        tarif.modifierTarif(nouveauTarif);
        return tarif;
    }

    @Transactional
    public TarifChauffeur toggleActif(UUID id, String tenantId) {
        TarifChauffeur tarif = tarifAppartenantA(id, tenantId);
        tarif.toggleActif();
        return tarif;
    }

    @Transactional
    public void supprimer(UUID id, String tenantId) {
        tarifChauffeurRepository.delete(tarifAppartenantA(id, tenantId));
    }

    // Lecture interne (X-Internal-Service-Key, OPT) : un candidat precis du
    // L1, deja filtre compatible par ailleurs - 404 cote appelant si ce
    // chauffeur n'a pas configure/active de tarif pour cet axe+categorie.
    @Transactional(readOnly = true)
    public Optional<TarifChauffeur> detailPourAppelInterne(UUID transporteurId, UUID axeId, CategorieMarchandise categorie) {
        return tarifChauffeurRepository.findByTransporteurIdAndAxeIdAndCategorieMarchandiseAndActifTrue(
                transporteurId, axeId, categorie);
    }

    // Intervalle affiche au client (plan §1bis) : min/max parmi tous les
    // chauffeurs actuellement actifs sur cet axe+categorie - PAS pondere
    // par le poids de la demande, c'est l'appelant (gateway/service-mkt)
    // qui multiplie par le poids taxable pour obtenir le prix final.
    @Transactional(readOnly = true)
    public Optional<IntervalleTarif> intervallePourAppelInterne(UUID axeId, CategorieMarchandise categorie) {
        List<TarifChauffeur> actifs = tarifChauffeurRepository.findByAxeIdAndCategorieMarchandiseAndActifTrue(axeId, categorie);
        if (actifs.isEmpty()) {
            return Optional.empty();
        }
        BigDecimal min = actifs.stream().map(TarifChauffeur::getTarifFcfaParKg).min(BigDecimal::compareTo).orElseThrow();
        BigDecimal max = actifs.stream().map(TarifChauffeur::getTarifFcfaParKg).max(BigDecimal::compareTo).orElseThrow();
        return Optional.of(new IntervalleTarif(min.setScale(2, RoundingMode.HALF_UP),
                max.setScale(2, RoundingMode.HALF_UP), actifs.size()));
    }

    private TarifChauffeur tarifAppartenantA(UUID id, String tenantId) {
        TarifChauffeur tarif = tarifChauffeurRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tarif introuvable : " + id));
        if (!Objects.equals(tarif.getTenantId(), tenantId)) {
            throw new IllegalArgumentException("Tarif introuvable : " + id);
        }
        return tarif;
    }

    public record IntervalleTarif(BigDecimal tarifMinFcfaParKg, BigDecimal tarifMaxFcfaParKg, int nombreChauffeurs) {
    }
}
