// ignore: unused_import
import 'package:intl/intl.dart' as intl;
import 'app_localizations.dart';

// ignore_for_file: type=lint

/// The translations for French (`fr`).
class AppLocalizationsFr extends AppLocalizations {
  AppLocalizationsFr([String locale = 'fr']) : super(locale);

  @override
  String get langueTitre => 'Langue';

  @override
  String get langueFrancais => 'Français';

  @override
  String get langueAnglais => 'English';

  @override
  String get welcomeTitre => 'Vos envois,\nsans complications';

  @override
  String get welcomeSousTitre =>
      'Publiez votre demande et connectez-vous aux\ntransporteurs du réseau CEMAC.';

  @override
  String get commencer => 'Commencer';

  @override
  String get dejaUnCompte => 'Vous avez déjà un compte ? ';

  @override
  String get connexion => 'Connexion';

  @override
  String get seConnecter => 'Se connecter';

  @override
  String get loginSousTitre => 'Envoyez vos marchandises, simplement';

  @override
  String get champTelephone => 'TÉLÉPHONE';

  @override
  String get champCodePin => 'CODE PIN';

  @override
  String get pinObligatoire => 'PIN obligatoire';

  @override
  String get pinFormatInvalide => '4 à 6 chiffres';

  @override
  String get pasEncoreDeCompte => 'Pas encore de compte ? Créer un compte';

  @override
  String get menuTitre => 'Menu';

  @override
  String get menuEspaceUtilisateur => 'Espace utilisateur';

  @override
  String get profil => 'Profil';

  @override
  String get notifications => 'Notifications';

  @override
  String langueMenuItem(String langue) {
    return 'Langue ($langue)';
  }

  @override
  String get centreAide => 'Centre d\'aide';

  @override
  String get politiqueConfidentialite => 'Politique & confidentialité';

  @override
  String get conditionsUtilisation => 'Conditions d\'utilisation';

  @override
  String get parametres => 'Paramètres';

  @override
  String get seDeconnecter => 'Se déconnecter';

  @override
  String get sectionNotifications => 'NOTIFICATIONS';

  @override
  String get notificationsPush => 'Notifications push';

  @override
  String get notificationsPushDescription =>
      'Propositions reçues, statut des demandes';

  @override
  String get sectionGeneral => 'GÉNÉRAL';

  @override
  String get langue => 'Langue';

  @override
  String get versionApp => 'FretCorridor · Version 1.0.0 (bêta)';

  @override
  String get chargeurDefaut => 'Chargeur';

  @override
  String get profilCompleteMessage =>
      'Profil complété ✅ — vous pouvez publier une demande.';

  @override
  String bonjour(String nom) {
    return 'Bonjour, $nom';
  }

  @override
  String get marketplaceCemac => 'Marketplace CEMAC';

  @override
  String get profilACompleter => 'Profil à compléter';

  @override
  String get profilACompleterDescription =>
      'Complétez votre profil pour publier une demande.';

  @override
  String get completer => 'Compléter';

  @override
  String get envoyerMarchandise => 'Envoyer une marchandise';

  @override
  String get envoyerMarchandiseDescription =>
      'Publiez une demande via le catalogue d\'emballages';

  @override
  String get monProfil => 'Mon profil';

  @override
  String get monProfilDescription => 'Informations personnelles et niveau KYC';

  @override
  String get aideFaqTitre => 'QUESTIONS FRÉQUENTES';

  @override
  String get aideQ1 => 'Comment publier une demande de transport ?';

  @override
  String get aideR1 =>
      'Depuis l\'accueil, appuyez sur \"Envoyer une marchandise\", renseignez le lieu, le type et la quantité de marchandise, puis les informations du destinataire.';

  @override
  String get aideQ2 => 'Pourquoi mon profil doit être complété ?';

  @override
  String get aideR2 =>
      'La complétion du profil (identité + pièce d\'identité) est obligatoire avant de pouvoir publier une demande — c\'est une exigence de vérification (KYC).';

  @override
  String get aideQ3 => 'Le prix affiché est-il définitif ?';

  @override
  String get aideR3 =>
      'Non, le prix affiché lors de la publication est une estimation. Le prix définitif est celui de la proposition que vous acceptez.';

  @override
  String get aideQ4 => 'Comment suivre mes demandes ?';

  @override
  String get aideR4 =>
      'Rendez-vous sur \"Mes demandes\" depuis l\'accueil pour voir le statut de chaque demande et ses propositions reçues.';

  @override
  String get aideContact =>
      'Besoin d\'aide supplémentaire ? Contactez l\'agence FretCorridor la plus proche.';

  @override
  String get cguTitre => 'Conditions d\'utilisation';

  @override
  String get cguObjetTitre => 'Objet';

  @override
  String get cguObjetTexte =>
      'FretCorridor met en relation des chargeurs et des transporteurs pour l\'organisation de transports de marchandises. La plateforme ne réalise pas elle-même les transports.';

  @override
  String get cguCompteTitre => 'Compte utilisateur';

  @override
  String get cguCompteTexte =>
      'Vous êtes responsable de l\'exactitude des informations fournies lors de l\'inscription et de la complétion de votre profil (KYC). Un compte peut être suspendu en cas d\'information frauduleuse.';

  @override
  String get cguDemandesTitre => 'Demandes et propositions';

  @override
  String get cguDemandesTexte =>
      'Toute demande publiée peut recevoir jusqu\'à 3 propositions classées. Le prix affiché avant acceptation est une estimation ; le prix définitif est fixé au moment de l\'acceptation d\'une proposition.';

  @override
  String get cguResponsabilitesTitre => 'Responsabilités';

  @override
  String get cguResponsabilitesTexte =>
      'Le chargeur est responsable de l\'exactitude des informations sur la marchandise (poids, nature, destinataire). Le transporteur est responsable de la bonne exécution de la mission acceptée.';

  @override
  String get cguModificationTitre => 'Modification';

  @override
  String get cguModificationTexte =>
      'Ces conditions peuvent évoluer ; les utilisateurs seront informés des changements significatifs via l\'application.';

  @override
  String get politiqueTitre => 'Politique de confidentialité';

  @override
  String get politiqueDonneesTitre => 'Données collectées';

  @override
  String get politiqueDonneesTexte =>
      'Nous collectons les informations nécessaires au fonctionnement de la plateforme : identité déclarée, pièce d\'identité, numéro de téléphone, et les informations liées à vos demandes et missions de transport.';

  @override
  String get politiqueUtilisationTitre => 'Utilisation des données';

  @override
  String get politiqueUtilisationTexte =>
      'Ces données servent à vérifier votre identité (KYC), à assurer la mise en relation entre chargeurs et transporteurs, et à assurer le suivi des missions. Elles ne sont jamais vendues à des tiers.';

  @override
  String get politiquePartageTitre => 'Partage des données';

  @override
  String get politiquePartageTexte =>
      'Vos informations de contact sont partagées uniquement avec la contrepartie d\'une mission acceptée (chargeur ↔ transporteur), dans la limite nécessaire à son exécution.';

  @override
  String get politiqueConservationTitre => 'Conservation';

  @override
  String get politiqueConservationTexte =>
      'Les données sont conservées pendant la durée de votre compte actif, puis archivées conformément aux obligations légales applicables.';

  @override
  String get politiqueDroitsTitre => 'Vos droits';

  @override
  String get politiqueDroitsTexte =>
      'Vous pouvez demander l\'accès, la correction ou la suppression de vos données personnelles via le centre d\'aide.';

  @override
  String get creerUnCompte => 'Créer un compte';

  @override
  String get particulier => 'Particulier';

  @override
  String get entreprise => 'Entreprise';

  @override
  String get labelRaisonSociale => 'RAISON SOCIALE';

  @override
  String get hintRaisonSociale => 'Ex : Cimencam SA';

  @override
  String get raisonSocialeObligatoire => 'Raison sociale obligatoire';

  @override
  String get labelPrenom => 'PRÉNOM';

  @override
  String get hintPrenom => 'Ex : Awa';

  @override
  String get prenomObligatoire => 'Prénom obligatoire';

  @override
  String get labelNom => 'NOM';

  @override
  String get hintNom => 'Ex : Mballa';

  @override
  String get nomObligatoire => 'Nom obligatoire';

  @override
  String get labelCodePinInscription => 'CODE PIN (4 à 6 chiffres)';

  @override
  String get hintCodePin => 'Ex : 1234';

  @override
  String get creerMonCompte => 'Créer mon compte';
}
