import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ConfigurationsService } from './configurations.service';
import { Configuration } from '../../../shared/models/configuration.models';

/** FE-ADM-03 (Sprint 10) : chaque redéfinition crée une nouvelle version, jamais une modification en place. */
@Component({
  selector: 'app-configurations',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './configurations.component.html',
})
export class ConfigurationsComponent {
  readonly cle = signal('seuil-agregation-bur');
  readonly nouvelleValeur = signal('');
  readonly historique = signal<Configuration[]>([]);
  readonly errorMessage = signal<string | null>(null);

  constructor(private readonly configurationsService: ConfigurationsService) {}

  consulter(): void {
    this.errorMessage.set(null);
    this.configurationsService.historique(this.cle()).subscribe({
      next: (historique) => this.historique.set(historique),
      error: () => this.errorMessage.set('Impossible de charger l\'historique de cette configuration.'),
    });
  }

  definir(): void {
    this.configurationsService.definir(this.cle(), this.nouvelleValeur()).subscribe({
      next: () => {
        this.nouvelleValeur.set('');
        this.consulter();
      },
      error: () => this.errorMessage.set('Impossible de définir cette configuration.'),
    });
  }
}
