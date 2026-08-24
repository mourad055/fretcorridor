import { Injectable } from '@angular/core';

/**
 * Confirmation avant action irréversible (audit UX 2026-08-23,
 * docs/AUDIT_ROADMAP_Backoffice_Web_2026-08-23.md §3.4) : un simple clic
 * déclenchait jusqu'ici l'appel HTTP sans aucune étape de confirmation
 * (nouvelle version de config, décision de dossier, création de tenant,
 * désactivation de compte). `window.confirm` plutôt qu'une modale maison :
 * accessible nativement (focus, Échap, lecteurs d'écran) sans code
 * supplémentaire — cohérent avec la sobriété du design system pour un
 * garde-fou de sécurité, pas un moment de marque. Enveloppé dans un service
 * injectable pour rester testable (mock en `TestBed`, jamais un spy global).
 */
@Injectable({ providedIn: 'root' })
export class ConfirmationService {
  confirmer(message: string): boolean {
    return window.confirm(message);
  }
}
