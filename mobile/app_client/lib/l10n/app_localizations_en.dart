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
}
