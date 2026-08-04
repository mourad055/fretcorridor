package com.fretcorridor.opt.tarification;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Prix decompose en ses termes (RG-115 - "tout prix affiche est
 * decomposable en ses termes pour l'utilisateur"), structure du S8.9.1 :
 *   PRIX = COUT_BASE + COUT_UNITAIRE x POIDS_TAXABLE + SOMME(COUT_SERVICES)
 *          +/- FACTEUR_TENSION
 *
 * HYPOTHESE D'EQUIPE SUR RG-116 (a confirmer avec le porteur PAY - le CDC
 * dit la commission "distincte", mais ne precise pas si le chargeur la
 * paie en plus, ou si elle est prelevee sur le transporteur) :
 *   - prixTransport = ce que le chargeur voit et paie pour le transport
 *   - commissionPlateforme = prelevee SUR ce montant, cote reversement
 *     transporteur (donc montantVerseTransporteur = prixTransport - commission)
 *   - le chargeur ne paie jamais prixTransport + commission
 * Cette hypothese conditionne le futur cablage avec service-pay - a
 * verifier explicitement avant l'increment PAY (Sprint 8).
 *
 * modeDegrade = true si le calcul n'a pas pu utiliser un bareme reel
 * (aucun bareme actif trouve, ni specifique a l'axe ni par defaut) ou si
 * le regime POIDS_TAXABLE necessitait une distance Valhalla indisponible -
 * dans ce cas, prixTransport est null : jamais de prix invente en
 * silencieux remplacement (meme principe que ValhallaClient/itineraire).
 */
public record TarificationResultat(
        UUID baremeId,
        Integer baremeVersion,
        String regime,
        BigDecimal coutBase,
        BigDecimal coutVariablePoidsTaxable,
        BigDecimal coutServices,
        BigDecimal facteurTensionApplique,
        BigDecimal prixTransportAvantPlancher,
        boolean plancherApplique,
        BigDecimal prixTransport,
        BigDecimal commissionPlateforme,
        BigDecimal montantVerseTransporteur,
        boolean modeDegrade
) {
}
