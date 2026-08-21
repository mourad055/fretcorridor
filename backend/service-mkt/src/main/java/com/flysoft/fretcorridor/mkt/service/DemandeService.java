package com.flysoft.fretcorridor.mkt.service;

import com.flysoft.fretcorridor.mkt.client.ServiceCapClient;
import com.flysoft.fretcorridor.mkt.client.ServiceGeoClient;
import com.flysoft.fretcorridor.mkt.dto.DemandeDto;
import com.flysoft.fretcorridor.mkt.entity.CatalogueEmballage;
import com.flysoft.fretcorridor.mkt.entity.Demande;
import com.flysoft.fretcorridor.mkt.entity.Proposition;
import com.flysoft.fretcorridor.mkt.repository.CatalogueEmballageRepository;
import com.flysoft.fretcorridor.mkt.repository.DemandeRepository;
import com.flysoft.fretcorridor.mkt.repository.PropositionRepository;
import com.flysoft.fretcorridor.mkt.messaging.DemandePublieeEvent;
import com.flysoft.fretcorridor.mkt.messaging.MktEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DemandeService {

    // Coefficient volumétrique MVP (kg/m³) — placeholder simple, la vraie
    // tarification (RG-037) relève du moteur, pas de ce service.
    private static final double COEFFICIENT_VOLUMETRIQUE = 200.0;

    private final DemandeRepository demandeRepository;
    private final CatalogueEmballageRepository catalogueRepository;
    private final MktEventPublisher eventPublisher;
    private final PropositionRepository propositionRepository;
    private final ServiceGeoClient serviceGeoClient;
    private final ServiceCapClient serviceCapClient;

    // RG-038 : publication exige le niveau KYC 1 minimum
    @Transactional
    public DemandeDto.DemandeResponse publier(
            DemandeDto.PublierRequest request, UUID clientActeurId, String tenantId, String niveauKyc) {

        if ("NIVEAU_0".equals(niveauKyc)) {
            throw new RuntimeException("KYC_INSUFFISANT_NIVEAU_1_REQUIS");
        }

        CatalogueEmballage emballage = catalogueRepository.findById(request.getTypeEmballageId())
                .orElseThrow(() -> new RuntimeException("TYPE_EMBALLAGE_INTROUVABLE"));

        double poidsTotal = emballage.getPoidsUnitaireKg() * request.getQuantite();
        double volumeTotal = emballage.getVolumeUnitaireM3() * request.getQuantite();
        double poidsTaxable = Math.max(poidsTotal, volumeTotal * COEFFICIENT_VOLUMETRIQUE);

        // Resolution de l'axe (service-geo) par nom de ville - ferme le
        // bloquant audit §1.2 : sans axeId, DemandePubliee n'etait jamais
        // emis et le cycle de matching d'OPT ne se declenchait donc jamais.
        // valeursCriteres reste volontairement une map vide (pas de barème en
        // dur invente ici) : seule sa non-nullite conditionne la publication,
        // l'enrichissement reel des criteres restant a la charge du Moteur.
        Optional<UUID> axeId = serviceGeoClient.resoudreAxe(request.getVilleDepart(), request.getVilleArrivee());

        Demande demande = Demande.builder()
                .clientActeurId(clientActeurId)
                .villeDepart(request.getVilleDepart())
                .villeArrivee(request.getVilleArrivee())
                .typeEmballage(emballage)
                .quantite(request.getQuantite())
                .poidsTotalKg(poidsTotal)
                .volumeTotalM3(volumeTotal)
                .poidsTaxableKg(poidsTaxable)
                .fragile(request.getFragile() != null ? request.getFragile() : emballage.getFragileParDefaut())
                .perissable(request.getPerissable())
                .dangereuse(request.getDangereuse())
                .grandeValeur(request.getGrandeValeur())
                .typeDisponibilite(Demande.TypeDisponibilite.valueOf(request.getTypeDisponibilite()))
                .dateDisponibilite(request.getDateDisponibilite())
                .modeCollecte(Demande.ModeCollecte.valueOf(request.getModeCollecte()))
                .destinataireNom(request.getDestinataireNom())
                .destinataireTelephone(request.getDestinataireTelephone())
                .tenantId(tenantId)
                .axeId(axeId.orElse(null))
                .valeursCriteres(axeId.isPresent() ? Map.of() : null)
                .statut(axeId.isPresent() ? Demande.StatutDemande.PUBLIEE : Demande.StatutDemande.AXE_NON_DESSERVI)
                .build();

        demande = demandeRepository.save(demande);

        // Publication best-effort (ENF-DIS-04) : une demande dont l'axe/les
        // criteres ne sont pas encore renseignes est quand meme sauvegardee,
        // mais n'est pas publiee vers le Moteur tant que ces champs manquent -
        // evite d'envoyer un evenement incomplet que OPT devrait rejeter.
        if (demande.getAxeId() != null && demande.getValeursCriteres() != null) {
            eventPublisher.publierDemandePubliee(new DemandePublieeEvent(
                    java.util.UUID.randomUUID(),
                    demande.getId(),
                    demande.getAxeId(),
                    demande.getValeursCriteres(),
                    (demande.getOrigineLatitude() != null && demande.getOrigineLongitude() != null)
                            ? new DemandePublieeEvent.PointGeo(demande.getOrigineLatitude(), demande.getOrigineLongitude())
                            : null,
                    (demande.getDestinationLatitude() != null && demande.getDestinationLongitude() != null)
                            ? new DemandePublieeEvent.PointGeo(demande.getDestinationLatitude(), demande.getDestinationLongitude())
                            : null,
                    java.math.BigDecimal.valueOf(demande.getPoidsTaxableKg())
            ));
        }

        return DemandeDto.DemandeResponse.fromEntity(demande);
    }

    @Transactional(readOnly = true)
    public List<DemandeDto.DemandeResponse> getMesDemandes(UUID clientActeurId, String tenantId) {
        return demandeRepository.findByClientActeurIdAndTenantIdOrderByDateCreationDesc(clientActeurId, tenantId)
                .stream().map(DemandeDto.DemandeResponse::fromEntity).toList();
    }

    // S5 — plus un stub : lit les Proposition persistees par
    // PropositionEmiseListener (Kafka, evenement publie par service-opt).
    @Transactional(readOnly = true)
    public List<DemandeDto.PropositionResponse> getPropositions(UUID demandeId, String tenantId) {
        demandeRepository.findByIdAndTenantId(demandeId, tenantId)
                .orElseThrow(() -> new RuntimeException("DEMANDE_INTROUVABLE"));

        return propositionRepository.findByDemandeIdOrderByRangAsc(demandeId).stream()
                .map(p -> DemandeDto.PropositionResponse.builder()
                        .id(p.getId())
                        .rang(p.getRang())
                        .motifClassement(p.getMotifClassement())
                        .prixEstime(p.getPrixTransport() != null ? p.getPrixTransport().toString() : null)
                        .statut(p.getStatut().name())
                        .build())
                .toList();
    }

    // RG-039/EF-MKT-08 : le chargeur choisit une des au plus 3 propositions
    // reçues (EF-MKT-07) -- marque celle-ci ACCEPTEE et les autres de la même
    // demande EXPIREE. La réservation atomique de capacité est désormais
    // réelle (ServiceCapClient, audit de suivi Mobile) -- si elle échoue,
    // toute la transaction est annulée (aucune Proposition marquée ACCEPTEE
    // sans réservation effective côté transporteur).
    @Transactional
    public DemandeDto.PropositionResponse accepterProposition(UUID demandeId, UUID propositionId, String tenantId) {
        Demande demande = demandeRepository.findByIdAndTenantId(demandeId, tenantId)
                .orElseThrow(() -> new RuntimeException("DEMANDE_INTROUVABLE"));

        Proposition proposition = propositionRepository.findByIdAndDemandeId(propositionId, demandeId)
                .orElseThrow(() -> new RuntimeException("PROPOSITION_INTROUVABLE"));

        if (proposition.getStatut() != Proposition.Statut.EN_ATTENTE) {
            throw new RuntimeException("PROPOSITION_DEJA_TRAITEE");
        }

        if (proposition.getCapaciteId() != null && demande.getPoidsTaxableKg() != null) {
            serviceCapClient.reserver(
                    proposition.getCapaciteId(),
                    java.math.BigDecimal.valueOf(demande.getPoidsTaxableKg()),
                    propositionId.toString());
        }

        List<Proposition> propositionsDeLaDemande = propositionRepository.findByDemandeIdOrderByRangAsc(demandeId);
        for (Proposition p : propositionsDeLaDemande) {
            if (p.getId().equals(propositionId)) {
                p.setStatut(Proposition.Statut.ACCEPTEE);
            } else if (p.getStatut() == Proposition.Statut.EN_ATTENTE) {
                p.setStatut(Proposition.Statut.EXPIREE);
            }
        }
        propositionRepository.saveAll(propositionsDeLaDemande);

        return DemandeDto.PropositionResponse.builder()
                .id(proposition.getId())
                .rang(proposition.getRang())
                .motifClassement(proposition.getMotifClassement())
                .prixEstime(proposition.getPrixTransport() != null ? proposition.getPrixTransport().toString() : null)
                .statut(proposition.getStatut().name())
                .build();
    }
}
