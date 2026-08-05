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

export function axeStatusVariant(etat?: string): StatusBadgeVariant {
  switch (etat) {
    case 'PAIEMENT':
      return 'success';
    case 'MATCHING':
      return 'primary';
    case 'VISIBILITE':
      return 'neutral';
    default:
      return 'neutral';
  }
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
