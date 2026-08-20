import { Injectable, inject, signal } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { CLE_STOCKAGE_LANGUE, LANGUE_PAR_DEFAUT, LANGUES_DISPONIBLES, Langue } from './i18n.constants';

/**
 * Sprint 21 : porte la langue active côté FR/EN, persistée en localStorage.
 * ngx-translate fait le changement runtime (pas de rebuild par locale) —
 * choix cohérent avec les contraintes de build de cette machine (ADR-0009).
 */
@Injectable({ providedIn: 'root' })
export class LangueService {
  private readonly translate = inject(TranslateService);
  readonly langueActuelle = signal<Langue>(LANGUE_PAR_DEFAUT);

  initialiser(): void {
    this.definir(this.lireLangueStockee() ?? LANGUE_PAR_DEFAUT);
  }

  definir(langue: Langue): void {
    this.translate.use(langue);
    this.langueActuelle.set(langue);
    localStorage.setItem(CLE_STOCKAGE_LANGUE, langue);
  }

  private lireLangueStockee(): Langue | null {
    const valeur = localStorage.getItem(CLE_STOCKAGE_LANGUE);
    return (LANGUES_DISPONIBLES as string[]).includes(valeur ?? '') ? (valeur as Langue) : null;
  }
}
