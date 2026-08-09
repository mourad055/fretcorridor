import { Component, computed, input } from '@angular/core';

export type StatusBadgeVariant = 'success' | 'warning' | 'danger' | 'neutral' | 'primary';

/** Pastille de statut, reprise du design V3 (fc-badge). */
@Component({
  selector: 'app-status-badge',
  standalone: true,
  template: `<span class="fc-badge" [class]="variantClass()">{{ label() }}</span>`,
  styles: `
    .fc-badge {
      display: inline-flex;
      align-items: center;
      padding: 0.15rem 0.5rem;
      border-radius: var(--fc-radius-pill);
      font-size: 0.6875rem;
      font-weight: 700;
      letter-spacing: 0.02em;
      text-transform: uppercase;
      white-space: nowrap;
    }

    .fc-badge--success {
      background: var(--fc-success-soft);
      color: var(--fc-success);
    }

    .fc-badge--warning {
      background: color-mix(in srgb, var(--fc-warning) 12%, #fff);
      color: var(--fc-warning);
    }

    .fc-badge--danger {
      background: var(--fc-danger-soft);
      color: var(--fc-danger);
    }

    .fc-badge--primary {
      background: var(--fc-primary-soft);
      color: var(--fc-primary);
    }

    .fc-badge--neutral {
      background: var(--fc-bg);
      color: var(--fc-muted);
      border: 1px solid var(--fc-border);
    }
  `,
})
export class StatusBadgeComponent {
  readonly label = input.required<string>();
  readonly variant = input<StatusBadgeVariant>('neutral');

  readonly variantClass = computed(() => `fc-badge--${this.variant()}`);
}

export function dossierStatusVariant(statut?: string): StatusBadgeVariant {
  switch (statut) {
    case 'EN_COURS':
      return 'primary';
    case 'CLOS':
      return 'success';
    case 'ESCALADE':
      return 'danger';
    case 'OUVERT':
      return 'warning';
    default:
      return 'neutral';
  }
}

export function kycStatusVariant(statut?: string): StatusBadgeVariant {
  switch (statut) {
    case 'VALIDE':
      return 'success';
    case 'REJETE':
      return 'danger';
    case 'EN_ATTENTE':
      return 'warning';
    default:
      return 'neutral';
  }
}

export function axeVisibiliteVariant(active: boolean): StatusBadgeVariant {
  return active ? 'neutral' : 'danger';
}

export function axeMatchingVariant(actif: boolean): StatusBadgeVariant {
  return actif ? 'primary' : 'danger';
}

export function axePaiementVariant(actif: boolean): StatusBadgeVariant {
  return actif ? 'success' : 'danger';
}

export function ecritureStatusVariant(statut?: string): StatusBadgeVariant {
  switch (statut) {
    case 'VALIDE':
      return 'success';
    case 'SUSPENDU':
      return 'danger';
    default:
      return 'neutral';
  }
}

export function missionStatusVariant(statut?: string): StatusBadgeVariant {
  switch (statut) {
    case 'CLOTUREE':
      return 'success';
    case 'EN_COURS':
      return 'primary';
    case 'CONFIRMEE':
      return 'neutral';
    default:
      return 'neutral';
  }
}

export function capaciteStatusVariant(etat?: string): StatusBadgeVariant {
  switch (etat) {
    case 'APPARIEE':
      return 'success';
    case 'PUBLIEE':
      return 'primary';
    case 'EXPIREE':
      return 'danger';
    default:
      return 'neutral';
  }
}

function humanize(value?: string): string {
  if (!value) {
    return '';
  }
  const lower = value.toLowerCase().split('_').filter(Boolean).join(' ');
  return lower.charAt(0).toUpperCase() + lower.slice(1);
}

/** Libellés FR lisibles pour les enums backend affichés bruts (Sprint 15). */
export function libelleDossierStatut(statut?: string): string {
  switch (statut) {
    case 'OUVERT':
      return 'Ouvert';
    case 'EN_COURS':
      return 'En cours';
    case 'ESCALADE':
      return 'Escaladé';
    case 'CLOS':
      return 'Clos';
    default:
      return statut ?? '';
  }
}

export function libelleKycStatut(statut?: string): string {
  switch (statut) {
    case 'EN_ATTENTE':
      return 'En attente';
    case 'VALIDE':
      return 'Validé';
    case 'REJETE':
      return 'Rejeté';
    default:
      return statut ?? '';
  }
}

export function libelleAxeVisibilite(active: boolean): string {
  return active ? 'Visible' : 'Masqué';
}

export function libelleAxeMatching(actif: boolean): string {
  return actif ? 'Matching actif' : 'Matching inactif';
}

export function libelleAxePaiement(actif: boolean): string {
  return actif ? 'Paiement actif' : 'Paiement inactif';
}

export function libelleEcritureStatut(statut?: string): string {
  switch (statut) {
    case 'VALIDE':
      return 'Validée';
    case 'SUSPENDU':
      return 'Suspendue';
    default:
      return statut ?? '';
  }
}

export function libelleMissionStatut(statut?: string): string {
  switch (statut) {
    case 'CONFIRMEE':
      return 'Confirmée';
    case 'EN_COURS':
      return 'En cours';
    case 'CLOTUREE':
      return 'Clôturée';
    default:
      return statut ?? '';
  }
}

export function libelleModeCollecte(mode?: string): string {
  switch (mode) {
    case 'PORTE_A_PORTE':
      return 'Porte à porte';
    case 'POINT_DEPOT':
      return 'Point de dépôt';
    default:
      return mode ?? '';
  }
}

export function libelleCapaciteEtat(etat?: string): string {
  switch (etat) {
    case 'PUBLIEE':
      return 'Publiée';
    case 'APPARIEE':
      return 'Appariée';
    case 'EXPIREE':
      return 'Expirée';
    default:
      return etat ?? '';
  }
}

export function libelleTypeActeur(type?: string): string {
  switch (type) {
    case 'CHAUFFEUR':
      return 'Chauffeur';
    case 'TRANSPORTEUR_PERSONNE_MORALE':
      return 'Transporteur (personne morale)';
    default:
      return humanize(type);
  }
}

export function libelleTypeDossier(type?: string): string {
  switch (type) {
    case 'MODERATION':
      return 'Modération';
    case 'INCIDENT':
      return 'Incident';
    case 'LITIGE':
      return 'Litige';
    default:
      return type ?? '';
  }
}

export function libellePrioriteDossier(priorite?: string): string {
  switch (priorite) {
    case 'BASSE':
      return 'Basse';
    case 'NORMALE':
      return 'Normale';
    case 'HAUTE':
      return 'Haute';
    default:
      return priorite ?? '';
  }
}

export function libelleEtapeEtat(etat?: string): string {
  switch (etat) {
    case 'A_VENIR':
      return 'À venir';
    case 'EN_COURS':
      return 'En cours';
    case 'TERMINEE':
      return 'Terminée';
    default:
      return etat ?? '';
  }
}

export function libelleJournalAction(action?: string): string {
  return humanize(action);
}
