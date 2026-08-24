import { Provider } from '@angular/core';
import { provideTranslateService, TranslateLoader } from '@ngx-translate/core';
import { Observable, of } from 'rxjs';

/**
 * Traductions FR utilisées en test (Sprint 21) — copie volontairement minimale
 * de public/assets/i18n/fr.json, tenue à jour manuellement au fil des sprints
 * i18n plutôt qu'importée (pas de resolveJsonModule dans ce projet).
 */
const TRADUCTIONS_TEST_FR = {
  login: {
    title: 'Connexion',
    hint: 'Le corridor de fret, connecté.',
    phoneLabel: 'Numéro de téléphone',
    codeLabel: 'Code PIN',
    submit: 'Se connecter',
    submitting: 'Connexion…',
    error: 'Numéro de téléphone ou code invalide.',
    demo: {
      label: 'Comptes de démonstration',
      bureau: 'Bureau de fret',
      transporteur: 'Transporteur',
      admin: 'Administration',
      bureauTchad: 'Bureau de fret (Tchad)',
    },
    showcase: { title: 'FretCorridor' },
    tenant: {
      title: 'Choisir un bureau',
      hint: 'Ce compte est rattaché à plusieurs bureaux. Choisissez celui sous lequel vous voulez opérer.',
      origine: 'Bureau principal',
      error: 'Impossible de sélectionner ce bureau. Réessayez.',
    },
  },
  langue: {
    fr: 'Français',
    en: 'Anglais',
    selecteurAria: "Changer la langue de l'interface",
  },
  shell: {
    role: {
      bureau: 'Bureau de fret',
      transporteur: 'Transporteur',
      admin: 'Administration',
    },
    logout: 'Déconnexion',
    navAriaLabel: 'Navigation principale',
  },
  nav: {
    groupe: {
      supervision: 'Supervision',
      exploitation: 'Exploitation',
      conformite: 'Conformité',
      finance: 'Finance',
      configuration: 'Configuration',
    },
    bureau: {
      axes: 'Axes',
      missions: 'Missions appariées',
      positions: 'Suivi temps réel',
      chronologie: 'Chronologie',
      rapportFinancier: 'Rapport financier',
      notifications: 'Notifications',
      observatoire: 'Observatoire',
      affiliations: 'Inviter un transporteur',
    },
    transporteur: {
      capacites: 'Capacités',
      missions: 'Mes missions',
      paiement: 'Paiement',
    },
    admin: {
      kyc: 'KYC',
      rapportFinancier: 'Rapport financier',
      dossiers: 'Dossiers',
      configuration: 'Configuration',
      tenants: 'Tenants',
      journalAudit: "Journal d'audit",
      comptes: 'Comptes',
      recherche: 'Recherche',
      notifications: 'Notifications',
    },
  },
  enum: {
    dossierStatut: { OUVERT: 'Ouvert', EN_COURS: 'En cours', ESCALADE: 'Escaladé', CLOS: 'Clos' },
    kycStatut: { EN_ATTENTE: 'En attente', VALIDE: 'Validé', REJETE: 'Rejeté' },
    axeVisibilite: { ACTIVE: 'Visible', INACTIVE: 'Masqué' },
    axeMatching: { ACTIVE: 'Matching actif', INACTIVE: 'Matching inactif' },
    axePaiement: { ACTIVE: 'Paiement actif', INACTIVE: 'Paiement inactif' },
    ecritureStatut: { VALIDE: 'Validée', SUSPENDU: 'Suspendue' },
    missionStatut: { CONFIRMEE: 'Confirmée', EN_COURS: 'En cours', CLOTUREE: 'Clôturée' },
    modeCollecte: { PORTE_A_PORTE: 'Porte à porte', POINT_DEPOT: 'Point de dépôt' },
    capaciteEtat: { PUBLIEE: 'Publiée', APPARIEE: 'Appariée', EXPIREE: 'Expirée' },
    typeActeur: { CHAUFFEUR: 'Chauffeur', TRANSPORTEUR_PERSONNE_MORALE: 'Transporteur (personne morale)' },
    typeDossier: { MODERATION: 'Modération', INCIDENT: 'Incident', LITIGE: 'Litige' },
    prioriteDossier: { BASSE: 'Basse', NORMALE: 'Normale', HAUTE: 'Haute' },
    etapeEtat: { A_VENIR: 'À venir', EN_COURS: 'En cours', TERMINEE: 'Terminée' },
    etapeType: { ENLEVEMENT: 'Enlèvement', LIVRAISON: 'Livraison' },
  },
  affiliations: {
    titre: 'Inviter un transporteur',
    sousTitre:
      'Rattachez un chauffeur ou transporteur déjà inscrit à votre tenant. L\'invitation vaut validation immédiate — aucune confirmation de sa part n\'est nécessaire.',
    telephone: 'Numéro de téléphone',
    placeholder: '+237690000001',
    soumettre: 'Inviter',
    confirmation: 'Rattacher {{telephone}} à votre tenant ? L\'invitation vaut validation immédiate.',
    succes: '{{telephone}} a été rattaché à votre tenant.',
    erreur: {
      acteurIntrouvable: 'Aucun compte ne correspond à ce numéro de téléphone.',
      roleNonAffiliable:
        'Ce numéro ne correspond pas à un chauffeur ou un transporteur — seuls ces rôles peuvent être rattachés.',
      indisponible: "Service d'affiliation indisponible, réessayez plus tard.",
      generique: "Impossible d'inviter ce numéro.",
    },
  },
};

class TranslateFakeLoader implements TranslateLoader {
  getTranslation(): Observable<Record<string, unknown>> {
    return of(TRADUCTIONS_TEST_FR);
  }
}

/** À ajouter aux `providers` du TestBed pour tout composant utilisant `TranslatePipe`/`TranslateService`. */
export function provideTranslateServiceForTests(): Provider[] {
  return provideTranslateService({ lang: 'fr', fallbackLang: 'fr', loader: TranslateFakeLoader });
}
