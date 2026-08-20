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

/**
 * Clés de traduction (`| translate` côté template) pour les enums backend
 * affichés bruts (Sprint 15, converties en clés i18n au Sprint 23 — voir
 * public/assets/i18n/{fr,en}.json, namespace `enum.*`). Le texte de repli
 * (valeur brute) sert pour une valeur backend future non encore traduite.
 */
export function libelleDossierStatut(statut?: string): string {
  switch (statut) {
    case 'OUVERT':
      return 'enum.dossierStatut.OUVERT';
    case 'EN_COURS':
      return 'enum.dossierStatut.EN_COURS';
    case 'ESCALADE':
      return 'enum.dossierStatut.ESCALADE';
    case 'CLOS':
      return 'enum.dossierStatut.CLOS';
    default:
      return statut ?? '';
  }
}

export function libelleKycStatut(statut?: string): string {
  switch (statut) {
    case 'EN_ATTENTE':
      return 'enum.kycStatut.EN_ATTENTE';
    case 'VALIDE':
      return 'enum.kycStatut.VALIDE';
    case 'REJETE':
      return 'enum.kycStatut.REJETE';
    default:
      return statut ?? '';
  }
}

export function libelleAxeVisibilite(active: boolean): string {
  return active ? 'enum.axeVisibilite.ACTIVE' : 'enum.axeVisibilite.INACTIVE';
}

export function libelleAxeMatching(actif: boolean): string {
  return actif ? 'enum.axeMatching.ACTIVE' : 'enum.axeMatching.INACTIVE';
}

export function libelleAxePaiement(actif: boolean): string {
  return actif ? 'enum.axePaiement.ACTIVE' : 'enum.axePaiement.INACTIVE';
}

export function libelleEcritureStatut(statut?: string): string {
  switch (statut) {
    case 'VALIDE':
      return 'enum.ecritureStatut.VALIDE';
    case 'SUSPENDU':
      return 'enum.ecritureStatut.SUSPENDU';
    default:
      return statut ?? '';
  }
}

export function libelleMissionStatut(statut?: string): string {
  switch (statut) {
    case 'CONFIRMEE':
      return 'enum.missionStatut.CONFIRMEE';
    case 'EN_COURS':
      return 'enum.missionStatut.EN_COURS';
    case 'CLOTUREE':
      return 'enum.missionStatut.CLOTUREE';
    default:
      return statut ?? '';
  }
}

export function libelleModeCollecte(mode?: string): string {
  switch (mode) {
    case 'PORTE_A_PORTE':
      return 'enum.modeCollecte.PORTE_A_PORTE';
    case 'POINT_DEPOT':
      return 'enum.modeCollecte.POINT_DEPOT';
    default:
      return mode ?? '';
  }
}

export function libelleCapaciteEtat(etat?: string): string {
  switch (etat) {
    case 'PUBLIEE':
      return 'enum.capaciteEtat.PUBLIEE';
    case 'APPARIEE':
      return 'enum.capaciteEtat.APPARIEE';
    case 'EXPIREE':
      return 'enum.capaciteEtat.EXPIREE';
    default:
      return etat ?? '';
  }
}

export function libelleTypeActeur(type?: string): string {
  switch (type) {
    case 'CHAUFFEUR':
      return 'enum.typeActeur.CHAUFFEUR';
    case 'TRANSPORTEUR_PERSONNE_MORALE':
      return 'enum.typeActeur.TRANSPORTEUR_PERSONNE_MORALE';
    default:
      return humanize(type);
  }
}

export function libelleTypeDossier(type?: string): string {
  switch (type) {
    case 'MODERATION':
      return 'enum.typeDossier.MODERATION';
    case 'INCIDENT':
      return 'enum.typeDossier.INCIDENT';
    case 'LITIGE':
      return 'enum.typeDossier.LITIGE';
    default:
      return type ?? '';
  }
}

export function libellePrioriteDossier(priorite?: string): string {
  switch (priorite) {
    case 'BASSE':
      return 'enum.prioriteDossier.BASSE';
    case 'NORMALE':
      return 'enum.prioriteDossier.NORMALE';
    case 'HAUTE':
      return 'enum.prioriteDossier.HAUTE';
    default:
      return priorite ?? '';
  }
}

export function libelleEtapeEtat(etat?: string): string {
  switch (etat) {
    case 'A_VENIR':
      return 'enum.etapeEtat.A_VENIR';
    case 'EN_COURS':
      return 'enum.etapeEtat.EN_COURS';
    case 'TERMINEE':
      return 'enum.etapeEtat.TERMINEE';
    default:
      return etat ?? '';
  }
}

/** Action du journal d'audit : vocabulaire ouvert (pas un enum fermé) — reste humanisée plutôt que traduite. */
export function libelleJournalAction(action?: string): string {
  return humanize(action);
}

/** EF-PAY-06/07 : moyen de paiement choisi à l'encaissement, `null` sur un reversement. */
export function libelleModePaiement(mode?: string | null): string {
  switch (mode) {
    case 'MONNAIE_ELECTRONIQUE':
      return 'Monnaie électronique';
    case 'VIREMENT':
      return 'Virement';
    case 'TERME_CONTRACTUEL':
      return 'Terme contractuel';
    case 'ESPECES':
      return 'Espèces';
    default:
      return mode ?? '—';
  }
}
