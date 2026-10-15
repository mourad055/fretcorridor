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
  String get notifTitrePropositionRecue => 'Nouvelle proposition';

  @override
  String get notifTitreStatutMission => 'Mise à jour de mission';

  @override
  String get notifTitreInfoGenerale => 'Information';

  @override
  String get notifTitrePropositionRetour => 'Proposition de retour';

  @override
  String get notifTitreAlerteEcart => 'Alerte';

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

  @override
  String get statutEnAttente => 'En attente';

  @override
  String get statutPriseEnCharge => 'Prise en charge';

  @override
  String get statutEnTransit => 'En transit';

  @override
  String get statutLivree => 'Livrée';

  @override
  String get statutAnnulee => 'Annulée';

  @override
  String get etapeLivraison => 'Livraison';

  @override
  String get incidentLabel => 'Incident';

  @override
  String get disponibiliteDesQuePossible => 'Dès que possible';

  @override
  String get disponibiliteDatePrecise => 'À date précise';

  @override
  String get disponibilitePlage => 'Sur une plage horaire';

  @override
  String get collecteDomicile => 'Collecte à domicile';

  @override
  String get collectePointRelais => 'Collecte en point relais';

  @override
  String get aucuneMissionPourLeMoment => 'Aucune mission pour le moment.';

  @override
  String get idDeLaMission => 'ID de la mission :';

  @override
  String destinataireSansTel(String nom) {
    return 'Destinataire : $nom';
  }

  @override
  String destinataireAvecTel(String nom, String telephone) {
    return 'Destinataire : $nom · $telephone';
  }

  @override
  String poidsTotalLabel(String poids) {
    return 'Poids total : $poids kg';
  }

  @override
  String typeLabel(String type) {
    return 'Type : $type';
  }

  @override
  String publieeLe(String date) {
    return 'Publiée le $date';
  }

  @override
  String get valeurLabel => 'Valeur : ';

  @override
  String get grandeValeur => 'Grande valeur';

  @override
  String get faitPartieTourneeGroupee => 'Fait partie d\'une tournée groupée';

  @override
  String get chronologie => 'Chronologie';

  @override
  String get aucuneEtapePourLeMoment => 'Aucune étape pour le moment.';

  @override
  String statutAvecValeur(String statut) {
    return 'Statut : $statut';
  }

  @override
  String get suiviGpsActif => 'Suivi GPS actif';

  @override
  String get voirLePlanDeChargement => 'Voir le plan de chargement';

  @override
  String get signalerUnIncident => 'Signaler un incident';

  @override
  String get confirmerLaLivraison => 'Confirmer la livraison';

  @override
  String get destinataire => 'Destinataire';

  @override
  String get grilleDecisionNote =>
      'Grille de décision et recours traités par le Bureau — pas encore automatisés côté app.';

  @override
  String get categorieLabel => 'CATÉGORIE';

  @override
  String get descriptionLabel => 'DESCRIPTION';

  @override
  String get detaillezOptionnel => 'Détaillez ce qui s\'est passé (optionnel)';

  @override
  String get ajouterPhotoOptionnel => 'Ajouter une photo (optionnel)';

  @override
  String get photoJointe => 'Photo jointe';

  @override
  String get envoyerLeSignalement => 'Envoyer le signalement';

  @override
  String get preuveDePriseEnCharge => 'Preuve de prise en charge';

  @override
  String get preuveDeLivraison => 'Preuve de livraison';

  @override
  String get photoEtSignatureObligatoires =>
      'Une photo et une signature sont obligatoires.';

  @override
  String get photosAuMoinsUn => 'PHOTOS (au moins 1)';

  @override
  String get signatureDuDestinataire => 'SIGNATURE DU DESTINATAIRE';

  @override
  String get effacer => 'Effacer';

  @override
  String get valider => 'Valider';

  @override
  String get noteRg070 =>
      'Photo(s) de la marchandise + signature du destinataire — obligatoire (RG-070).';

  @override
  String get categorieRetard => 'Retard';

  @override
  String get categorieMarchandiseEndommagee => 'Marchandise endommagée';

  @override
  String get categorieAccident => 'Accident';

  @override
  String get categoriePanneVehicule => 'Panne véhicule';

  @override
  String get categorieAutre => 'Autre';

  @override
  String get aucuneNotification => 'Aucune notification.';

  @override
  String get refuser => 'Refuser';

  @override
  String get accepter => 'Accepter';

  @override
  String get promoTitre1 => 'Trouvez des missions rapidement';

  @override
  String get promoDesc1 =>
      'Déclarez votre capacité, recevez des propositions sur vos axes';

  @override
  String get promoTitre2 => 'Paiement sécurisé';

  @override
  String get promoDesc2 =>
      'Suivez vos gains et vos paiements directement dans l\'app';

  @override
  String get promoTitre3 => 'Suivi GPS en temps réel';

  @override
  String get promoDesc3 => 'Partagez votre position pendant vos missions';

  @override
  String get tourneeGroupee => 'Tournée groupée';

  @override
  String envoiGroupeEtapes(int n) {
    return 'Envoi groupé — $n étapes';
  }

  @override
  String get aucuneEtapeTermineePourLeMoment =>
      'Aucune étape terminée pour le moment.';

  @override
  String get enlevementLabel => 'Enlèvement';

  @override
  String demandeIdLabel(String id) {
    return 'Demande $id';
  }

  @override
  String get confirmerEnlevement => 'Confirmer l\'enlèvement';

  @override
  String get toutesEtapesTerminees =>
      'Toutes les étapes de la tournée sont terminées.';

  @override
  String get historique => 'Historique';

  @override
  String get aucuneEcriturePourLeMoment => 'Aucune écriture pour le moment.';

  @override
  String get soldeLabel => 'SOLDE';

  @override
  String get natureEncaissement => 'Encaissement';

  @override
  String get natureReversement => 'Reversement';

  @override
  String get natureCommission => 'Commission';

  @override
  String get natureSequestre => 'Séquestre';

  @override
  String get modeMonnaieElectronique => 'Monnaie électronique';

  @override
  String get modeVirement => 'Virement';

  @override
  String get modeTermeContractuel => 'Terme contractuel';

  @override
  String get modeEspeces => 'Espèces';

  @override
  String regleVia(String mode) {
    return 'Réglé via $mode';
  }

  @override
  String get badgeVisible => 'Visible';

  @override
  String get badgeMatching => 'Matching';

  @override
  String get badgePaiement => 'Paiement';

  @override
  String get ajouter => 'Ajouter';

  @override
  String get aucunVehiculeEnregistre =>
      'Aucun véhicule enregistré.\nAppuyez sur \"Ajouter\" pour en déclarer un.';

  @override
  String get nouveauVehicule => 'Nouveau véhicule';

  @override
  String get typeDeVehicule => 'Type de véhicule';

  @override
  String get champObligatoire => 'Champ obligatoire';

  @override
  String get immatriculationFacultatif => 'Immatriculation (facultatif)';

  @override
  String get poidsMaxTonnesFacultatif => 'Poids max (tonnes, facultatif)';

  @override
  String get nombreEssieuxFacultatif => 'Nombre d\'essieux (facultatif)';

  @override
  String get matieresDangereuses => 'Matières dangereuses';

  @override
  String get enregistrer => 'Enregistrer';

  @override
  String get kycPhotoNonReconnue =>
      'Cette photo ne ressemble pas à une pièce d\'identité — cadrez bien le document (texte lisible) et réessayez.';

  @override
  String get profilCompleteEmoji => 'Profil complété ✅';

  @override
  String get telephoneLabel => 'Téléphone';

  @override
  String get typeDeCompte => 'Type de compte';

  @override
  String get entreprise => 'Entreprise';

  @override
  String get particulier => 'Particulier';

  @override
  String get pieceDeposeeLabel => 'Pièce déposée';

  @override
  String get modifier => 'Modifier';

  @override
  String get modifierNumeroTelephone => 'Modifier le numéro de téléphone';

  @override
  String numeroActuelLabel(String telephone) {
    return 'Numéro actuel : $telephone';
  }

  @override
  String get confirmezNumeroActuel => 'Confirmez votre numéro actuel';

  @override
  String get nouveauNumero => 'Nouveau numéro';

  @override
  String get annuler => 'Annuler';

  @override
  String get numeroTelephoneMisAJour => 'Numéro de téléphone mis à jour.';

  @override
  String get echecModification => 'Échec de la modification.';

  @override
  String get completezVotreProfil => 'Complétez votre profil';

  @override
  String get identitePieceCondition =>
      'Identité déclarée et pièce déposée — condition pour publier ou accepter une mission (RG-011).';

  @override
  String get identite => 'Identité';

  @override
  String get pieceIdentite => 'Pièce d\'identité';

  @override
  String get verificationEnCours => 'Vérification…';

  @override
  String get envoiEnCours => 'Envoi…';

  @override
  String get prendrePhotoIdentite => 'Prendre en photo ma pièce d\'identité';

  @override
  String get numeroRegistreCommerceFacultatif =>
      'N° REGISTRE DE COMMERCE (facultatif)';

  @override
  String get enrolerUnChauffeur => 'Enrôler un chauffeur';

  @override
  String get synchroniserFileOffline => 'Synchroniser la file offline';

  @override
  String enrolementsEnAttenteSync(int n) {
    String _temp0 = intl.Intl.pluralLogic(
      n,
      locale: localeName,
      other: '$n enrôlements en attente de synchronisation (hors ligne).',
      one: '$n enrôlement en attente de synchronisation (hors ligne).',
    );
    return '$_temp0';
  }

  @override
  String get nouvelEnrolementTitre => 'Nouvel enrôlement';

  @override
  String get codeActivationSmsMessage =>
      'Le code d\'activation part par SMS directement au téléphone de la personne — jamais au vôtre.';

  @override
  String get typeSectionLabel => 'TYPE';

  @override
  String get typeChauffeurProprietaireEnrolement => 'Chauffeur-propriétaire';

  @override
  String get telephoneDeLaPersonneLabel => 'TÉLÉPHONE DE LA PERSONNE';

  @override
  String get telephoneObligatoire => 'Téléphone obligatoire';

  @override
  String get formatInvalide => 'Format invalide';

  @override
  String get envoyerLeCode => 'Envoyer le code';

  @override
  String get activerLeCompte => 'Activer le compte';

  @override
  String get codeEtPinParLaPersonneMessage =>
      'À faire saisir par la personne elle-même : le code reçu par SMS, puis un code PIN de son choix.';

  @override
  String get codeRecuParSmsLabel => 'CODE REÇU PAR SMS';

  @override
  String get codeSixChiffres => 'Code à 6 chiffres';

  @override
  String get nouveauCodePinLabel => 'NOUVEAU CODE PIN (4-6 chiffres)';

  @override
  String get pinObligatoire => 'PIN obligatoire';

  @override
  String get planDeChargementTitre => 'Plan de chargement';

  @override
  String get planChargementNonDisponibleMessage =>
      'Plan de chargement pas encore disponible pour cette tournée — le Moteur ne l\'a pas encore calculé (ou cette tournée est trop simple pour en nécessiter un).';

  @override
  String etapePlanLabel(int rang, String type, String demandeId) {
    return 'Étape $rang — $type — Demande $demandeId';
  }

  @override
  String get repartitionApproximativeMessage =>
      'Répartition approximative (poids total réparti uniformément entre essieux)';

  @override
  String get choisissezUnAxe => 'Choisissez un axe.';

  @override
  String get choisissezUnVehicule => 'Choisissez un véhicule.';

  @override
  String get choisissezUneDateDepart => 'Choisissez une date de départ.';

  @override
  String get capaciteModifiee => 'Capacité modifiée.';

  @override
  String get nouvelleCapaciteAncienneEchouee =>
      'Nouvelle capacité déclarée, mais l\'ancienne n\'a pas pu être supprimée.';

  @override
  String capacitePubliee(String kg) {
    return 'Capacité publiée — $kg kg taxables.';
  }

  @override
  String get capaciteEnregistree => 'Capacité enregistrée.';

  @override
  String get modifierLaCapacite => 'Modifier la capacité';

  @override
  String get mesCapacites => 'Mes capacités';

  @override
  String get axeLabel => 'AXE';

  @override
  String get chargementEnCours => 'Chargement…';

  @override
  String get choisirUnAxe => 'Choisir un axe';

  @override
  String get vehiculeLabel => 'VÉHICULE';

  @override
  String get gererMaFlotte => 'Gérer ma flotte';

  @override
  String get aucunVehiculeAjoutezEnUn =>
      'Aucun véhicule — ajoutez-en un via \"Gérer ma flotte\".';

  @override
  String get choisirUnVehicule => 'Choisir un véhicule';

  @override
  String get poidsDisponibleKgLabel => 'POIDS DISPONIBLE (KG)';

  @override
  String get nombreInvalide => 'Nombre invalide';

  @override
  String get departLabel => 'DÉPART';

  @override
  String get choisirDateEtHeure => 'Choisir une date et une heure';

  @override
  String get declarerLaCapacite => 'Déclarer la capacité';

  @override
  String get enregistrerLesModifications => 'Enregistrer les modifications';

  @override
  String get supprimerCetteCapacite => 'Supprimer cette capacité ?';

  @override
  String get actionDefinitive => 'Cette action est définitive.';

  @override
  String get supprimer => 'Supprimer';

  @override
  String get capaciteSupprimee => 'Capacité supprimée.';

  @override
  String get suppressionImpossible => 'Suppression impossible pour le moment.';

  @override
  String get jourLun => 'lun.';

  @override
  String get jourMar => 'mar.';

  @override
  String get jourMer => 'mer.';

  @override
  String get jourJeu => 'jeu.';

  @override
  String get jourVen => 'ven.';

  @override
  String get jourSam => 'sam.';

  @override
  String get jourDim => 'dim.';

  @override
  String get aucuneCapaciteDeclareePourLeMoment =>
      'Aucune capacité déclarée pour le moment';

  @override
  String get expiree => 'Expirée';

  @override
  String get publiee => 'Publiée';

  @override
  String kgDisponibles(String kg) {
    return '$kg kg disponibles';
  }

  @override
  String departLabelValeur(String date) {
    return 'Départ : $date';
  }

  @override
  String declareeLe(String date) {
    return 'Déclarée le $date';
  }

  @override
  String get mesPropositionsMission => 'Mes propositions';

  @override
  String get mesPropositionsMissionDescription =>
      'Missions à accepter ou refuser';

  @override
  String get remunerationLabel => 'Rémunération';

  @override
  String expireDans(String secondes) {
    return 'Expire dans $secondes s';
  }

  @override
  String get propositionExpiree => 'Expirée';

  @override
  String get aucunePropositionMission =>
      'Aucune proposition en attente pour le moment.';

  @override
  String get poidsLabel => 'Poids';

  @override
  String get distanceLabel => 'Distance';

  @override
  String get dureeLabel => 'Durée';

  @override
  String get modeCollecteLabel => 'Mode de collecte';

  @override
  String get disponibiliteLabel => 'Disponibilité';

  @override
  String get marchandiseLabel => 'Marchandise';

  @override
  String get destinataireLabel => 'Destinataire';

  @override
  String get motifRefusTitre => 'Pourquoi refusez-vous cette mission ?';

  @override
  String get motifTropLoin => 'Trop loin de ma position';

  @override
  String get motifIndisponible => 'Indisponible sur ce créneau';

  @override
  String get motifRemunerationInsuffisante => 'Rémunération insuffisante';

  @override
  String get motifVehiculeInadapte => 'Véhicule non adapté';

  @override
  String get motifAutre => 'Autre raison';

  @override
  String get confirmerRefus => 'Confirmer le refus';

  @override
  String get missionAcceptee => 'Mission acceptée !';

  @override
  String get missionRefusee => 'Mission refusée.';

  @override
  String get propositionIndisponible =>
      'Cette proposition n\'est plus disponible (déjà répondue ou expirée).';
}
