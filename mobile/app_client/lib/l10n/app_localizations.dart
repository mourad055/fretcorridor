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
  /// **'Vos envois,\nsans complications'**
  String get welcomeTitre;

  /// No description provided for @welcomeSousTitre.
  ///
  /// In fr, this message translates to:
  /// **'Publiez votre demande et connectez-vous aux\ntransporteurs du réseau CEMAC.'**
  String get welcomeSousTitre;

  /// No description provided for @commencer.
  ///
  /// In fr, this message translates to:
  /// **'Commencer'**
  String get commencer;

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

  /// No description provided for @loginSousTitre.
  ///
  /// In fr, this message translates to:
  /// **'Envoyez vos marchandises, simplement'**
  String get loginSousTitre;

  /// No description provided for @champTelephone.
  ///
  /// In fr, this message translates to:
  /// **'TÉLÉPHONE'**
  String get champTelephone;

  /// No description provided for @champCodePin.
  ///
  /// In fr, this message translates to:
  /// **'CODE PIN'**
  String get champCodePin;

  /// No description provided for @pinObligatoire.
  ///
  /// In fr, this message translates to:
  /// **'PIN obligatoire'**
  String get pinObligatoire;

  /// No description provided for @pinFormatInvalide.
  ///
  /// In fr, this message translates to:
  /// **'4 à 6 chiffres'**
  String get pinFormatInvalide;

  /// No description provided for @pasEncoreDeCompte.
  ///
  /// In fr, this message translates to:
  /// **'Pas encore de compte ? Créer un compte'**
  String get pasEncoreDeCompte;

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

  /// No description provided for @chargeurDefaut.
  ///
  /// In fr, this message translates to:
  /// **'Chargeur'**
  String get chargeurDefaut;

  /// No description provided for @profilCompleteMessage.
  ///
  /// In fr, this message translates to:
  /// **'Profil complété ✅ — vous pouvez publier une demande.'**
  String get profilCompleteMessage;

  /// No description provided for @bonjour.
  ///
  /// In fr, this message translates to:
  /// **'Bonjour, {nom}'**
  String bonjour(String nom);

  /// No description provided for @marketplaceCemac.
  ///
  /// In fr, this message translates to:
  /// **'Marketplace CEMAC'**
  String get marketplaceCemac;

  /// No description provided for @profilACompleter.
  ///
  /// In fr, this message translates to:
  /// **'Profil à compléter'**
  String get profilACompleter;

  /// No description provided for @profilACompleterDescription.
  ///
  /// In fr, this message translates to:
  /// **'Complétez votre profil pour publier une demande.'**
  String get profilACompleterDescription;

  /// No description provided for @completer.
  ///
  /// In fr, this message translates to:
  /// **'Compléter'**
  String get completer;

  /// No description provided for @envoyerMarchandise.
  ///
  /// In fr, this message translates to:
  /// **'Envoyer une marchandise'**
  String get envoyerMarchandise;

  /// No description provided for @envoyerMarchandiseDescription.
  ///
  /// In fr, this message translates to:
  /// **'Publiez une demande via le catalogue d\'emballages'**
  String get envoyerMarchandiseDescription;

  /// No description provided for @monProfil.
  ///
  /// In fr, this message translates to:
  /// **'Mon profil'**
  String get monProfil;

  /// No description provided for @monProfilDescription.
  ///
  /// In fr, this message translates to:
  /// **'Informations personnelles et niveau KYC'**
  String get monProfilDescription;

  /// No description provided for @aideFaqTitre.
  ///
  /// In fr, this message translates to:
  /// **'QUESTIONS FRÉQUENTES'**
  String get aideFaqTitre;

  /// No description provided for @aideQ1.
  ///
  /// In fr, this message translates to:
  /// **'Comment publier une demande de transport ?'**
  String get aideQ1;

  /// No description provided for @aideR1.
  ///
  /// In fr, this message translates to:
  /// **'Depuis l\'accueil, appuyez sur \"Envoyer une marchandise\", renseignez le lieu, le type et la quantité de marchandise, puis les informations du destinataire.'**
  String get aideR1;

  /// No description provided for @aideQ2.
  ///
  /// In fr, this message translates to:
  /// **'Pourquoi mon profil doit être complété ?'**
  String get aideQ2;

  /// No description provided for @aideR2.
  ///
  /// In fr, this message translates to:
  /// **'La complétion du profil (identité + pièce d\'identité) est obligatoire avant de pouvoir publier une demande — c\'est une exigence de vérification (KYC).'**
  String get aideR2;

  /// No description provided for @aideQ3.
  ///
  /// In fr, this message translates to:
  /// **'Le prix affiché est-il définitif ?'**
  String get aideQ3;

  /// No description provided for @aideR3.
  ///
  /// In fr, this message translates to:
  /// **'Non, le prix affiché lors de la publication est une estimation. Le prix définitif est celui de la proposition que vous acceptez.'**
  String get aideR3;

  /// No description provided for @aideQ4.
  ///
  /// In fr, this message translates to:
  /// **'Comment suivre mes demandes ?'**
  String get aideQ4;

  /// No description provided for @aideR4.
  ///
  /// In fr, this message translates to:
  /// **'Rendez-vous sur \"Mes demandes\" depuis l\'accueil pour voir le statut de chaque demande et ses propositions reçues.'**
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

  /// No description provided for @creerUnCompte.
  ///
  /// In fr, this message translates to:
  /// **'Créer un compte'**
  String get creerUnCompte;

  /// No description provided for @particulier.
  ///
  /// In fr, this message translates to:
  /// **'Particulier'**
  String get particulier;

  /// No description provided for @entreprise.
  ///
  /// In fr, this message translates to:
  /// **'Entreprise'**
  String get entreprise;

  /// No description provided for @labelRaisonSociale.
  ///
  /// In fr, this message translates to:
  /// **'RAISON SOCIALE'**
  String get labelRaisonSociale;

  /// No description provided for @hintRaisonSociale.
  ///
  /// In fr, this message translates to:
  /// **'Ex : Cimencam SA'**
  String get hintRaisonSociale;

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

  /// No description provided for @hintPrenom.
  ///
  /// In fr, this message translates to:
  /// **'Ex : Awa'**
  String get hintPrenom;

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

  /// No description provided for @hintNom.
  ///
  /// In fr, this message translates to:
  /// **'Ex : Mballa'**
  String get hintNom;

  /// No description provided for @nomObligatoire.
  ///
  /// In fr, this message translates to:
  /// **'Nom obligatoire'**
  String get nomObligatoire;

  /// No description provided for @labelCodePinInscription.
  ///
  /// In fr, this message translates to:
  /// **'CODE PIN (4 à 6 chiffres)'**
  String get labelCodePinInscription;

  /// No description provided for @hintCodePin.
  ///
  /// In fr, this message translates to:
  /// **'Ex : 1234'**
  String get hintCodePin;

  /// No description provided for @creerMonCompte.
  ///
  /// In fr, this message translates to:
  /// **'Créer mon compte'**
  String get creerMonCompte;

  /// No description provided for @aucuneNotification.
  ///
  /// In fr, this message translates to:
  /// **'Aucune notification pour le moment.'**
  String get aucuneNotification;

  /// No description provided for @paiement.
  ///
  /// In fr, this message translates to:
  /// **'Paiement'**
  String get paiement;

  /// No description provided for @intentionReglementInfo.
  ///
  /// In fr, this message translates to:
  /// **'Ce choix indique votre intention de règlement — l\'encaissement effectif se fait séparément via le prestataire agréé.'**
  String get intentionReglementInfo;

  /// No description provided for @choisirMoyenReglement.
  ///
  /// In fr, this message translates to:
  /// **'Choisissez votre moyen de règlement'**
  String get choisirMoyenReglement;

  /// No description provided for @confirmer.
  ///
  /// In fr, this message translates to:
  /// **'Confirmer'**
  String get confirmer;

  /// No description provided for @moyenReglementRetenu.
  ///
  /// In fr, this message translates to:
  /// **'Moyen de règlement retenu : {moyen}.'**
  String moyenReglementRetenu(String moyen);

  /// No description provided for @especes.
  ///
  /// In fr, this message translates to:
  /// **'Espèces'**
  String get especes;

  /// No description provided for @promoTitre1.
  ///
  /// In fr, this message translates to:
  /// **'Envoyez partout au Cameroun'**
  String get promoTitre1;

  /// No description provided for @promoDesc1.
  ///
  /// In fr, this message translates to:
  /// **'Des centaines de transporteurs vérifiés sur le corridor CEMAC'**
  String get promoDesc1;

  /// No description provided for @promoTitre2.
  ///
  /// In fr, this message translates to:
  /// **'Suivi en temps réel'**
  String get promoTitre2;

  /// No description provided for @promoDesc2.
  ///
  /// In fr, this message translates to:
  /// **'Suivez votre marchandise du départ jusqu\'à la livraison'**
  String get promoDesc2;

  /// No description provided for @promoTitre3.
  ///
  /// In fr, this message translates to:
  /// **'Transporteurs vérifiés'**
  String get promoTitre3;

  /// No description provided for @promoDesc3.
  ///
  /// In fr, this message translates to:
  /// **'Chaque chauffeur passe par une vérification d\'identité'**
  String get promoDesc3;

  /// No description provided for @signalerLitige.
  ///
  /// In fr, this message translates to:
  /// **'Signaler un litige'**
  String get signalerLitige;

  /// No description provided for @missionConcernee.
  ///
  /// In fr, this message translates to:
  /// **'Mission concernée : {id}'**
  String missionConcernee(String id);

  /// No description provided for @motif.
  ///
  /// In fr, this message translates to:
  /// **'MOTIF'**
  String get motif;

  /// No description provided for @description.
  ///
  /// In fr, this message translates to:
  /// **'DESCRIPTION'**
  String get description;

  /// No description provided for @hintDescriptionLitige.
  ///
  /// In fr, this message translates to:
  /// **'Décrivez le problème rencontré'**
  String get hintDescriptionLitige;

  /// No description provided for @envoyerSignalement.
  ///
  /// In fr, this message translates to:
  /// **'Envoyer le signalement'**
  String get envoyerSignalement;

  /// No description provided for @litigeConfirmation.
  ///
  /// In fr, this message translates to:
  /// **'Votre signalement a été transmis. Le Bureau reviendra vers vous.'**
  String get litigeConfirmation;

  /// No description provided for @mesDemandes.
  ///
  /// In fr, this message translates to:
  /// **'Mes demandes'**
  String get mesDemandes;

  /// No description provided for @nouvelleDemande.
  ///
  /// In fr, this message translates to:
  /// **'Nouvelle demande'**
  String get nouvelleDemande;

  /// No description provided for @aucuneDemande.
  ///
  /// In fr, this message translates to:
  /// **'Aucune demande publiée pour le moment.'**
  String get aucuneDemande;

  /// No description provided for @annulerCetteDemandeTitre.
  ///
  /// In fr, this message translates to:
  /// **'Annuler cette demande ?'**
  String get annulerCetteDemandeTitre;

  /// No description provided for @annulerCetteDemandeContenu.
  ///
  /// In fr, this message translates to:
  /// **'Cette action est définitive.'**
  String get annulerCetteDemandeContenu;

  /// No description provided for @retour.
  ///
  /// In fr, this message translates to:
  /// **'Retour'**
  String get retour;

  /// No description provided for @annulerLaDemande.
  ///
  /// In fr, this message translates to:
  /// **'Annuler la demande'**
  String get annulerLaDemande;

  /// No description provided for @demandeAnnulee.
  ///
  /// In fr, this message translates to:
  /// **'Demande annulée.'**
  String get demandeAnnulee;

  /// No description provided for @prochaineAEtreServie.
  ///
  /// In fr, this message translates to:
  /// **'Prochaine à être servie'**
  String get prochaineAEtreServie;

  /// No description provided for @positionDansLaFile.
  ///
  /// In fr, this message translates to:
  /// **'Position {rang} dans la file'**
  String positionDansLaFile(int rang);

  /// No description provided for @fragile.
  ///
  /// In fr, this message translates to:
  /// **'Fragile'**
  String get fragile;

  /// No description provided for @perissable.
  ///
  /// In fr, this message translates to:
  /// **'Périssable'**
  String get perissable;

  /// No description provided for @dangereuse.
  ///
  /// In fr, this message translates to:
  /// **'Dangereuse'**
  String get dangereuse;

  /// No description provided for @grandeValeur.
  ///
  /// In fr, this message translates to:
  /// **'Grande valeur'**
  String get grandeValeur;

  /// No description provided for @voirLesPropositions.
  ///
  /// In fr, this message translates to:
  /// **'Voir les propositions'**
  String get voirLesPropositions;

  /// No description provided for @suivi.
  ///
  /// In fr, this message translates to:
  /// **'Suivi'**
  String get suivi;

  /// No description provided for @modifier.
  ///
  /// In fr, this message translates to:
  /// **'Modifier'**
  String get modifier;

  /// No description provided for @annuler.
  ///
  /// In fr, this message translates to:
  /// **'Annuler'**
  String get annuler;

  /// No description provided for @desQuePossible.
  ///
  /// In fr, this message translates to:
  /// **'Dès que possible'**
  String get desQuePossible;

  /// No description provided for @datePrecise.
  ///
  /// In fr, this message translates to:
  /// **'Date précise'**
  String get datePrecise;

  /// No description provided for @plageHoraire.
  ///
  /// In fr, this message translates to:
  /// **'Plage horaire'**
  String get plageHoraire;

  /// No description provided for @collecteADomicile.
  ///
  /// In fr, this message translates to:
  /// **'Collecte à domicile'**
  String get collecteADomicile;

  /// No description provided for @pointRelais.
  ///
  /// In fr, this message translates to:
  /// **'Point relais'**
  String get pointRelais;

  /// No description provided for @destinataireLabel.
  ///
  /// In fr, this message translates to:
  /// **'Destinataire : {nom} · {telephone}'**
  String destinataireLabel(String nom, String telephone);

  /// No description provided for @publieeLe.
  ///
  /// In fr, this message translates to:
  /// **'Publiée le {date}'**
  String publieeLe(String date);

  /// No description provided for @propositions.
  ///
  /// In fr, this message translates to:
  /// **'Propositions'**
  String get propositions;

  /// No description provided for @aucuneProposition.
  ///
  /// In fr, this message translates to:
  /// **'Aucune proposition pour le moment'**
  String get aucuneProposition;

  /// No description provided for @aucunePropositionDescription.
  ///
  /// In fr, this message translates to:
  /// **'Votre demande est en attente d\'appariement avec un transporteur disponible sur cet axe.'**
  String get aucunePropositionDescription;

  /// No description provided for @prixEnCoursCalcul.
  ///
  /// In fr, this message translates to:
  /// **'Prix en cours de calcul'**
  String get prixEnCoursCalcul;

  /// No description provided for @statutAcceptee.
  ///
  /// In fr, this message translates to:
  /// **'Acceptée'**
  String get statutAcceptee;

  /// No description provided for @statutExpiree.
  ///
  /// In fr, this message translates to:
  /// **'Expirée'**
  String get statutExpiree;

  /// No description provided for @statutEnAttente.
  ///
  /// In fr, this message translates to:
  /// **'En attente'**
  String get statutEnAttente;

  /// No description provided for @accepterCetteProposition.
  ///
  /// In fr, this message translates to:
  /// **'Accepter cette proposition'**
  String get accepterCetteProposition;

  /// No description provided for @propositionAcceptee.
  ///
  /// In fr, this message translates to:
  /// **'Proposition acceptée ✅'**
  String get propositionAcceptee;

  /// No description provided for @dateSpecifique.
  ///
  /// In fr, this message translates to:
  /// **'À date précise'**
  String get dateSpecifique;

  /// No description provided for @surPlageHoraire.
  ///
  /// In fr, this message translates to:
  /// **'Sur une plage horaire'**
  String get surPlageHoraire;

  /// No description provided for @collecteEnPointRelais.
  ///
  /// In fr, this message translates to:
  /// **'Collecte en point relais'**
  String get collecteEnPointRelais;

  /// No description provided for @suiviTitre.
  ///
  /// In fr, this message translates to:
  /// **'Suivi de ma livraison'**
  String get suiviTitre;

  /// No description provided for @suiviPasDisponible.
  ///
  /// In fr, this message translates to:
  /// **'Suivi pas encore disponible'**
  String get suiviPasDisponible;

  /// No description provided for @suiviPasDisponibleDescription.
  ///
  /// In fr, this message translates to:
  /// **'Le suivi démarre dès qu\'un transporteur prend en charge votre demande.'**
  String get suiviPasDisponibleDescription;

  /// No description provided for @envoiGroupe.
  ///
  /// In fr, this message translates to:
  /// **'Envoi groupé : votre colis fait partie d\'une tournée consolidée avec d\'autres envois.'**
  String get envoiGroupe;

  /// No description provided for @vehiculeEnMouvement.
  ///
  /// In fr, this message translates to:
  /// **'Véhicule en mouvement'**
  String get vehiculeEnMouvement;

  /// No description provided for @positionMiseAJourInstant.
  ///
  /// In fr, this message translates to:
  /// **'Position mise à jour à l\'instant'**
  String get positionMiseAJourInstant;

  /// No description provided for @positionMiseAJourDepuis.
  ///
  /// In fr, this message translates to:
  /// **'Position mise à jour il y a {minutes} min'**
  String positionMiseAJourDepuis(int minutes);

  /// No description provided for @positionGpsIndisponible.
  ///
  /// In fr, this message translates to:
  /// **'Position GPS pas encore disponible.'**
  String get positionGpsIndisponible;

  /// No description provided for @etapesTitre.
  ///
  /// In fr, this message translates to:
  /// **'Étapes'**
  String get etapesTitre;

  /// No description provided for @aucuneEtape.
  ///
  /// In fr, this message translates to:
  /// **'Aucune étape enregistrée pour le moment.'**
  String get aucuneEtape;

  /// No description provided for @choisirMoyenPaiement.
  ///
  /// In fr, this message translates to:
  /// **'Choisir le moyen de paiement'**
  String get choisirMoyenPaiement;

  /// No description provided for @modifierLaDemande.
  ///
  /// In fr, this message translates to:
  /// **'Modifier la demande'**
  String get modifierLaDemande;

  /// No description provided for @sectionLieu.
  ///
  /// In fr, this message translates to:
  /// **'Lieu'**
  String get sectionLieu;

  /// No description provided for @axeFacultatif.
  ///
  /// In fr, this message translates to:
  /// **'AXE (FACULTATIF)'**
  String get axeFacultatif;

  /// No description provided for @champVilleDepart.
  ///
  /// In fr, this message translates to:
  /// **'VILLE DE DÉPART'**
  String get champVilleDepart;

  /// No description provided for @hintVilleDepart.
  ///
  /// In fr, this message translates to:
  /// **'Ex : Yaoundé'**
  String get hintVilleDepart;

  /// No description provided for @obligatoire.
  ///
  /// In fr, this message translates to:
  /// **'Obligatoire'**
  String get obligatoire;

  /// No description provided for @champVilleArrivee.
  ///
  /// In fr, this message translates to:
  /// **'VILLE D\'ARRIVÉE'**
  String get champVilleArrivee;

  /// No description provided for @hintVilleArrivee.
  ///
  /// In fr, this message translates to:
  /// **'Ex : Douala'**
  String get hintVilleArrivee;

  /// No description provided for @sectionMarchandise.
  ///
  /// In fr, this message translates to:
  /// **'Marchandise'**
  String get sectionMarchandise;

  /// No description provided for @typeMarchandise.
  ///
  /// In fr, this message translates to:
  /// **'TYPE DE MARCHANDISE'**
  String get typeMarchandise;

  /// No description provided for @catalogueIndisponible.
  ///
  /// In fr, this message translates to:
  /// **'Catalogue indisponible pour le moment'**
  String get catalogueIndisponible;

  /// No description provided for @reessayer.
  ///
  /// In fr, this message translates to:
  /// **'Réessayer'**
  String get reessayer;

  /// No description provided for @selectionnerLeType.
  ///
  /// In fr, this message translates to:
  /// **'Sélectionner le type'**
  String get selectionnerLeType;

  /// No description provided for @choisirTypeMarchandise.
  ///
  /// In fr, this message translates to:
  /// **'Choisissez un type de marchandise'**
  String get choisirTypeMarchandise;

  /// No description provided for @quantiteNombreUnites.
  ///
  /// In fr, this message translates to:
  /// **'QUANTITÉ (NOMBRE D\'UNITÉS)'**
  String get quantiteNombreUnites;

  /// No description provided for @quantiteNombreDe.
  ///
  /// In fr, this message translates to:
  /// **'QUANTITÉ (NOMBRE DE \"{nom}\")'**
  String quantiteNombreDe(String nom);

  /// No description provided for @unites.
  ///
  /// In fr, this message translates to:
  /// **'unité(s)'**
  String get unites;

  /// No description provided for @nombreInvalide.
  ///
  /// In fr, this message translates to:
  /// **'Nombre invalide'**
  String get nombreInvalide;

  /// No description provided for @poidsTotalLabel.
  ///
  /// In fr, this message translates to:
  /// **'Poids total'**
  String get poidsTotalLabel;

  /// No description provided for @volumeTotalLabel.
  ///
  /// In fr, this message translates to:
  /// **'Volume total'**
  String get volumeTotalLabel;

  /// No description provided for @vehiculeAdapte.
  ///
  /// In fr, this message translates to:
  /// **'Véhicule adapté : {vehicule}'**
  String vehiculeAdapte(String vehicule);

  /// No description provided for @vehiculeCamionnette.
  ///
  /// In fr, this message translates to:
  /// **'Camionnette (jusqu\'à 500 kg)'**
  String get vehiculeCamionnette;

  /// No description provided for @vehiculeFourgon.
  ///
  /// In fr, this message translates to:
  /// **'Fourgon (jusqu\'à 1,5 t)'**
  String get vehiculeFourgon;

  /// No description provided for @vehiculeCamionLeger.
  ///
  /// In fr, this message translates to:
  /// **'Camion léger 3T5 (jusqu\'à 3,5 t)'**
  String get vehiculeCamionLeger;

  /// No description provided for @vehiculeCamionMoyen.
  ///
  /// In fr, this message translates to:
  /// **'Camion moyen 8T (jusqu\'à 8 t)'**
  String get vehiculeCamionMoyen;

  /// No description provided for @vehiculeCamionLourd.
  ///
  /// In fr, this message translates to:
  /// **'Camion lourd 20T (jusqu\'à 20 t)'**
  String get vehiculeCamionLourd;

  /// No description provided for @vehiculeSemiRemorque.
  ///
  /// In fr, this message translates to:
  /// **'Semi-remorque (plus de 20 t)'**
  String get vehiculeSemiRemorque;

  /// No description provided for @naturesParticulieres.
  ///
  /// In fr, this message translates to:
  /// **'NATURE PARTICULIÈRE'**
  String get naturesParticulieres;

  /// No description provided for @sectionModalites.
  ///
  /// In fr, this message translates to:
  /// **'Modalités'**
  String get sectionModalites;

  /// No description provided for @disponibiliteLabel.
  ///
  /// In fr, this message translates to:
  /// **'DISPONIBILITÉ'**
  String get disponibiliteLabel;

  /// No description provided for @dansUnePlage.
  ///
  /// In fr, this message translates to:
  /// **'Dans une plage'**
  String get dansUnePlage;

  /// No description provided for @modeCollecteLabel.
  ///
  /// In fr, this message translates to:
  /// **'MODE DE COLLECTE'**
  String get modeCollecteLabel;

  /// No description provided for @aDomicile.
  ///
  /// In fr, this message translates to:
  /// **'À domicile'**
  String get aDomicile;

  /// No description provided for @sectionDestinataire.
  ///
  /// In fr, this message translates to:
  /// **'Destinataire'**
  String get sectionDestinataire;

  /// No description provided for @hintDestinataireNom.
  ///
  /// In fr, this message translates to:
  /// **'Ex : Paul Nkomo'**
  String get hintDestinataireNom;

  /// No description provided for @telephoneRenseigner.
  ///
  /// In fr, this message translates to:
  /// **'Renseignez le téléphone du destinataire'**
  String get telephoneRenseigner;

  /// No description provided for @prixEstimationMessage.
  ///
  /// In fr, this message translates to:
  /// **'Le prix affiché sera une estimation — le prix ferme viendra avec la proposition acceptée.'**
  String get prixEstimationMessage;

  /// No description provided for @nouvelleDemandeAnnulationEchouee.
  ///
  /// In fr, this message translates to:
  /// **'Nouvelle demande publiée, mais l\'ancienne n\'a pas pu être annulée : {erreur}'**
  String nouvelleDemandeAnnulationEchouee(String erreur);

  /// No description provided for @demandeModifiee.
  ///
  /// In fr, this message translates to:
  /// **'Demande modifiée.'**
  String get demandeModifiee;

  /// No description provided for @publierLaDemande.
  ///
  /// In fr, this message translates to:
  /// **'Publier la demande'**
  String get publierLaDemande;

  /// No description provided for @enregistrerModifications.
  ///
  /// In fr, this message translates to:
  /// **'Enregistrer les modifications'**
  String get enregistrerModifications;

  /// No description provided for @kyc1Info.
  ///
  /// In fr, this message translates to:
  /// **'Ce niveau (KYC 1) vous permet de publier des demandes de transport.'**
  String get kyc1Info;

  /// No description provided for @etapeIdentite.
  ///
  /// In fr, this message translates to:
  /// **'Identité'**
  String get etapeIdentite;

  /// No description provided for @etapePieceIdentite.
  ///
  /// In fr, this message translates to:
  /// **'Pièce d\'identité'**
  String get etapePieceIdentite;

  /// No description provided for @pieceInvalideMessage.
  ///
  /// In fr, this message translates to:
  /// **'Cette photo ne ressemble pas à une pièce d\'identité — cadrez bien le document (texte lisible) et réessayez.'**
  String get pieceInvalideMessage;

  /// No description provided for @phoneLabel.
  ///
  /// In fr, this message translates to:
  /// **'Téléphone'**
  String get phoneLabel;

  /// No description provided for @typeDeCompte.
  ///
  /// In fr, this message translates to:
  /// **'Type de compte'**
  String get typeDeCompte;

  /// No description provided for @pieceDeposeeLabel.
  ///
  /// In fr, this message translates to:
  /// **'Pièce déposée'**
  String get pieceDeposeeLabel;

  /// No description provided for @modifierNumeroTelephone.
  ///
  /// In fr, this message translates to:
  /// **'Modifier le numéro de téléphone'**
  String get modifierNumeroTelephone;

  /// No description provided for @numeroActuel.
  ///
  /// In fr, this message translates to:
  /// **'Numéro actuel : {numero}'**
  String numeroActuel(String numero);

  /// No description provided for @confirmezNumeroActuel.
  ///
  /// In fr, this message translates to:
  /// **'Confirmez votre numéro actuel'**
  String get confirmezNumeroActuel;

  /// No description provided for @nouveauNumero.
  ///
  /// In fr, this message translates to:
  /// **'Nouveau numéro'**
  String get nouveauNumero;

  /// No description provided for @champObligatoire.
  ///
  /// In fr, this message translates to:
  /// **'Champ obligatoire'**
  String get champObligatoire;

  /// No description provided for @valider.
  ///
  /// In fr, this message translates to:
  /// **'Valider'**
  String get valider;

  /// No description provided for @numeroTelephoneMisAJour.
  ///
  /// In fr, this message translates to:
  /// **'Numéro de téléphone mis à jour.'**
  String get numeroTelephoneMisAJour;

  /// No description provided for @echecModification.
  ///
  /// In fr, this message translates to:
  /// **'Échec de la modification.'**
  String get echecModification;

  /// No description provided for @numeroRccmOptionnel.
  ///
  /// In fr, this message translates to:
  /// **'NUMÉRO RCCM (optionnel)'**
  String get numeroRccmOptionnel;

  /// No description provided for @hintRccm.
  ///
  /// In fr, this message translates to:
  /// **'Ex : RC/DLA/2024/B/1234'**
  String get hintRccm;

  /// No description provided for @verificationEnCours.
  ///
  /// In fr, this message translates to:
  /// **'Vérification…'**
  String get verificationEnCours;

  /// No description provided for @envoiEnCours.
  ///
  /// In fr, this message translates to:
  /// **'Envoi…'**
  String get envoiEnCours;

  /// No description provided for @prendreEnPhotoPiece.
  ///
  /// In fr, this message translates to:
  /// **'Prendre en photo ma pièce d\'identité'**
  String get prendreEnPhotoPiece;

  /// No description provided for @enregistrer.
  ///
  /// In fr, this message translates to:
  /// **'Enregistrer'**
  String get enregistrer;
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
