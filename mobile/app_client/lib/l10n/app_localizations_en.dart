// ignore: unused_import
import 'package:intl/intl.dart' as intl;
import 'app_localizations.dart';

// ignore_for_file: type=lint

/// The translations for English (`en`).
class AppLocalizationsEn extends AppLocalizations {
  AppLocalizationsEn([String locale = 'en']) : super(locale);

  @override
  String get langueTitre => 'Language';

  @override
  String get langueFrancais => 'Français';

  @override
  String get langueAnglais => 'English';

  @override
  String get welcomeTitre => 'Your shipments,\nmade simple';

  @override
  String get welcomeSousTitre =>
      'Post your request and connect with\ncarriers across the CEMAC network.';

  @override
  String get commencer => 'Get started';

  @override
  String get dejaUnCompte => 'Already have an account? ';

  @override
  String get connexion => 'Log in';

  @override
  String get seConnecter => 'Log in';

  @override
  String get loginSousTitre => 'Send your goods, simply';

  @override
  String get champTelephone => 'PHONE';

  @override
  String get champCodePin => 'PIN CODE';

  @override
  String get pinObligatoire => 'PIN required';

  @override
  String get pinFormatInvalide => '4 to 6 digits';

  @override
  String get pasEncoreDeCompte => 'Don\'t have an account yet? Create one';

  @override
  String get menuTitre => 'Menu';

  @override
  String get menuEspaceUtilisateur => 'User area';

  @override
  String get profil => 'Profile';

  @override
  String get notifications => 'Notifications';

  @override
  String get notifTitrePropositionRecue => 'New offer';

  @override
  String get notifTitreStatutMission => 'Mission update';

  @override
  String get notifTitreInfoGenerale => 'Information';

  @override
  String get notifTitrePropositionRetour => 'Return trip offer';

  @override
  String get notifTitreAlerteEcart => 'Alert';

  @override
  String langueMenuItem(String langue) {
    return 'Language ($langue)';
  }

  @override
  String get centreAide => 'Help center';

  @override
  String get politiqueConfidentialite => 'Privacy policy';

  @override
  String get conditionsUtilisation => 'Terms of use';

  @override
  String get parametres => 'Settings';

  @override
  String get seDeconnecter => 'Log out';

  @override
  String get sectionNotifications => 'NOTIFICATIONS';

  @override
  String get notificationsPush => 'Push notifications';

  @override
  String get notificationsPushDescription =>
      'New offers received, request status';

  @override
  String get sectionGeneral => 'GENERAL';

  @override
  String get langue => 'Language';

  @override
  String get versionApp => 'FretCorridor · Version 1.0.0 (beta)';

  @override
  String get chargeurDefaut => 'Shipper';

  @override
  String get profilCompleteMessage =>
      'Profile completed ✅ — you can now post a request.';

  @override
  String bonjour(String nom) {
    return 'Hello, $nom';
  }

  @override
  String get marketplaceCemac => 'CEMAC Marketplace';

  @override
  String get profilACompleter => 'Profile to complete';

  @override
  String get profilACompleterDescription =>
      'Complete your profile to post a request.';

  @override
  String get completer => 'Complete';

  @override
  String get envoyerMarchandise => 'Send a shipment';

  @override
  String get envoyerMarchandiseDescription =>
      'Post a request via the packaging catalog';

  @override
  String get monProfil => 'My profile';

  @override
  String get monProfilDescription => 'Personal information and KYC level';

  @override
  String get aideFaqTitre => 'FREQUENTLY ASKED QUESTIONS';

  @override
  String get aideQ1 => 'How do I post a transport request?';

  @override
  String get aideR1 =>
      'From the home screen, tap \"Send a shipment\", enter the location, type and quantity of goods, then the recipient\'s details.';

  @override
  String get aideQ2 => 'Why does my profile need to be completed?';

  @override
  String get aideR2 =>
      'Completing your profile (identity + ID document) is required before you can post a request — it\'s a verification requirement (KYC).';

  @override
  String get aideQ3 => 'Is the displayed price final?';

  @override
  String get aideR3 =>
      'No, the price shown when posting is an estimate. The final price is the one in the offer you accept.';

  @override
  String get aideQ4 => 'How do I track my requests?';

  @override
  String get aideR4 =>
      'Go to \"My requests\" from the home screen to see the status of each request and the offers received.';

  @override
  String get aideContact =>
      'Need more help? Contact the nearest FretCorridor agency.';

  @override
  String get cguTitre => 'Terms of use';

  @override
  String get cguObjetTitre => 'Purpose';

  @override
  String get cguObjetTexte =>
      'FretCorridor connects shippers and carriers to organize the transport of goods. The platform does not carry out the transport itself.';

  @override
  String get cguCompteTitre => 'User account';

  @override
  String get cguCompteTexte =>
      'You are responsible for the accuracy of the information provided when registering and completing your profile (KYC). An account may be suspended in case of fraudulent information.';

  @override
  String get cguDemandesTitre => 'Requests and offers';

  @override
  String get cguDemandesTexte =>
      'Any posted request may receive up to 3 ranked offers. The price shown before acceptance is an estimate; the final price is set when an offer is accepted.';

  @override
  String get cguResponsabilitesTitre => 'Responsibilities';

  @override
  String get cguResponsabilitesTexte =>
      'The shipper is responsible for the accuracy of the goods information (weight, nature, recipient). The carrier is responsible for properly carrying out the accepted mission.';

  @override
  String get cguModificationTitre => 'Changes';

  @override
  String get cguModificationTexte =>
      'These terms may evolve; users will be informed of significant changes via the app.';

  @override
  String get politiqueTitre => 'Privacy policy';

  @override
  String get politiqueDonneesTitre => 'Data collected';

  @override
  String get politiqueDonneesTexte =>
      'We collect the information necessary for the platform to operate: declared identity, ID document, phone number, and information related to your requests and transport missions.';

  @override
  String get politiqueUtilisationTitre => 'Use of data';

  @override
  String get politiqueUtilisationTexte =>
      'This data is used to verify your identity (KYC), connect shippers and carriers, and track missions. It is never sold to third parties.';

  @override
  String get politiquePartageTitre => 'Data sharing';

  @override
  String get politiquePartageTexte =>
      'Your contact information is shared only with the counterparty of an accepted mission (shipper ↔ carrier), to the extent necessary for its execution.';

  @override
  String get politiqueConservationTitre => 'Retention';

  @override
  String get politiqueConservationTexte =>
      'Data is retained for as long as your account is active, then archived in accordance with applicable legal obligations.';

  @override
  String get politiqueDroitsTitre => 'Your rights';

  @override
  String get politiqueDroitsTexte =>
      'You may request access to, correction of, or deletion of your personal data via the help center.';

  @override
  String get creerUnCompte => 'Create an account';

  @override
  String get particulier => 'Individual';

  @override
  String get entreprise => 'Company';

  @override
  String get labelRaisonSociale => 'COMPANY NAME';

  @override
  String get hintRaisonSociale => 'E.g.: Cimencam SA';

  @override
  String get raisonSocialeObligatoire => 'Company name required';

  @override
  String get labelPrenom => 'FIRST NAME';

  @override
  String get hintPrenom => 'E.g.: Awa';

  @override
  String get prenomObligatoire => 'First name required';

  @override
  String get labelNom => 'LAST NAME';

  @override
  String get hintNom => 'E.g.: Mballa';

  @override
  String get nomObligatoire => 'Last name required';

  @override
  String get labelCodePinInscription => 'PIN CODE (4 to 6 digits)';

  @override
  String get hintCodePin => 'E.g.: 1234';

  @override
  String get creerMonCompte => 'Create my account';

  @override
  String get aucuneNotification => 'No notifications for now.';

  @override
  String get paiement => 'Payment';

  @override
  String get intentionReglementInfo =>
      'This choice indicates your payment intent — actual collection happens separately via the approved provider.';

  @override
  String get choisirMoyenReglement => 'Choose your payment method';

  @override
  String get confirmer => 'Confirm';

  @override
  String moyenReglementRetenu(String moyen) {
    return 'Payment method selected: $moyen.';
  }

  @override
  String get especes => 'Cash';

  @override
  String get promoTitre1 => 'Ship anywhere in Cameroon';

  @override
  String get promoDesc1 =>
      'Hundreds of verified carriers across the CEMAC corridor';

  @override
  String get promoTitre2 => 'Real-time tracking';

  @override
  String get promoDesc2 => 'Track your shipment from pickup to delivery';

  @override
  String get promoTitre3 => 'Verified carriers';

  @override
  String get promoDesc3 => 'Every driver goes through an identity check';

  @override
  String get signalerLitige => 'Report an issue';

  @override
  String missionConcernee(String id) {
    return 'Related mission: $id';
  }

  @override
  String get motif => 'REASON';

  @override
  String get description => 'DESCRIPTION';

  @override
  String get hintDescriptionLitige => 'Describe the problem you encountered';

  @override
  String get envoyerSignalement => 'Send report';

  @override
  String get litigeConfirmation =>
      'Your report has been sent. The office will get back to you.';

  @override
  String get mesDemandes => 'My requests';

  @override
  String get nouvelleDemande => 'New request';

  @override
  String get aucuneDemande => 'No requests posted yet.';

  @override
  String get annulerCetteDemandeTitre => 'Cancel this request?';

  @override
  String get annulerCetteDemandeContenu => 'This action is final.';

  @override
  String get retour => 'Back';

  @override
  String get annulerLaDemande => 'Cancel the request';

  @override
  String get demandeAnnulee => 'Request cancelled.';

  @override
  String get prochaineAEtreServie => 'Next to be served';

  @override
  String positionDansLaFile(int rang) {
    return 'Position $rang in the queue';
  }

  @override
  String get fragile => 'Fragile';

  @override
  String get perissable => 'Perishable';

  @override
  String get dangereuse => 'Hazardous';

  @override
  String get grandeValeur => 'High value';

  @override
  String get voirLesPropositions => 'See offers';

  @override
  String get voirEstimation => 'See the estimate';

  @override
  String get suivi => 'Tracking';

  @override
  String get modifier => 'Edit';

  @override
  String get annuler => 'Cancel';

  @override
  String get desQuePossible => 'As soon as possible';

  @override
  String get datePrecise => 'Specific date';

  @override
  String get plageHoraire => 'Time window';

  @override
  String get collecteADomicile => 'Home pickup';

  @override
  String get pointRelais => 'Drop-off point';

  @override
  String destinataireLabel(String nom, String telephone) {
    return 'Recipient: $nom · $telephone';
  }

  @override
  String publieeLe(String date) {
    return 'Posted on $date';
  }

  @override
  String get propositions => 'Offers';

  @override
  String get aucuneProposition => 'No offers yet';

  @override
  String get aucunePropositionDescription =>
      'Your request is waiting to be matched with an available carrier on this corridor.';

  @override
  String get prixEnCoursCalcul => 'Price being calculated';

  @override
  String get estimationTitre => 'Estimate';

  @override
  String get fourchetteDePrix => 'PRICE RANGE';

  @override
  String get entre => 'Between';

  @override
  String chauffeursCompatibles(int n) {
    return '$n compatible drivers right now';
  }

  @override
  String get poidsTaxableLabel => 'Taxable weight';

  @override
  String get tarifsDesChauffeurs => 'Drivers\' rates';

  @override
  String get prixDunChauffeur => 'A driver\'s price';

  @override
  String get poidsFoisTarif => 'weight × their rate';

  @override
  String get negocierLaFourchette => 'Negotiate the range';

  @override
  String get negocierFacultatif => 'Optional · before the search only';

  @override
  String get votrePrixMaximum => 'Your maximum price';

  @override
  String get negoAvertissement =>
      'Below every current rate: no driver will receive the request.';

  @override
  String negoInfo(String plafond) {
    return 'Only drivers whose price is ≤ $plafond F will receive the request.';
  }

  @override
  String validationFondsBloques(String min, String max) {
    return 'By validating, you accept any price between $min and $max FCFA. $max F will be held (not charged) while we search.';
  }

  @override
  String get validerEtRechercherChauffeur => 'Validate and find a driver';

  @override
  String get etape1sur2 => 'STEP 1 / 2';

  @override
  String get etape2sur2 => 'STEP 2 / 2';

  @override
  String get fondsBloquesEnAttente => 'Funds held, waiting for a driver';

  @override
  String get paiementConfirmeChauffeurEnRoute =>
      'Payment confirmed, driver on the way';

  @override
  String get montantBloque => 'Amount held';

  @override
  String get preleveActuellement => 'Charged so far';

  @override
  String get compteLabel => 'Account';

  @override
  String get fourchetteAcceptee => 'Accepted range';

  @override
  String get etapePaiement1 => 'A driver accepts: only their price is charged.';

  @override
  String get etapePaiement2 =>
      'The rest of the held amount is released to you right away.';

  @override
  String get etapePaiement3 =>
      'No one in 15 min: everything is released, nothing is charged.';

  @override
  String get lancerLaRecherche => 'Start the search';

  @override
  String get montantPreleve => 'Charged · driver\'s price';

  @override
  String get montantDebloque => 'Released to your account';

  @override
  String get sequestreNote =>
      'The payment stays in escrow: it is only released to the driver after proof of delivery.';

  @override
  String get suivreLeChauffeur => 'Track the driver';

  @override
  String get diffusionTitre => 'Searching for a driver';

  @override
  String get expireDansLabel => 'EXPIRES IN';

  @override
  String get sur1500 => 'out of 15:00';

  @override
  String get rechercheChauffeurEnCours => 'Looking for a driver…';

  @override
  String get diffusionDescription =>
      'Your request is sent at the same time to every compatible driver. The first to accept takes the mission.';

  @override
  String get demoChauffeurAccepte => 'Demo: a driver accepts';

  @override
  String get offreExpireeTitre => 'Offer expired, republish?';

  @override
  String get offreExpireeDescription =>
      'No driver accepted within 15 minutes. Your funds were released, nothing was charged.';

  @override
  String get republier => 'Republish';

  @override
  String get revoirMonPrix => 'Review my price';

  @override
  String get statutAcceptee => 'Accepted';

  @override
  String get statutExpiree => 'Expired';

  @override
  String get statutEnAttente => 'Pending';

  @override
  String get accepterCetteProposition => 'Accept this offer';

  @override
  String get propositionAcceptee => 'Offer accepted ✅';

  @override
  String get dateSpecifique => 'On a specific date';

  @override
  String get surPlageHoraire => 'Within a time window';

  @override
  String get collecteEnPointRelais => 'Drop-off point pickup';

  @override
  String get suiviTitre => 'Delivery tracking';

  @override
  String get suiviPasDisponible => 'Tracking not available yet';

  @override
  String get suiviPasDisponibleDescription =>
      'Tracking starts as soon as a carrier takes charge of your request.';

  @override
  String get envoiGroupe =>
      'Grouped shipment: your parcel is part of a consolidated route with other shipments.';

  @override
  String get vehiculeEnMouvement => 'Vehicle in motion';

  @override
  String get positionMiseAJourInstant => 'Position updated just now';

  @override
  String positionMiseAJourDepuis(int minutes) {
    return 'Position updated $minutes min ago';
  }

  @override
  String get positionGpsIndisponible => 'GPS position not available yet.';

  @override
  String get votreChauffeur => 'Your driver';

  @override
  String get nomChauffeurIndisponible => 'Driver\'s name not available';

  @override
  String get appeler => 'Call';

  @override
  String get numeroNonDisponible => 'Phone number not available yet.';

  @override
  String get etapesTitre => 'Stages';

  @override
  String get aucuneEtape => 'No stage recorded yet.';

  @override
  String get choisirMoyenPaiement => 'Choose payment method';

  @override
  String get modifierLaDemande => 'Edit request';

  @override
  String get sectionLieu => 'Location';

  @override
  String get axeFacultatif => 'CORRIDOR (OPTIONAL)';

  @override
  String get champVilleDepart => 'DEPARTURE CITY';

  @override
  String get hintVilleDepart => 'E.g.: Yaoundé';

  @override
  String get obligatoire => 'Required';

  @override
  String get champVilleArrivee => 'ARRIVAL CITY';

  @override
  String get hintVilleArrivee => 'E.g.: Douala';

  @override
  String get sectionMarchandise => 'Goods';

  @override
  String get typeMarchandise => 'TYPE OF GOODS';

  @override
  String get catalogueIndisponible => 'Catalog unavailable for now';

  @override
  String get reessayer => 'Retry';

  @override
  String get selectionnerLeType => 'Select the type';

  @override
  String get choisirTypeMarchandise => 'Choose a type of goods';

  @override
  String get quantiteNombreUnites => 'QUANTITY (NUMBER OF UNITS)';

  @override
  String quantiteNombreDe(String nom) {
    return 'QUANTITY (NUMBER OF \"$nom\")';
  }

  @override
  String get unites => 'unit(s)';

  @override
  String get nombreInvalide => 'Invalid number';

  @override
  String get poidsTotalLabel => 'Total weight';

  @override
  String get volumeTotalLabel => 'Total volume';

  @override
  String vehiculeAdapte(String vehicule) {
    return 'Suitable vehicle: $vehicule';
  }

  @override
  String get vehiculeCamionnette => 'Van (up to 500 kg)';

  @override
  String get vehiculeFourgon => 'Light truck (up to 1.5 t)';

  @override
  String get vehiculeCamionLeger => 'Light truck 3.5T (up to 3.5 t)';

  @override
  String get vehiculeCamionMoyen => 'Medium truck 8T (up to 8 t)';

  @override
  String get vehiculeCamionLourd => 'Heavy truck 20T (up to 20 t)';

  @override
  String get vehiculeSemiRemorque => 'Semi-trailer (over 20 t)';

  @override
  String get naturesParticulieres => 'SPECIAL HANDLING';

  @override
  String get sectionModalites => 'Details';

  @override
  String get disponibiliteLabel => 'AVAILABILITY';

  @override
  String get dansUnePlage => 'Within a window';

  @override
  String get modeCollecteLabel => 'PICKUP METHOD';

  @override
  String get aDomicile => 'At home';

  @override
  String get categorieMarchandiseLabel => 'GOODS CATEGORY';

  @override
  String get categorieMarchandiseStandard => 'Standard';

  @override
  String get categorieMarchandiseStandardDescription =>
      'Handles normal handling and stacking.';

  @override
  String get categorieMarchandiseFragile => 'Fragile';

  @override
  String get categorieMarchandiseFragileDescription =>
      'Can break: needs bracing and straps, nothing heavy on top.';

  @override
  String get categorieMarchandiseUltraFragile => 'Extra fragile';

  @override
  String get categorieMarchandiseUltraFragileDescription =>
      'Very shock-sensitive: never stacked, loaded last.';

  @override
  String get sectionDestinataire => 'Recipient';

  @override
  String get hintDestinataireNom => 'E.g.: Paul Nkomo';

  @override
  String get cniDestinataireLabel => 'Recipient\'s ID (optional)';

  @override
  String get cniDestinataireDescription =>
      'Will be checked at delivery to confirm the identity of the person receiving the goods.';

  @override
  String get prendreUnePhoto => 'Take a photo';

  @override
  String get cniAjoutee => 'ID added';

  @override
  String get changerCni => 'Change';

  @override
  String get telephoneRenseigner => 'Enter the recipient\'s phone number';

  @override
  String get prixEstimationMessage =>
      'The price shown will be an estimate — the final price comes with the accepted offer.';

  @override
  String nouvelleDemandeAnnulationEchouee(String erreur) {
    return 'New request posted, but the old one could not be cancelled: $erreur';
  }

  @override
  String get demandeModifiee => 'Request updated.';

  @override
  String get publierLaDemande => 'Post the request';

  @override
  String get enregistrerModifications => 'Save changes';

  @override
  String get kyc1Info => 'This level (KYC 1) lets you post transport requests.';

  @override
  String get etapeIdentite => 'Identity';

  @override
  String get etapePieceIdentite => 'ID document';

  @override
  String get pieceInvalideMessage =>
      'This photo doesn\'t look like an ID document — frame the document well (readable text) and try again.';

  @override
  String get phoneLabel => 'Phone';

  @override
  String get typeDeCompte => 'Account type';

  @override
  String get pieceDeposeeLabel => 'Document submitted';

  @override
  String get modifierNumeroTelephone => 'Change phone number';

  @override
  String numeroActuel(String numero) {
    return 'Current number: $numero';
  }

  @override
  String get confirmezNumeroActuel => 'Confirm your current number';

  @override
  String get nouveauNumero => 'New number';

  @override
  String get champObligatoire => 'Required field';

  @override
  String get valider => 'Confirm';

  @override
  String get numeroTelephoneMisAJour => 'Phone number updated.';

  @override
  String get echecModification => 'Failed to update.';

  @override
  String get numeroRccmOptionnel => 'RCCM NUMBER (optional)';

  @override
  String get hintRccm => 'E.g.: RC/DLA/2024/B/1234';

  @override
  String get verificationEnCours => 'Checking…';

  @override
  String get envoiEnCours => 'Sending…';

  @override
  String get prendreEnPhotoPiece => 'Take a photo of my ID document';

  @override
  String get enregistrer => 'Save';
}
