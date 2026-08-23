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
  String get welcomeTitre => 'Roulez,\nlivrez, gagnez';

  @override
  String get welcomeSousTitre =>
      'Déclarez vos capacités et acceptez des missions\nsur le corridor CEMAC.';

  @override
  String get creerUnCompte => 'Créer un compte';

  @override
  String get dejaUnCompte => 'Vous avez déjà un compte ? ';

  @override
  String get connexion => 'Connexion';

  @override
  String get seConnecter => 'Se connecter';

  @override
  String get champTelephone => 'TÉLÉPHONE';

  @override
  String get champCode => 'CODE';

  @override
  String get codeObligatoire => 'Code obligatoire';

  @override
  String get codeFormatInvalide => '4 à 6 chiffres';

  @override
  String get compteClientMessage =>
      'Ce compte est un compte client — utilisez l\'app FretCorridor Client.';

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
  String get aideFaqTitre => 'QUESTIONS FRÉQUENTES';

  @override
  String get aideQ1 => 'Comment déclarer une capacité de transport ?';

  @override
  String get aideR1 =>
      'Depuis l\'accueil, appuyez sur \"Déclarer une capacité\", renseignez l\'axe, le poids disponible et la date, puis validez.';

  @override
  String get aideQ2 => 'Pourquoi mon profil doit être complété ?';

  @override
  String get aideR2 =>
      'La complétion du profil (identité + pièce d\'identité) est obligatoire avant de pouvoir déclarer une capacité ou accepter une mission — c\'est une exigence de vérification (KYC).';

  @override
  String get aideQ3 => 'Comment sont attribuées les missions ?';

  @override
  String get aideR3 =>
      'Une mission vous est proposée lorsque votre capacité correspond à une demande de chargeur sur le même axe. Vous pouvez l\'accepter ou la refuser.';

  @override
  String get aideQ4 => 'Comment suivre mes missions ?';

  @override
  String get aideR4 =>
      'Rendez-vous sur \"Mes missions\" depuis l\'accueil pour voir le statut de chaque mission acceptée.';

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
  String get notificationsTooltip => 'Notifications';

  @override
  String get profilCompleteMessageChauffeur =>
      'Profil complété ✅ — vous pouvez déclarer une capacité.';

  @override
  String bonjour(String nom) {
    return 'Bonjour, $nom';
  }

  @override
  String get profilACompleter => 'Profil à compléter';

  @override
  String get profilACompleterDescriptionChauffeur =>
      'Complétez votre profil pour déclarer une capacité ou accepter une mission.';

  @override
  String get completer => 'Compléter';

  @override
  String get mesMissions => 'Mes missions';

  @override
  String get mesMissionsDescription => 'Missions en cours et historique';

  @override
  String get declarerCapacite => 'Déclarer une capacité';

  @override
  String get declarerCapaciteDescription =>
      'Proposer un trajet et de la place disponible';

  @override
  String get maFlotte => 'Ma flotte';

  @override
  String get maFlotteDescription => 'Gérer mes véhicules';

  @override
  String get soldeEtGains => 'Solde et gains';

  @override
  String get soldeEtGainsDescription => 'Consulter mes paiements';

  @override
  String get axes => 'Axes';

  @override
  String get axesDescription => 'Corridors disponibles';

  @override
  String get monProfil => 'Mon profil';

  @override
  String get monProfilDescriptionChauffeur => 'Identité et niveau KYC';

  @override
  String get enroler => 'Enrôler';

  @override
  String get roleChauffeur => 'Chauffeur';

  @override
  String get roleTransporteur => 'Transporteur';

  @override
  String get roleAgent => 'Agent';

  @override
  String get inscriptionSousTitre =>
      'Vous compléterez votre profil juste après.';

  @override
  String get jeSuis => 'JE SUIS';

  @override
  String get typeChauffeur => 'Chauffeur';

  @override
  String get typeTransporteur => 'Transporteur';

  @override
  String get typeChauffeurProprietaire => 'Les deux';

  @override
  String get labelRaisonSociale => 'RAISON SOCIALE';

  @override
  String get hintRaisonSocialeChauffeur => 'Ex : Transport Fotso SARL';

  @override
  String get raisonSocialeObligatoire => 'Raison sociale obligatoire';

  @override
  String get labelPrenom => 'PRÉNOM';

  @override
  String get hintPrenomChauffeur => 'Ex : Paul';

  @override
  String get prenomObligatoire => 'Prénom obligatoire';

  @override
  String get labelNom => 'NOM';

  @override
  String get hintNomChauffeur => 'Ex : Kamga';

  @override
  String get nomObligatoire => 'Nom obligatoire';

  @override
  String get labelCodeInscription => 'CODE (4 à 6 chiffres)';

  @override
  String get creerMonCompte => 'Créer mon compte';

  @override
  String get choisirUnBureau => 'Choisir un bureau';

  @override
  String get compteMultiBureau =>
      'Votre compte est rattaché à plusieurs bureaux. Choisissez celui avec lequel travailler :';

  @override
  String get bureauPrincipal => '(principal)';
}
