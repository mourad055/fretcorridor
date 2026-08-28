import { Injectable, signal } from '@angular/core';

export interface ConfirmationDemande {
  message: string;
  title: string;
  confirmLabel: string;
  cancelLabel: string;
  danger: boolean;
}

export interface ConfirmationOptions {
  title?: string;
  confirmLabel?: string;
  cancelLabel?: string;
  danger?: boolean;
}

/**
 * Confirmation avant action irréversible — modale centrée `fc-modal` (portail web).
 * Injectable pour rester testable via mock `confirmer: jest.fn().mockResolvedValue(...)`.
 */
@Injectable({ providedIn: 'root' })
export class ConfirmationService {
  readonly demande = signal<ConfirmationDemande | null>(null);

  private resolve: ((value: boolean) => void) | null = null;

  confirmer(message: string, options: ConfirmationOptions = {}): Promise<boolean> {
    if (this.resolve) {
      this.repondre(false);
    }
    return new Promise((resolve) => {
      this.resolve = resolve;
      this.demande.set({
        message,
        title: options.title ?? 'Confirmation',
        confirmLabel: options.confirmLabel ?? 'Confirmer',
        cancelLabel: options.cancelLabel ?? 'Annuler',
        danger: options.danger ?? false,
      });
    });
  }

  repondre(oui: boolean): void {
    this.demande.set(null);
    const resolve = this.resolve;
    this.resolve = null;
    resolve?.(oui);
  }
}
