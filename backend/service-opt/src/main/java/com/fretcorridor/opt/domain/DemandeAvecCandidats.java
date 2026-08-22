package com.fretcorridor.opt.domain;

import com.fretcorridor.opt.client.CandidatCoutDto;
import com.fretcorridor.dto.PointGeoDto;

import java.util.List;
import java.util.UUID;

/**
 * Entree du L1 : une demande a apparier avec la liste de ses candidats
 * (capacites) deja filtres par L0 (GEO), chacun avec ses valeurs de critere
 * deja normalisees pour cette paire demande/candidat - meme convention que
 * CoutLotRequest cote service-mat.
 *
 * origineDemande / destinationDemande : necessaires au calcul d'itineraire
 * Valhalla en sortie de L1 (cf CandidatCoutDto pour la meme decision cote
 * capacite). Le CDC S13 rattache origine/destination nativement a l'entite
 * Demande - portees ici des la construction de l'objet plutot que
 * re-derivees plus tard.
 */
public record DemandeAvecCandidats(
        UUID demandeId,
        PointGeoDto origineDemande,
        PointGeoDto destinationDemande,
        // Ajoutes pour la Tarification L4 (CDC S8.9). axeId : selectionne
        // le bareme specifique a l'axe (EF-GEO-02), repli sur le bareme par
        // defaut si absent (cf TarificationL4Service.resoudreBareme).
        // poidsTaxableKg : regle du maximum deja appliquee en amont cote
        // service-cap (EF-CAP-01/02) - portee ici brute, pas recalculee.
        UUID axeId,
        java.math.BigDecimal poidsTaxableKg,
        List<CandidatCoutDto> candidats,
        // Infos marchandise (audit de suivi Mobile) : propagees jusqu'a
        // AffectationConfirmeeEvent -> Mission, pour que l'app Chauffeur
        // sache ce qu'elle transporte. Nullables : l'endpoint de
        // verification manuelle (AffectationL1Controller) n'a pas a les
        // fournir.
        String typeEmballageNom,
        Integer quantite,
        // Infos destinataire (audit de suivi Mobile), meme raisonnement que
        // typeEmballageNom/quantite ci-dessus.
        String destinataireNom,
        String destinataireTelephone,
        String modeCollecte,
        String typeDisponibilite,
        Double poidsTotalKg,
        Boolean grandeValeur
) {
}
