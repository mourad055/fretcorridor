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
  String get welcomeTitre => 'Drive,\ndeliver, earn';

  @override
  String get welcomeSousTitre =>
      'Declare your capacities and accept missions\nacross the CEMAC corridor.';

  @override
  String get creerUnCompte => 'Create an account';

  @override
  String get dejaUnCompte => 'Already have an account? ';

  @override
  String get connexion => 'Log in';

  @override
  String get seConnecter => 'Log in';

  @override
  String get champTelephone => 'PHONE';

  @override
  String get champCode => 'CODE';

  @override
  String get codeObligatoire => 'Code required';

  @override
  String get codeFormatInvalide => '4 to 6 digits';

  @override
  String get compteClientMessage =>
      'This is a shipper account — use the FretCorridor Client app.';

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
  String get aideFaqTitre => 'FREQUENTLY ASKED QUESTIONS';

  @override
  String get aideQ1 => 'How do I declare a transport capacity?';

  @override
  String get aideR1 =>
      'From the home screen, tap \"Declare a capacity\", enter the corridor, available weight and date, then confirm.';

  @override
  String get aideQ2 => 'Why does my profile need to be completed?';

  @override
  String get aideR2 =>
      'Completing your profile (identity + ID document) is required before you can declare a capacity or accept a mission — it\'s a verification requirement (KYC).';

  @override
  String get aideQ3 => 'How are missions assigned?';

  @override
  String get aideR3 =>
      'A mission is offered to you when your capacity matches a shipper\'s request on the same corridor. You can accept or decline it.';

  @override
  String get aideQ4 => 'How do I track my missions?';

  @override
  String get aideR4 =>
      'Go to \"My missions\" from the home screen to see the status of each accepted mission.';

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
  String get notificationsTooltip => 'Notifications';

  @override
  String get profilCompleteMessageChauffeur =>
      'Profile completed ✅ — you can now declare a capacity.';

  @override
  String bonjour(String nom) {
    return 'Hello, $nom';
  }

  @override
  String get profilACompleter => 'Profile to complete';

  @override
  String get profilACompleterDescriptionChauffeur =>
      'Complete your profile to declare a capacity or accept a mission.';

  @override
  String get completer => 'Complete';

  @override
  String get mesMissions => 'My missions';

  @override
  String get mesMissionsDescription => 'Current and past missions';

  @override
  String get declarerCapacite => 'Declare a capacity';

  @override
  String get declarerCapaciteDescription => 'Offer a route and available space';

  @override
  String get maFlotte => 'My fleet';

  @override
  String get maFlotteDescription => 'Manage my vehicles';

  @override
  String get soldeEtGains => 'Balance and earnings';

  @override
  String get soldeEtGainsDescription => 'View my payments';

  @override
  String get axes => 'Corridors';

  @override
  String get axesDescription => 'Available corridors';

  @override
  String get monProfil => 'My profile';

  @override
  String get monProfilDescriptionChauffeur => 'Identity and KYC level';

  @override
  String get enroler => 'Enroll';

  @override
  String get roleChauffeur => 'Driver';

  @override
  String get roleTransporteur => 'Carrier';

  @override
  String get roleAgent => 'Agent';

  @override
  String get inscriptionSousTitre =>
      'You\'ll complete your profile right after.';

  @override
  String get jeSuis => 'I AM';

  @override
  String get typeChauffeur => 'Driver';

  @override
  String get typeTransporteur => 'Carrier';

  @override
  String get typeChauffeurProprietaire => 'Both';

  @override
  String get labelRaisonSociale => 'COMPANY NAME';

  @override
  String get hintRaisonSocialeChauffeur => 'E.g.: Transport Fotso SARL';

  @override
  String get raisonSocialeObligatoire => 'Company name required';

  @override
  String get labelPrenom => 'FIRST NAME';

  @override
  String get hintPrenomChauffeur => 'E.g.: Paul';

  @override
  String get prenomObligatoire => 'First name required';

  @override
  String get labelNom => 'LAST NAME';

  @override
  String get hintNomChauffeur => 'E.g.: Kamga';

  @override
  String get nomObligatoire => 'Last name required';

  @override
  String get labelCodeInscription => 'CODE (4 to 6 digits)';

  @override
  String get creerMonCompte => 'Create my account';

  @override
  String get choisirUnBureau => 'Choose an agency';

  @override
  String get compteMultiBureau =>
      'Your account is linked to several agencies. Choose which one to work with:';

  @override
  String get bureauPrincipal => '(main)';

  @override
  String get statutEnAttente => 'Pending';

  @override
  String get statutPriseEnCharge => 'Picked up';

  @override
  String get statutEnTransit => 'In transit';

  @override
  String get statutLivree => 'Delivered';

  @override
  String get statutAnnulee => 'Cancelled';

  @override
  String get etapeLivraison => 'Delivery';

  @override
  String get incidentLabel => 'Incident';

  @override
  String get disponibiliteDesQuePossible => 'As soon as possible';

  @override
  String get disponibiliteDatePrecise => 'On a set date';

  @override
  String get disponibilitePlage => 'Within a time window';

  @override
  String get collecteDomicile => 'Pickup at address';

  @override
  String get collectePointRelais => 'Pickup at relay point';

  @override
  String get aucuneMissionPourLeMoment => 'No missions for now.';

  @override
  String get idDeLaMission => 'Mission ID:';

  @override
  String destinataireSansTel(String nom) {
    return 'Recipient: $nom';
  }

  @override
  String destinataireAvecTel(String nom, String telephone) {
    return 'Recipient: $nom · $telephone';
  }

  @override
  String poidsTotalLabel(String poids) {
    return 'Total weight: $poids kg';
  }

  @override
  String typeLabel(String type) {
    return 'Type: $type';
  }

  @override
  String publieeLe(String date) {
    return 'Published on $date';
  }

  @override
  String get valeurLabel => 'Value: ';

  @override
  String get grandeValeur => 'High value';

  @override
  String get faitPartieTourneeGroupee => 'Part of a consolidated route';

  @override
  String get chronologie => 'Timeline';

  @override
  String get aucuneEtapePourLeMoment => 'No steps for now.';

  @override
  String statutAvecValeur(String statut) {
    return 'Status: $statut';
  }

  @override
  String get suiviGpsActif => 'GPS tracking active';

  @override
  String get voirLePlanDeChargement => 'View loading plan';

  @override
  String get signalerUnIncident => 'Report an incident';

  @override
  String get confirmerLaLivraison => 'Confirm delivery';

  @override
  String get destinataire => 'Recipient';

  @override
  String get grilleDecisionNote =>
      'Decision and appeal process handled by the office — not yet automated in the app.';

  @override
  String get categorieLabel => 'CATEGORY';

  @override
  String get descriptionLabel => 'DESCRIPTION';

  @override
  String get detaillezOptionnel => 'Describe what happened (optional)';

  @override
  String get ajouterPhotoOptionnel => 'Add a photo (optional)';

  @override
  String get photoJointe => 'Photo attached';

  @override
  String get envoyerLeSignalement => 'Send report';

  @override
  String get preuveDePriseEnCharge => 'Pickup proof';

  @override
  String get preuveDeLivraison => 'Delivery proof';

  @override
  String get photoEtSignatureObligatoires =>
      'A photo and a signature are required.';

  @override
  String get photosAuMoinsUn => 'PHOTOS (at least 1)';

  @override
  String get signatureDuDestinataire => 'RECIPIENT\'S SIGNATURE';

  @override
  String get effacer => 'Clear';

  @override
  String get valider => 'Confirm';

  @override
  String get noteRg070 =>
      'Photo(s) of the goods + recipient\'s signature — required (RG-070).';

  @override
  String get categorieRetard => 'Delay';

  @override
  String get categorieMarchandiseEndommagee => 'Damaged goods';

  @override
  String get categorieAccident => 'Accident';

  @override
  String get categoriePanneVehicule => 'Vehicle breakdown';

  @override
  String get categorieAutre => 'Other';

  @override
  String get aucuneNotification => 'No notifications.';

  @override
  String get refuser => 'Decline';

  @override
  String get accepter => 'Accept';

  @override
  String get promoTitre1 => 'Find missions quickly';

  @override
  String get promoDesc1 =>
      'Declare your capacity, receive offers on your routes';

  @override
  String get promoTitre2 => 'Secure payment';

  @override
  String get promoDesc2 => 'Track your earnings and payments right in the app';

  @override
  String get promoTitre3 => 'Real-time GPS tracking';

  @override
  String get promoDesc3 => 'Share your position during your missions';

  @override
  String get tourneeGroupee => 'Grouped route';

  @override
  String envoiGroupeEtapes(int n) {
    return 'Grouped shipment — $n stops';
  }

  @override
  String get aucuneEtapeTermineePourLeMoment => 'No completed stops yet.';

  @override
  String get enlevementLabel => 'Pickup';

  @override
  String demandeIdLabel(String id) {
    return 'Request $id';
  }

  @override
  String get confirmerEnlevement => 'Confirm pickup';

  @override
  String get toutesEtapesTerminees => 'All stops on this route are complete.';

  @override
  String get historique => 'History';

  @override
  String get aucuneEcriturePourLeMoment => 'No entries yet.';

  @override
  String get soldeLabel => 'BALANCE';

  @override
  String get natureEncaissement => 'Collection';

  @override
  String get natureReversement => 'Payout';

  @override
  String get natureCommission => 'Commission';

  @override
  String get natureSequestre => 'Escrow';

  @override
  String get modeMonnaieElectronique => 'Mobile money';

  @override
  String get modeVirement => 'Bank transfer';

  @override
  String get modeTermeContractuel => 'Contractual term';

  @override
  String get modeEspeces => 'Cash';

  @override
  String regleVia(String mode) {
    return 'Paid via $mode';
  }

  @override
  String get badgeVisible => 'Visible';

  @override
  String get badgeMatching => 'Matching';

  @override
  String get badgePaiement => 'Payment';

  @override
  String get ajouter => 'Add';

  @override
  String get aucunVehiculeEnregistre =>
      'No vehicle registered.\nTap \"Add\" to declare one.';

  @override
  String get nouveauVehicule => 'New vehicle';

  @override
  String get modifierLeVehicule => 'Edit vehicle';

  @override
  String get supprimerCeVehicule => 'Delete this vehicle?';

  @override
  String get carteGriseRecto => 'Registration card (front)';

  @override
  String get carteGriseVerso => 'Registration card (back)';

  @override
  String poidsMaxLabel(String poids) {
    return 'Max weight: $poids t';
  }

  @override
  String essieuxLabel(String nombre) {
    return 'Axles: $nombre';
  }

  @override
  String get typeDeVehicule => 'Vehicle type';

  @override
  String get champObligatoire => 'Required field';

  @override
  String get immatriculationFacultatif => 'License plate (optional)';

  @override
  String get poidsMaxTonnesFacultatif => 'Max weight (tonnes, optional)';

  @override
  String get nombreEssieuxFacultatif => 'Number of axles (optional)';

  @override
  String get matieresDangereuses => 'Hazardous materials';

  @override
  String get enregistrer => 'Save';

  @override
  String get kycPhotoNonReconnue =>
      'This photo doesn\'t look like an ID document — frame the document clearly (readable text) and try again.';

  @override
  String get profilCompleteEmoji => 'Profile completed ✅';

  @override
  String get telephoneLabel => 'Phone';

  @override
  String get typeDeCompte => 'Account type';

  @override
  String get entreprise => 'Business';

  @override
  String get particulier => 'Individual';

  @override
  String get pieceDeposeeLabel => 'Document submitted';

  @override
  String get modifier => 'Edit';

  @override
  String get modifierNumeroTelephone => 'Change phone number';

  @override
  String numeroActuelLabel(String telephone) {
    return 'Current number: $telephone';
  }

  @override
  String get confirmezNumeroActuel => 'Confirm your current number';

  @override
  String get nouveauNumero => 'New number';

  @override
  String get annuler => 'Cancel';

  @override
  String get numeroTelephoneMisAJour => 'Phone number updated.';

  @override
  String get echecModification => 'Update failed.';

  @override
  String get completezVotreProfil => 'Complete your profile';

  @override
  String get identitePieceCondition =>
      'Declared identity and submitted document — required to publish or accept a mission (RG-011).';

  @override
  String get identite => 'Identity';

  @override
  String get pieceIdentite => 'ID document';

  @override
  String get verificationEnCours => 'Checking…';

  @override
  String get envoiEnCours => 'Sending…';

  @override
  String get prendrePhotoIdentite => 'Take a photo of my ID document';

  @override
  String get numeroRegistreCommerceFacultatif =>
      'BUSINESS REGISTRATION NUMBER (optional)';

  @override
  String get enrolerUnChauffeur => 'Enroll a driver';

  @override
  String get synchroniserFileOffline => 'Sync offline queue';

  @override
  String enrolementsEnAttenteSync(int n) {
    String _temp0 = intl.Intl.pluralLogic(
      n,
      locale: localeName,
      other: '$n enrollments awaiting sync (offline).',
      one: '$n enrollment awaiting sync (offline).',
    );
    return '$_temp0';
  }

  @override
  String get nouvelEnrolementTitre => 'New enrollment';

  @override
  String get codeActivationSmsMessage =>
      'The activation code is sent by SMS directly to the person\'s phone — never to yours.';

  @override
  String get typeSectionLabel => 'TYPE';

  @override
  String get typeChauffeurProprietaireEnrolement => 'Owner-driver';

  @override
  String get telephoneDeLaPersonneLabel => 'PERSON\'S PHONE NUMBER';

  @override
  String get telephoneObligatoire => 'Phone number required';

  @override
  String get formatInvalide => 'Invalid format';

  @override
  String get envoyerLeCode => 'Send code';

  @override
  String get activerLeCompte => 'Activate account';

  @override
  String get codeEtPinParLaPersonneMessage =>
      'To be entered by the person themselves: the code received by SMS, then a PIN of their choice.';

  @override
  String get codeRecuParSmsLabel => 'CODE RECEIVED BY SMS';

  @override
  String get codeSixChiffres => '6-digit code';

  @override
  String get nouveauCodePinLabel => 'NEW PIN CODE (4-6 digits)';

  @override
  String get pinObligatoire => 'PIN required';

  @override
  String get planDeChargementTitre => 'Loading plan';

  @override
  String get planChargementNonDisponibleMessage =>
      'Loading plan not yet available for this route — the Engine hasn\'t computed it yet (or this route is too simple to require one).';

  @override
  String etapePlanLabel(int rang, String type, String demandeId) {
    return 'Step $rang — $type — Request $demandeId';
  }

  @override
  String get repartitionApproximativeMessage =>
      'Approximate distribution (total weight spread evenly across axles)';

  @override
  String get choisissezUnAxe => 'Choose a corridor.';

  @override
  String get choisissezUnVehicule => 'Choose a vehicle.';

  @override
  String get choisissezUneDateDepart => 'Choose a departure date.';

  @override
  String get capaciteModifiee => 'Capacity updated.';

  @override
  String get nouvelleCapaciteAncienneEchouee =>
      'New capacity declared, but the previous one couldn\'t be deleted.';

  @override
  String capacitePubliee(String kg) {
    return 'Capacity published — $kg taxable kg.';
  }

  @override
  String get capaciteEnregistree => 'Capacity saved.';

  @override
  String get modifierLaCapacite => 'Edit capacity';

  @override
  String get mesCapacites => 'My capacities';

  @override
  String get axeLabel => 'CORRIDOR';

  @override
  String get chargementEnCours => 'Loading…';

  @override
  String get choisirUnAxe => 'Choose a corridor';

  @override
  String get vehiculeLabel => 'VEHICLE';

  @override
  String get gererMaFlotte => 'Manage my fleet';

  @override
  String get aucunVehiculeAjoutezEnUn =>
      'No vehicle — add one via \"Manage my fleet\".';

  @override
  String get choisirUnVehicule => 'Choose a vehicle';

  @override
  String get poidsDisponibleKgLabel => 'AVAILABLE WEIGHT (KG)';

  @override
  String get nombreInvalide => 'Invalid number';

  @override
  String get departLabel => 'DEPARTURE';

  @override
  String get choisirDateEtHeure => 'Choose a date and time';

  @override
  String get declarerLaCapacite => 'Declare capacity';

  @override
  String get enregistrerLesModifications => 'Save changes';

  @override
  String get supprimerCetteCapacite => 'Delete this capacity?';

  @override
  String get actionDefinitive => 'This action is permanent.';

  @override
  String get supprimer => 'Delete';

  @override
  String get capaciteSupprimee => 'Capacity deleted.';

  @override
  String get suppressionImpossible => 'Deletion isn\'t possible right now.';

  @override
  String get jourLun => 'Mon.';

  @override
  String get jourMar => 'Tue.';

  @override
  String get jourMer => 'Wed.';

  @override
  String get jourJeu => 'Thu.';

  @override
  String get jourVen => 'Fri.';

  @override
  String get jourSam => 'Sat.';

  @override
  String get jourDim => 'Sun.';

  @override
  String get aucuneCapaciteDeclareePourLeMoment => 'No capacity declared yet';

  @override
  String get expiree => 'Expired';

  @override
  String get publiee => 'Published';

  @override
  String kgDisponibles(String kg) {
    return '$kg kg available';
  }

  @override
  String departLabelValeur(String date) {
    return 'Departure: $date';
  }

  @override
  String declareeLe(String date) {
    return 'Declared on $date';
  }

  @override
  String get mesPropositionsMission => 'My proposals';

  @override
  String get mesPropositionsMissionDescription =>
      'Missions to accept or decline';

  @override
  String get remunerationLabel => 'Payment';

  @override
  String expireDans(String secondes) {
    return 'Expires in ${secondes}s';
  }

  @override
  String get propositionExpiree => 'Expired';

  @override
  String get aucunePropositionMission => 'No pending proposal right now.';

  @override
  String get poidsLabel => 'Weight';

  @override
  String get distanceLabel => 'Distance';

  @override
  String get modeCollecteLabel => 'Pickup mode';

  @override
  String get disponibiliteLabel => 'Availability';

  @override
  String get marchandiseLabel => 'Goods';

  @override
  String get destinataireLabel => 'Recipient';

  @override
  String get motifRefusTitre => 'Why are you declining this mission?';

  @override
  String get motifTropLoin => 'Too far from my location';

  @override
  String get motifIndisponible => 'Not available on this slot';

  @override
  String get motifRemunerationInsuffisante => 'Payment too low';

  @override
  String get motifVehiculeInadapte => 'Vehicle not suited';

  @override
  String get motifAutre => 'Other reason';

  @override
  String get confirmerRefus => 'Confirm decline';

  @override
  String get missionAcceptee => 'Mission accepted!';

  @override
  String get missionRefusee => 'Mission declined.';

  @override
  String get propositionIndisponible =>
      'This proposal is no longer available (already answered or expired).';
}
