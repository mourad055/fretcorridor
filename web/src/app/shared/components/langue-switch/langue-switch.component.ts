import { Component, inject } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';
import { LangueService } from '../../../core/i18n/langue.service';
import { Langue } from '../../../core/i18n/i18n.constants';

/** Sélecteur FR/EN réutilisable (Sprint 21 : Login, Sprint 22 : Shell). */
@Component({
  selector: 'app-langue-switch',
  standalone: true,
  imports: [TranslatePipe],
  templateUrl: './langue-switch.component.html',
  styleUrl: './langue-switch.component.css',
})
export class LangueSwitchComponent {
  private readonly langueService = inject(LangueService);
  readonly langueActuelle = this.langueService.langueActuelle;

  changerLangue(langue: Langue): void {
    this.langueService.definir(langue);
  }
}
