package com.flysoft.fretcorridor.mkt.service;

import com.flysoft.fretcorridor.mkt.client.ServiceCapClient;
import com.flysoft.fretcorridor.mkt.client.ServiceGeoClient;
import com.flysoft.fretcorridor.mkt.client.ServiceNotClient;
import com.flysoft.fretcorridor.mkt.dto.DemandeDto;
import com.flysoft.fretcorridor.mkt.entity.CatalogueEmballage;
import com.flysoft.fretcorridor.mkt.entity.Demande;
import com.flysoft.fretcorridor.mkt.entity.Proposition;
import com.flysoft.fretcorridor.mkt.repository.CatalogueEmballageRepository;
import com.flysoft.fretcorridor.mkt.repository.DemandeRepository;
import com.flysoft.fretcorridor.mkt.repository.PropositionRepository;
import com.flysoft.fretcorridor.mkt.messaging.DemandeAnnuleeEvent;
import com.flysoft.fretcorridor.mkt.messaging.DemandePublieeEvent;
import com.flysoft.fretcorridor.mkt.messaging.MktEventPublisher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class DemandeService {

    // RG-101 (audit de suivi, 23 aout) : meme cle que CalculateurPoidsTaxable
    // cote service-cap (Axe.parametres.coefficientVolumetriqueKgParM3) - un
    // operateur d'axe fixe UNE valeur, exploitee de facon coherente cote
    // marketplace (ce fichier) ET cote declaration de capacite transporteur.
    // Repli sur cette reference globale si l'axe n'a rien configure -
    // alignee sur la valeur de reference deja validee cote service-cap
    // (333.0, remplace l'ancien placeholder 200.0 propre a ce service).
    private static final String CLE_COEFFICIENT_VOLUMETRIQUE = "coefficientVolumetriqueKgParM3";

    private final DemandeRepository demandeRepository;
    private final CatalogueEmballageRepository catalogueRepository;
    private final MktEventPublisher eventPublisher;
    private final PropositionRepository propositionRepository;
    private final ServiceGeoClient serviceGeoClient;
    private final ServiceCapClient serviceCapClient;
    private final ServiceNotClient serviceNotClient;
    private final double coefficientVolumetriqueReference;

    // Constructeur explicite (remplace @RequiredArgsConstructor) : seul moyen
    // de porter @Value sur coefficientVolumetriqueReference tout en gardant
    // le champ final et testable sans contexte Spring (meme pattern que
    // CalculateurPoidsTaxable cote service-cap).
    public DemandeService(DemandeRepository demandeRepository,
                           CatalogueEmballageRepository catalogueRepository,
                           MktEventPublisher eventPublisher,
                           PropositionRepository propositionRepository,
                           ServiceGeoClient serviceGeoClient,
                           ServiceCapClient serviceCapClient,
                           ServiceNotClient serviceNotClient,
                           @Value("${fretcorridor.mkt.coefficient-volumetrique-kg-par-m3:333.0}")
                           double coefficientVolumetriqueReference) {
        this.demandeRepository = demandeRepository;
        this.catalogueRepository = catalogueRepository;
        this.eventPublisher = eventPublisher;
        this.propositionRepository = propositionRepository;
        this.serviceGeoClient = serviceGeoClient;
        this.serviceCapClient = serviceCapClient;
        this.serviceNotClient = serviceNotClient;
        this.coefficientVolumetriqueReference = coefficientVolumetriqueReference;
    }

    // RG-038 : publication exige le niveau KYC 1 minimum
    @Transactional
    public DemandeDto.DemandeResponse publier(
            DemandeDto.PublierRequest request, UUID clientActeurId, String tenantId, String niveauKyc) {

        if ("NIVEAU_0".equals(niveauKyc)) {
            throw new RuntimeException("KYC_INSUFFISANT_NIVEAU_1_REQUIS");
        }

        CatalogueEmballage emballage = catalogueRepository.findById(request.getTypeEmballageId())
                .orElseThrow(() -> new RuntimeException("TYPE_EMBALLAGE_INTROUVABLE"));

        // Resolution de l'axe (service-geo) par nom de ville - ferme le
        // bloquant audit §1.2 : sans axeId, DemandePubliee n'etait jamais
        // emis et le cycle de matching d'OPT ne se declenchait donc jamais.
        // valeursCriteres reste volontairement une map vide (pas de barème en
        // dur invente ici) : seule sa non-nullite conditionne la publication,
        // l'enrichissement reel des criteres restant a la charge du Moteur.
        // Coordonnees de la demande (BUG CORRIGE, audit de suivi) : jamais
        // renseignees jusqu'ici -- Demande.origineLatitude/... existaient
        // deja sur l'entite et etaient deja lues par publierEvenement()
        // ci-dessous, mais rien ne les remplissait jamais, laissant le
        // moteur de matching (service-opt) sans position exploitable pour
        // TOUTE demande. Granularite hub (position reelle du hub resolu),
        // pas une coordonnee inventee -- coherente avec le fait qu'un axe
        // relie deux hubs, pas deux points arbitraires.
        //
        // Resolue AVANT le calcul du poids taxable (BUG CORRIGE, audit de
        // suivi 23 aout) : RG-101 exige un coefficient volumetrique resolu
        // PAR AXE (Axe.parametres), jamais une seule valeur globale figee
        // pour tout le systeme - jusqu'ici COEFFICIENT_VOLUMETRIQUE=200.0
        // etait un placeholder en dur propre a ce service, incoherent avec
        // le coefficient (333.0, egalement resolu par axe) deja applique
        // cote service-cap pour la meme grandeur physique.
        Optional<com.flysoft.fretcorridor.mkt.client.AxeDto> axe =
                serviceGeoClient.resoudreAxe(request.getVilleDepart(), request.getVilleArrivee());
        Optional<UUID> axeId = axe.map(com.flysoft.fretcorridor.mkt.client.AxeDto::id);

        double coefficientVolumetrique = axe.map(com.flysoft.fretcorridor.mkt.client.AxeDto::parametres)
                .map(parametres -> parametres.get(CLE_COEFFICIENT_VOLUMETRIQUE))
                .filter(Number.class::isInstance)
                .map(valeur -> ((Number) valeur).doubleValue())
                .orElse(coefficientVolumetriqueReference);

        double poidsTotal = emballage.getPoidsUnitaireKg() * request.getQuantite();
        double volumeTotal = emballage.getVolumeUnitaireM3() * request.getQuantite();
        double poidsTaxable = Math.max(poidsTotal, volumeTotal * coefficientVolumetrique);

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
                .origineLatitude(axe.map(com.flysoft.fretcorridor.mkt.client.AxeDto::hubOrigineLatitude).orElse(null))
                .origineLongitude(axe.map(com.flysoft.fretcorridor.mkt.client.AxeDto::hubOrigineLongitude).orElse(null))
                .destinationLatitude(axe.map(com.flysoft.fretcorridor.mkt.client.AxeDto::hubDestinationLatitude).orElse(null))
                .destinationLongitude(axe.map(com.flysoft.fretcorridor.mkt.client.AxeDto::hubDestinationLongitude).orElse(null))
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
                    java.math.BigDecimal.valueOf(demande.getPoidsTaxableKg()),
                    demande.getTypeEmballage().getNom(),
                    demande.getQuantite(),
                    demande.getDestinataireNom(),
                    demande.getDestinataireTelephone(),
                    demande.getModeCollecte().name(),
                    demande.getTypeDisponibilite().name(),
                    demande.getPoidsTotalKg(),
                    demande.getGrandeValeur() != null && demande.getGrandeValeur()
            ));
        }

        // Confirmation au chargeur (audit de suivi Mobile : la cloche de
        // notifications n'affichait jamais rien pour ce parcours, alors que
        // c'est le point d'entrée le plus fréquemment testé).
        serviceNotClient.notifier(
                clientActeurId,
                demande.getStatut() == Demande.StatutDemande.PUBLIEE ? "Demande publiée" : "Demande enregistrée",
                demande.getStatut() == Demande.StatutDemande.PUBLIEE
                        ? "Votre demande de transport (%s → %s) a été publiée.".formatted(demande.getVilleDepart(), demande.getVilleArrivee())
                        : "Votre demande a été enregistrée, mais l'axe %s → %s n'est pas encore desservi.".formatted(demande.getVilleDepart(), demande.getVilleArrivee()),
                "INFO_GENERALE",
                demande.getId(),
                tenantId);

        return DemandeDto.DemandeResponse.fromEntity(demande);
    }

    @Transactional(readOnly = true)
    public List<DemandeDto.DemandeResponse> getMesDemandes(UUID clientActeurId, String tenantId) {
        return demandeRepository.findByClientActeurIdAndTenantIdOrderByDateCreationDesc(clientActeurId, tenantId)
                .stream().map(DemandeDto.DemandeResponse::fromEntity).toList();
    }

    // "Annuler" plutot qu'une suppression physique : StatutDemande.ANNULEE
    // existait deja dans l'enum mais n'etait jusqu'ici jamais atteint par
    // aucun code (audit de suivi Mobile, demande CRUD "modifier/supprimer").
    // Refusee si une proposition a deja ete acceptee (RG-039) - annuler a
    // ce stade laisserait une reservation de capacite fantome cote
    // transporteur, deja engagee.
    @Transactional
    public DemandeDto.DemandeResponse annuler(UUID demandeId, String tenantId) {
        Demande demande = demandeRepository.findByIdAndTenantId(demandeId, tenantId)
                .orElseThrow(() -> new RuntimeException("DEMANDE_INTROUVABLE"));

        boolean propositionAcceptee = propositionRepository.findByDemandeIdOrderByRangAsc(demandeId).stream()
                .anyMatch(p -> p.getStatut() == Proposition.Statut.ACCEPTEE);
        if (propositionAcceptee) {
            throw new RuntimeException("DEMANDE_DEJA_ACCEPTEE");
        }
        if (demande.getStatut() == Demande.StatutDemande.ANNULEE) {
            throw new RuntimeException("DEMANDE_DEJA_ANNULEE");
        }

        demande.setStatut(Demande.StatutDemande.ANNULEE);
        demande = demandeRepository.save(demande);

        // Previent le Moteur (service-opt) de retirer cette demande de sa
        // file d'attente - sans ca, une demande annulee restait matchable et
        // pouvait consommer une capacite reelle pour rien (retour utilisateur
        // direct, 22 aout).
        eventPublisher.publierDemandeAnnulee(new DemandeAnnuleeEvent(java.util.UUID.randomUUID(), demande.getId()));

        return DemandeDto.DemandeResponse.fromEntity(demande);
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
