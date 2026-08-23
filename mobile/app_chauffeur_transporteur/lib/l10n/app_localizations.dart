import 'dart:async';

import 'package:flutter/foundation.dart';
import 'package:flutter/widgets.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:intl/intl.dart' as intl;

import 'app_localizations_en.dart';
import 'app_localizations_fr.dart';

// ignore_for_file: type=lint

/// Callers can lookup localized strings with an instance of AppLocalizations
/// returned by `AppLocalizations.of(context)`.
///
/// Applications need to include `AppLocalizations.delegate()` in their app's
/// `localizationDelegates` list, and the locales they support in the app's
/// `supportedLocales` list. For example:
///
/// ```dart
/// import 'l10n/app_localizations.dart';
///
/// return MaterialApp(
///   localizationsDelegates: AppLocalizations.localizationsDelegates,
///   supportedLocales: AppLocalizations.supportedLocales,
///   home: MyApplicationHome(),
/// );
/// ```
///
/// ## Update pubspec.yaml
///
/// Please make sure to update your pubspec.yaml to include the following
/// packages:
///
/// ```yaml
/// dependencies:
///   # Internationalization support.
///   flutter_localizations:
///     sdk: flutter
///   intl: any # Use the pinned version from flutter_localizations
///
///   # Rest of dependencies
/// ```
///
/// ## iOS Applications
///
/// iOS applications define key application metadata, including supported
/// locales, in an Info.plist file that is built into the application bundle.
/// To configure the locales supported by your app, you’ll need to edit this
/// file.
///
/// First, open your project’s ios/Runner.xcworkspace Xcode workspace file.
/// Then, in the Project Navigator, open the Info.plist file under the Runner
/// project’s Runner folder.
///
/// Next, select the Information Property List item, select Add Item from the
/// Editor menu, then select Localizations from the pop-up menu.
///
/// Select and expand the newly-created Localizations item then, for each
/// locale your application supports, add a new item and select the locale
/// you wish to add from the pop-up menu in the Value field. This list should
/// be consistent with the languages listed in the AppLocalizations.supportedLocales
/// property.
abstract class AppLocalizations {
  AppLocalizations(String locale)
      : localeName = intl.Intl.canonicalizedLocale(locale.toString());

  final String localeName;

  static AppLocalizations of(BuildContext context) {
    return Localizations.of<AppLocalizations>(context, AppLocalizations)!;
  }

  static const LocalizationsDelegate<AppLocalizations> delegate =
      _AppLocalizationsDelegate();

  /// A list of this localizations delegate along with the default localizations
  /// delegates.
  ///
  /// Returns a list of localizations delegates containing this delegate along with
  /// GlobalMaterialLocalizations.delegate, GlobalCupertinoLocalizations.delegate,
  /// and GlobalWidgetsLocalizations.delegate.
  ///
  /// Additional delegates can be added by appending to this list in
  /// MaterialApp. This list does not have to be used at all if a custom list
  /// of delegates is preferred or required.
  static const List<LocalizationsDelegate<dynamic>> localizationsDelegates =
      <LocalizationsDelegate<dynamic>>[
    delegate,
    GlobalMaterialLocalizations.delegate,
    GlobalCupertinoLocalizations.delegate,
    GlobalWidgetsLocalizations.delegate,
  ];

  /// A list of this localizations delegate's supported locales.
  static const List<Locale> supportedLocales = <Locale>[
    Locale('en'),
    Locale('fr')
  ];

  /// No description provided for @langueTitre.
  ///
  /// In fr, this message translates to:
  /// **'Langue'**
  String get langueTitre;

  /// No description provided for @langueFrancais.
  ///
  /// In fr, this message translates to:
  /// **'Français'**
  String get langueFrancais;

  /// No description provided for @langueAnglais.
  ///
  /// In fr, this message translates to:
  /// **'English'**
  String get langueAnglais;

  /// No description provided for @welcomeTitre.
  ///
  /// In fr, this message translates to:
  /// **'Roulez,\nlivrez, gagnez'**
  String get welcomeTitre;

  /// No description provided for @welcomeSousTitre.
  ///
  /// In fr, this message translates to:
  /// **'Déclarez vos capacités et acceptez des missions\nsur le corridor CEMAC.'**
  String get welcomeSousTitre;

  /// No description provided for @creerUnCompte.
  ///
  /// In fr, this message translates to:
  /// **'Créer un compte'**
  String get creerUnCompte;

  /// No description provided for @dejaUnCompte.
  ///
  /// In fr, this message translates to:
  /// **'Vous avez déjà un compte ? '**
  String get dejaUnCompte;

  /// No description provided for @connexion.
  ///
  /// In fr, this message translates to:
  /// **'Connexion'**
  String get connexion;

  /// No description provided for @seConnecter.
  ///
  /// In fr, this message translates to:
  /// **'Se connecter'**
  String get seConnecter;

  /// No description provided for @champTelephone.
  ///
  /// In fr, this message translates to:
  /// **'TÉLÉPHONE'**
  String get champTelephone;

  /// No description provided for @champCode.
  ///
  /// In fr, this message translates to:
  /// **'CODE'**
  String get champCode;

  /// No description provided for @codeObligatoire.
  ///
  /// In fr, this message translates to:
  /// **'Code obligatoire'**
  String get codeObligatoire;

  /// No description provided for @codeFormatInvalide.
  ///
  /// In fr, this message translates to:
  /// **'4 à 6 chiffres'**
  String get codeFormatInvalide;

  /// No description provided for @compteClientMessage.
  ///
  /// In fr, this message translates to:
  /// **'Ce compte est un compte client — utilisez l\'app FretCorridor Client.'**
  String get compteClientMessage;

  /// No description provided for @menuTitre.
  ///
  /// In fr, this message translates to:
  /// **'Menu'**
  String get menuTitre;

  /// No description provided for @menuEspaceUtilisateur.
  ///
  /// In fr, this message translates to:
  /// **'Espace utilisateur'**
  String get menuEspaceUtilisateur;

  /// No description provided for @profil.
  ///
  /// In fr, this message translates to:
  /// **'Profil'**
  String get profil;

  /// No description provided for @notifications.
  ///
  /// In fr, this message translates to:
  /// **'Notifications'**
  String get notifications;

  /// No description provided for @langueMenuItem.
  ///
  /// In fr, this message translates to:
  /// **'Langue ({langue})'**
  String langueMenuItem(String langue);

  /// No description provided for @centreAide.
  ///
  /// In fr, this message translates to:
  /// **'Centre d\'aide'**
  String get centreAide;

  /// No description provided for @politiqueConfidentialite.
  ///
  /// In fr, this message translates to:
  /// **'Politique & confidentialité'**
  String get politiqueConfidentialite;

  /// No description provided for @conditionsUtilisation.
  ///
  /// In fr, this message translates to:
  /// **'Conditions d\'utilisation'**
  String get conditionsUtilisation;

  /// No description provided for @parametres.
  ///
  /// In fr, this message translates to:
  /// **'Paramètres'**
  String get parametres;

  /// No description provided for @seDeconnecter.
  ///
  /// In fr, this message translates to:
  /// **'Se déconnecter'**
  String get seDeconnecter;

  /// No description provided for @sectionNotifications.
  ///
  /// In fr, this message translates to:
  /// **'NOTIFICATIONS'**
  String get sectionNotifications;

  /// No description provided for @notificationsPush.
  ///
  /// In fr, this message translates to:
  /// **'Notifications push'**
  String get notificationsPush;

  /// No description provided for @notificationsPushDescription.
  ///
  /// In fr, this message translates to:
  /// **'Propositions reçues, statut des demandes'**
  String get notificationsPushDescription;

  /// No description provided for @sectionGeneral.
  ///
  /// In fr, this message translates to:
  /// **'GÉNÉRAL'**
  String get sectionGeneral;

  /// No description provided for @langue.
  ///
  /// In fr, this message translates to:
  /// **'Langue'**
  String get langue;

  /// No description provided for @versionApp.
  ///
  /// In fr, this message translates to:
  /// **'FretCorridor · Version 1.0.0 (bêta)'**
  String get versionApp;

  /// No description provided for @aideFaqTitre.
  ///
  /// In fr, this message translates to:
  /// **'QUESTIONS FRÉQUENTES'**
  String get aideFaqTitre;

  /// No description provided for @aideQ1.
  ///
  /// In fr, this message translates to:
  /// **'Comment déclarer une capacité de transport ?'**
  String get aideQ1;

  /// No description provided for @aideR1.
  ///
  /// In fr, this message translates to:
  /// **'Depuis l\'accueil, appuyez sur \"Déclarer une capacité\", renseignez l\'axe, le poids disponible et la date, puis validez.'**
  String get aideR1;

  /// No description provided for @aideQ2.
  ///
  /// In fr, this message translates to:
  /// **'Pourquoi mon profil doit être complété ?'**
  String get aideQ2;

  /// No description provided for @aideR2.
  ///
  /// In fr, this message translates to:
  /// **'La complétion du profil (identité + pièce d\'identité) est obligatoire avant de pouvoir déclarer une capacité ou accepter une mission — c\'est une exigence de vérification (KYC).'**
  String get aideR2;

  /// No description provided for @aideQ3.
  ///
  /// In fr, this message translates to:
  /// **'Comment sont attribuées les missions ?'**
  String get aideQ3;

  /// No description provided for @aideR3.
  ///
  /// In fr, this message translates to:
  /// **'Une mission vous est proposée lorsque votre capacité correspond à une demande de chargeur sur le même axe. Vous pouvez l\'accepter ou la refuser.'**
  String get aideR3;

  /// No description provided for @aideQ4.
  ///
  /// In fr, this message translates to:
  /// **'Comment suivre mes missions ?'**
  String get aideQ4;

  /// No description provided for @aideR4.
  ///
  /// In fr, this message translates to:
  /// **'Rendez-vous sur \"Mes missions\" depuis l\'accueil pour voir le statut de chaque mission acceptée.'**
  String get aideR4;

  /// No description provided for @aideContact.
  ///
  /// In fr, this message translates to:
  /// **'Besoin d\'aide supplémentaire ? Contactez l\'agence FretCorridor la plus proche.'**
  String get aideContact;

  /// No description provided for @cguTitre.
  ///
  /// In fr, this message translates to:
  /// **'Conditions d\'utilisation'**
  String get cguTitre;

  /// No description provided for @cguObjetTitre.
  ///
  /// In fr, this message translates to:
  /// **'Objet'**
  String get cguObjetTitre;

  /// No description provided for @cguObjetTexte.
  ///
  /// In fr, this message translates to:
  /// **'FretCorridor met en relation des chargeurs et des transporteurs pour l\'organisation de transports de marchandises. La plateforme ne réalise pas elle-même les transports.'**
  String get cguObjetTexte;

  /// No description provided for @cguCompteTitre.
  ///
  /// In fr, this message translates to:
  /// **'Compte utilisateur'**
  String get cguCompteTitre;

  /// No description provided for @cguCompteTexte.
  ///
  /// In fr, this message translates to:
  /// **'Vous êtes responsable de l\'exactitude des informations fournies lors de l\'inscription et de la complétion de votre profil (KYC). Un compte peut être suspendu en cas d\'information frauduleuse.'**
  String get cguCompteTexte;

  /// No description provided for @cguDemandesTitre.
  ///
  /// In fr, this message translates to:
  /// **'Demandes et propositions'**
  String get cguDemandesTitre;

  /// No description provided for @cguDemandesTexte.
  ///
  /// In fr, this message translates to:
  /// **'Toute demande publiée peut recevoir jusqu\'à 3 propositions classées. Le prix affiché avant acceptation est une estimation ; le prix définitif est fixé au moment de l\'acceptation d\'une proposition.'**
  String get cguDemandesTexte;

  /// No description provided for @cguResponsabilitesTitre.
  ///
  /// In fr, this message translates to:
  /// **'Responsabilités'**
  String get cguResponsabilitesTitre;

  /// No description provided for @cguResponsabilitesTexte.
  ///
  /// In fr, this message translates to:
  /// **'Le chargeur est responsable de l\'exactitude des informations sur la marchandise (poids, nature, destinataire). Le transporteur est responsable de la bonne exécution de la mission acceptée.'**
  String get cguResponsabilitesTexte;

  /// No description provided for @cguModificationTitre.
  ///
  /// In fr, this message translates to:
  /// **'Modification'**
  String get cguModificationTitre;

  /// No description provided for @cguModificationTexte.
  ///
  /// In fr, this message translates to:
  /// **'Ces conditions peuvent évoluer ; les utilisateurs seront informés des changements significatifs via l\'application.'**
  String get cguModificationTexte;

  /// No description provided for @politiqueTitre.
  ///
  /// In fr, this message translates to:
  /// **'Politique de confidentialité'**
  String get politiqueTitre;

  /// No description provided for @politiqueDonneesTitre.
  ///
  /// In fr, this message translates to:
  /// **'Données collectées'**
  String get politiqueDonneesTitre;

  /// No description provided for @politiqueDonneesTexte.
  ///
  /// In fr, this message translates to:
  /// **'Nous collectons les informations nécessaires au fonctionnement de la plateforme : identité déclarée, pièce d\'identité, numéro de téléphone, et les informations liées à vos demandes et missions de transport.'**
  String get politiqueDonneesTexte;

  /// No description provided for @politiqueUtilisationTitre.
  ///
  /// In fr, this message translates to:
  /// **'Utilisation des données'**
  String get politiqueUtilisationTitre;

  /// No description provided for @politiqueUtilisationTexte.
  ///
  /// In fr, this message translates to:
  /// **'Ces données servent à vérifier votre identité (KYC), à assurer la mise en relation entre chargeurs et transporteurs, et à assurer le suivi des missions. Elles ne sont jamais vendues à des tiers.'**
  String get politiqueUtilisationTexte;

  /// No description provided for @politiquePartageTitre.
  ///
  /// In fr, this message translates to:
  /// **'Partage des données'**
  String get politiquePartageTitre;

  /// No description provided for @politiquePartageTexte.
  ///
  /// In fr, this message translates to:
  /// **'Vos informations de contact sont partagées uniquement avec la contrepartie d\'une mission acceptée (chargeur ↔ transporteur), dans la limite nécessaire à son exécution.'**
  String get politiquePartageTexte;

  /// No description provided for @politiqueConservationTitre.
  ///
  /// In fr, this message translates to:
  /// **'Conservation'**
  String get politiqueConservationTitre;

  /// No description provided for @politiqueConservationTexte.
  ///
  /// In fr, this message translates to:
  /// **'Les données sont conservées pendant la durée de votre compte actif, puis archivées conformément aux obligations légales applicables.'**
  String get politiqueConservationTexte;

  /// No description provided for @politiqueDroitsTitre.
  ///
  /// In fr, this message translates to:
  /// **'Vos droits'**
  String get politiqueDroitsTitre;

  /// No description provided for @politiqueDroitsTexte.
  ///
  /// In fr, this message translates to:
  /// **'Vous pouvez demander l\'accès, la correction ou la suppression de vos données personnelles via le centre d\'aide.'**
  String get politiqueDroitsTexte;

  /// No description provided for @notificationsTooltip.
  ///
  /// In fr, this message translates to:
  /// **'Notifications'**
  String get notificationsTooltip;

  /// No description provided for @profilCompleteMessageChauffeur.
  ///
  /// In fr, this message translates to:
  /// **'Profil complété ✅ — vous pouvez déclarer une capacité.'**
  String get profilCompleteMessageChauffeur;

  /// No description provided for @bonjour.
  ///
  /// In fr, this message translates to:
  /// **'Bonjour, {nom}'**
  String bonjour(String nom);

  /// No description provided for @profilACompleter.
  ///
  /// In fr, this message translates to:
  /// **'Profil à compléter'**
  String get profilACompleter;

  /// No description provided for @profilACompleterDescriptionChauffeur.
  ///
  /// In fr, this message translates to:
  /// **'Complétez votre profil pour déclarer une capacité ou accepter une mission.'**
  String get profilACompleterDescriptionChauffeur;

  /// No description provided for @completer.
  ///
  /// In fr, this message translates to:
  /// **'Compléter'**
  String get completer;

  /// No description provided for @mesMissions.
  ///
  /// In fr, this message translates to:
  /// **'Mes missions'**
  String get mesMissions;

  /// No description provided for @mesMissionsDescription.
  ///
  /// In fr, this message translates to:
  /// **'Missions en cours et historique'**
  String get mesMissionsDescription;

  /// No description provided for @declarerCapacite.
  ///
  /// In fr, this message translates to:
  /// **'Déclarer une capacité'**
  String get declarerCapacite;

  /// No description provided for @declarerCapaciteDescription.
  ///
  /// In fr, this message translates to:
  /// **'Proposer un trajet et de la place disponible'**
  String get declarerCapaciteDescription;

  /// No description provided for @maFlotte.
  ///
  /// In fr, this message translates to:
  /// **'Ma flotte'**
  String get maFlotte;

  /// No description provided for @maFlotteDescription.
  ///
  /// In fr, this message translates to:
  /// **'Gérer mes véhicules'**
  String get maFlotteDescription;

  /// No description provided for @soldeEtGains.
  ///
  /// In fr, this message translates to:
  /// **'Solde et gains'**
  String get soldeEtGains;

  /// No description provided for @soldeEtGainsDescription.
  ///
  /// In fr, this message translates to:
  /// **'Consulter mes paiements'**
  String get soldeEtGainsDescription;

  /// No description provided for @axes.
  ///
  /// In fr, this message translates to:
  /// **'Axes'**
  String get axes;

  /// No description provided for @axesDescription.
  ///
  /// In fr, this message translates to:
  /// **'Corridors disponibles'**
  String get axesDescription;

  /// No description provided for @monProfil.
  ///
  /// In fr, this message translates to:
  /// **'Mon profil'**
  String get monProfil;

  /// No description provided for @monProfilDescriptionChauffeur.
  ///
  /// In fr, this message translates to:
  /// **'Identité et niveau KYC'**
  String get monProfilDescriptionChauffeur;

  /// No description provided for @enroler.
  ///
  /// In fr, this message translates to:
  /// **'Enrôler'**
  String get enroler;

  /// No description provided for @roleChauffeur.
  ///
  /// In fr, this message translates to:
  /// **'Chauffeur'**
  String get roleChauffeur;

  /// No description provided for @roleTransporteur.
  ///
  /// In fr, this message translates to:
  /// **'Transporteur'**
  String get roleTransporteur;

  /// No description provided for @roleAgent.
  ///
  /// In fr, this message translates to:
  /// **'Agent'**
  String get roleAgent;

  /// No description provided for @inscriptionSousTitre.
  ///
  /// In fr, this message translates to:
  /// **'Vous compléterez votre profil juste après.'**
  String get inscriptionSousTitre;

  /// No description provided for @jeSuis.
  ///
  /// In fr, this message translates to:
  /// **'JE SUIS'**
  String get jeSuis;

  /// No description provided for @typeChauffeur.
  ///
  /// In fr, this message translates to:
  /// **'Chauffeur'**
  String get typeChauffeur;

  /// No description provided for @typeTransporteur.
  ///
  /// In fr, this message translates to:
  /// **'Transporteur'**
  String get typeTransporteur;

  /// No description provided for @typeChauffeurProprietaire.
  ///
  /// In fr, this message translates to:
  /// **'Les deux'**
  String get typeChauffeurProprietaire;

  /// No description provided for @labelRaisonSociale.
  ///
  /// In fr, this message translates to:
  /// **'RAISON SOCIALE'**
  String get labelRaisonSociale;

  /// No description provided for @hintRaisonSocialeChauffeur.
  ///
  /// In fr, this message translates to:
  /// **'Ex : Transport Fotso SARL'**
  String get hintRaisonSocialeChauffeur;

  /// No description provided for @raisonSocialeObligatoire.
  ///
  /// In fr, this message translates to:
  /// **'Raison sociale obligatoire'**
  String get raisonSocialeObligatoire;

  /// No description provided for @labelPrenom.
  ///
  /// In fr, this message translates to:
  /// **'PRÉNOM'**
  String get labelPrenom;

  /// No description provided for @hintPrenomChauffeur.
  ///
  /// In fr, this message translates to:
  /// **'Ex : Paul'**
  String get hintPrenomChauffeur;

  /// No description provided for @prenomObligatoire.
  ///
  /// In fr, this message translates to:
  /// **'Prénom obligatoire'**
  String get prenomObligatoire;

  /// No description provided for @labelNom.
  ///
  /// In fr, this message translates to:
  /// **'NOM'**
  String get labelNom;

  /// No description provided for @hintNomChauffeur.
  ///
  /// In fr, this message translates to:
  /// **'Ex : Kamga'**
  String get hintNomChauffeur;

  /// No description provided for @nomObligatoire.
  ///
  /// In fr, this message translates to:
  /// **'Nom obligatoire'**
  String get nomObligatoire;

  /// No description provided for @labelCodeInscription.
  ///
  /// In fr, this message translates to:
  /// **'CODE (4 à 6 chiffres)'**
  String get labelCodeInscription;

  /// No description provided for @creerMonCompte.
  ///
  /// In fr, this message translates to:
  /// **'Créer mon compte'**
  String get creerMonCompte;

  /// No description provided for @choisirUnBureau.
  ///
  /// In fr, this message translates to:
  /// **'Choisir un bureau'**
  String get choisirUnBureau;

  /// No description provided for @compteMultiBureau.
  ///
  /// In fr, this message translates to:
  /// **'Votre compte est rattaché à plusieurs bureaux. Choisissez celui avec lequel travailler :'**
  String get compteMultiBureau;

  /// No description provided for @bureauPrincipal.
  ///
  /// In fr, this message translates to:
  /// **'(principal)'**
  String get bureauPrincipal;
}

class _AppLocalizationsDelegate
    extends LocalizationsDelegate<AppLocalizations> {
  const _AppLocalizationsDelegate();

  @override
  Future<AppLocalizations> load(Locale locale) {
    return SynchronousFuture<AppLocalizations>(lookupAppLocalizations(locale));
  }

  @override
  bool isSupported(Locale locale) =>
      <String>['en', 'fr'].contains(locale.languageCode);

  @override
  bool shouldReload(_AppLocalizationsDelegate old) => false;
}

AppLocalizations lookupAppLocalizations(Locale locale) {
  // Lookup logic when only language code is specified.
  switch (locale.languageCode) {
    case 'en':
      return AppLocalizationsEn();
    case 'fr':
      return AppLocalizationsFr();
  }

  throw FlutterError(
      'AppLocalizations.delegate failed to load unsupported locale "$locale". This is likely '
      'an issue with the localizations generation tool. Please file an issue '
      'on GitHub with a reproducible sample app and the gen-l10n configuration '
      'that was used.');
}
