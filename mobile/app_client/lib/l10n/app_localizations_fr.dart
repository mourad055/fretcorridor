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

  @override
  String get aucuneNotification => 'Aucune notification pour le moment.';

  @override
  String get paiement => 'Paiement';

  @override
  String get intentionReglementInfo =>
      'Ce choix indique votre intention de règlement — l\'encaissement effectif se fait séparément via le prestataire agréé.';

  @override
  String get choisirMoyenReglement => 'Choisissez votre moyen de règlement';

  @override
  String get confirmer => 'Confirmer';

  @override
  String moyenReglementRetenu(String moyen) {
    return 'Moyen de règlement retenu : $moyen.';
  }

  @override
  String get especes => 'Espèces';

  @override
  String get promoTitre1 => 'Envoyez partout au Cameroun';

  @override
  String get promoDesc1 =>
      'Des centaines de transporteurs vérifiés sur le corridor CEMAC';

  @override
  String get promoTitre2 => 'Suivi en temps réel';

  @override
  String get promoDesc2 =>
      'Suivez votre marchandise du départ jusqu\'à la livraison';

  @override
  String get promoTitre3 => 'Transporteurs vérifiés';

  @override
  String get promoDesc3 =>
      'Chaque chauffeur passe par une vérification d\'identité';

  @override
  String get signalerLitige => 'Signaler un litige';

  @override
  String missionConcernee(String id) {
    return 'Mission concernée : $id';
  }

  @override
  String get motif => 'MOTIF';

  @override
  String get description => 'DESCRIPTION';

  @override
  String get hintDescriptionLitige => 'Décrivez le problème rencontré';

  @override
  String get envoyerSignalement => 'Envoyer le signalement';

  @override
  String get litigeConfirmation =>
      'Votre signalement a été transmis. Le Bureau reviendra vers vous.';

  @override
  String get mesDemandes => 'Mes demandes';

  @override
  String get nouvelleDemande => 'Nouvelle demande';

  @override
  String get aucuneDemande => 'Aucune demande publiée pour le moment.';

  @override
  String get annulerCetteDemandeTitre => 'Annuler cette demande ?';

  @override
  String get annulerCetteDemandeContenu => 'Cette action est définitive.';

  @override
  String get retour => 'Retour';

  @override
  String get annulerLaDemande => 'Annuler la demande';

  @override
  String get demandeAnnulee => 'Demande annulée.';

  @override
  String get prochaineAEtreServie => 'Prochaine à être servie';

  @override
  String positionDansLaFile(int rang) {
    return 'Position $rang dans la file';
  }

  @override
  String get fragile => 'Fragile';

  @override
  String get perissable => 'Périssable';

  @override
  String get dangereuse => 'Dangereuse';

  @override
  String get grandeValeur => 'Grande valeur';

  @override
  String get voirLesPropositions => 'Voir les propositions';

  @override
  String get suivi => 'Suivi';

  @override
  String get modifier => 'Modifier';

  @override
  String get annuler => 'Annuler';

  @override
  String get desQuePossible => 'Dès que possible';

  @override
  String get datePrecise => 'Date précise';

  @override
  String get plageHoraire => 'Plage horaire';

  @override
  String get collecteADomicile => 'Collecte à domicile';

  @override
  String get pointRelais => 'Point relais';

  @override
  String destinataireLabel(String nom, String telephone) {
    return 'Destinataire : $nom · $telephone';
  }

  @override
  String publieeLe(String date) {
    return 'Publiée le $date';
  }

  @override
  String get propositions => 'Propositions';

  @override
  String get aucuneProposition => 'Aucune proposition pour le moment';

  @override
  String get aucunePropositionDescription =>
      'Votre demande est en attente d\'appariement avec un transporteur disponible sur cet axe.';

  @override
  String get prixEnCoursCalcul => 'Prix en cours de calcul';

  @override
  String get statutAcceptee => 'Acceptée';

  @override
  String get statutExpiree => 'Expirée';

  @override
  String get statutEnAttente => 'En attente';

  @override
  String get accepterCetteProposition => 'Accepter cette proposition';

  @override
  String get propositionAcceptee => 'Proposition acceptée ✅';

  @override
  String get dateSpecifique => 'À date précise';

  @override
  String get surPlageHoraire => 'Sur une plage horaire';

  @override
  String get collecteEnPointRelais => 'Collecte en point relais';

  @override
  String get suiviTitre => 'Suivi de ma livraison';

  @override
  String get suiviPasDisponible => 'Suivi pas encore disponible';

  @override
  String get suiviPasDisponibleDescription =>
      'Le suivi démarre dès qu\'un transporteur prend en charge votre demande.';

  @override
  String get envoiGroupe =>
      'Envoi groupé : votre colis fait partie d\'une tournée consolidée avec d\'autres envois.';

  @override
  String get vehiculeEnMouvement => 'Véhicule en mouvement';

  @override
  String get positionMiseAJourInstant => 'Position mise à jour à l\'instant';

  @override
  String positionMiseAJourDepuis(int minutes) {
    return 'Position mise à jour il y a $minutes min';
  }

  @override
  String get positionGpsIndisponible => 'Position GPS pas encore disponible.';

  @override
  String get etapesTitre => 'Étapes';

  @override
  String get aucuneEtape => 'Aucune étape enregistrée pour le moment.';

  @override
  String get choisirMoyenPaiement => 'Choisir le moyen de paiement';

  @override
  String get modifierLaDemande => 'Modifier la demande';

  @override
  String get sectionLieu => 'Lieu';

  @override
  String get axeFacultatif => 'AXE (FACULTATIF)';

  @override
  String get champVilleDepart => 'VILLE DE DÉPART';

  @override
  String get hintVilleDepart => 'Ex : Yaoundé';

  @override
  String get obligatoire => 'Obligatoire';

  @override
  String get champVilleArrivee => 'VILLE D\'ARRIVÉE';

  @override
  String get hintVilleArrivee => 'Ex : Douala';

  @override
  String get sectionMarchandise => 'Marchandise';

  @override
  String get typeMarchandise => 'TYPE DE MARCHANDISE';

  @override
  String get catalogueIndisponible => 'Catalogue indisponible pour le moment';

  @override
  String get reessayer => 'Réessayer';

  @override
  String get selectionnerLeType => 'Sélectionner le type';

  @override
  String get choisirTypeMarchandise => 'Choisissez un type de marchandise';

  @override
  String get quantiteNombreUnites => 'QUANTITÉ (NOMBRE D\'UNITÉS)';

  @override
  String quantiteNombreDe(String nom) {
    return 'QUANTITÉ (NOMBRE DE \"$nom\")';
  }

  @override
  String get unites => 'unité(s)';

  @override
  String get nombreInvalide => 'Nombre invalide';

  @override
  String get poidsTotalLabel => 'Poids total';

  @override
  String get volumeTotalLabel => 'Volume total';

  @override
  String vehiculeAdapte(String vehicule) {
    return 'Véhicule adapté : $vehicule';
  }

  @override
  String get vehiculeCamionnette => 'Camionnette (jusqu\'à 500 kg)';

  @override
  String get vehiculeFourgon => 'Fourgon (jusqu\'à 1,5 t)';

  @override
  String get vehiculeCamionLeger => 'Camion léger 3T5 (jusqu\'à 3,5 t)';

  @override
  String get vehiculeCamionMoyen => 'Camion moyen 8T (jusqu\'à 8 t)';

  @override
  String get vehiculeCamionLourd => 'Camion lourd 20T (jusqu\'à 20 t)';

  @override
  String get vehiculeSemiRemorque => 'Semi-remorque (plus de 20 t)';

  @override
  String get naturesParticulieres => 'NATURE PARTICULIÈRE';

  @override
  String get sectionModalites => 'Modalités';

  @override
  String get disponibiliteLabel => 'DISPONIBILITÉ';

  @override
  String get dansUnePlage => 'Dans une plage';

  @override
  String get modeCollecteLabel => 'MODE DE COLLECTE';

  @override
  String get aDomicile => 'À domicile';

  @override
  String get sectionDestinataire => 'Destinataire';

  @override
  String get hintDestinataireNom => 'Ex : Paul Nkomo';

  @override
  String get telephoneRenseigner => 'Renseignez le téléphone du destinataire';

  @override
  String get prixEstimationMessage =>
      'Le prix affiché sera une estimation — le prix ferme viendra avec la proposition acceptée.';

  @override
  String nouvelleDemandeAnnulationEchouee(String erreur) {
    return 'Nouvelle demande publiée, mais l\'ancienne n\'a pas pu être annulée : $erreur';
  }

  @override
  String get demandeModifiee => 'Demande modifiée.';

  @override
  String get publierLaDemande => 'Publier la demande';

  @override
  String get enregistrerModifications => 'Enregistrer les modifications';

  @override
  String get kyc1Info =>
      'Ce niveau (KYC 1) vous permet de publier des demandes de transport.';

  @override
  String get etapeIdentite => 'Identité';

  @override
  String get etapePieceIdentite => 'Pièce d\'identité';

  @override
  String get pieceInvalideMessage =>
      'Cette photo ne ressemble pas à une pièce d\'identité — cadrez bien le document (texte lisible) et réessayez.';

  @override
  String get phoneLabel => 'Téléphone';

  @override
  String get typeDeCompte => 'Type de compte';

  @override
  String get pieceDeposeeLabel => 'Pièce déposée';

  @override
  String get modifierNumeroTelephone => 'Modifier le numéro de téléphone';

  @override
  String numeroActuel(String numero) {
    return 'Numéro actuel : $numero';
  }

  @override
  String get confirmezNumeroActuel => 'Confirmez votre numéro actuel';

  @override
  String get nouveauNumero => 'Nouveau numéro';

  @override
  String get champObligatoire => 'Champ obligatoire';

  @override
  String get valider => 'Valider';

  @override
  String get numeroTelephoneMisAJour => 'Numéro de téléphone mis à jour.';

  @override
  String get echecModification => 'Échec de la modification.';

  @override
  String get numeroRccmOptionnel => 'NUMÉRO RCCM (optionnel)';

  @override
  String get hintRccm => 'Ex : RC/DLA/2024/B/1234';

  @override
  String get verificationEnCours => 'Vérification…';

  @override
  String get envoiEnCours => 'Envoi…';

  @override
  String get prendreEnPhotoPiece => 'Prendre en photo ma pièce d\'identité';

  @override
  String get enregistrer => 'Enregistrer';
}
