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
}
