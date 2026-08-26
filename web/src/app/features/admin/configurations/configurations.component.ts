import { Component, OnInit, computed, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { PageShellComponent } from '../../../shared/components/page-shell/page-shell.component';
import { FormsModule } from '@angular/forms';
import { ConfigurationsService } from './configurations.service';
import { Configuration } from '../../../shared/models/configuration.models';
import { ConfirmationService } from '../../../shared/services/confirmation.service';
import { PaginationComponent } from '../../../shared/components/pagination/pagination.component';
import { paginer } from '../../../shared/utils/pagination';

const TAILLE_PAGE = 20;

/**
 * FE-ADM-03/EF-ADM-06 : chaque redéfinition crée une nouvelle version, jamais
 * une modification en place. Le catalogue liste les paramètres déjà
 * configurés (plus besoin de connaître le nom de la clé à l'avance) ;
 * sélectionner une clé, ou en saisir une nouvelle, ouvre son historique.
 */
@Component({
  selector: 'app-configurations',
  standalone: true,
  imports: [CommonModule, PageShellComponent, FormsModule, PaginationComponent],
  templateUrl: './configurations.component.html',
})
export class ConfigurationsComponent implements OnInit {
  readonly catalogue = signal<Configuration[]>([]);
  readonly catalogueLoading = signal(false);

  readonly cle = signal('');
  readonly nouvelleValeur = signal('');
  readonly historique = signal<Configuration[]>([]);
  readonly loading = signal(false);
  readonly consulte = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly pageCatalogue = signal(1);
  readonly pageHistorique = signal(1);
  readonly taillePage = TAILLE_PAGE;
  readonly catalogueAffiche = computed(() => paginer(this.catalogue(), this.pageCatalogue(), this.taillePage));
  readonly historiqueAffiche = computed(() => paginer(this.historique(), this.pageHistorique(), this.taillePage));

  constructor(
    private readonly configurationsService: ConfigurationsService,
    private readonly confirmationService: ConfirmationService
  ) {}

  ngOnInit(): void {
    this.chargerCatalogue();
  }

  private chargerCatalogue(): void {
    this.catalogueLoading.set(true);
    this.pageCatalogue.set(1);
    this.configurationsService.catalogue().subscribe({
      next: (catalogue) => {
        this.catalogue.set(catalogue);
        this.catalogueLoading.set(false);
      },
      error: () => {
        this.errorMessage.set('Impossible de charger le catalogue des configurations.');
        this.catalogueLoading.set(false);
      },
    });
  }

  selectionner(cle: string): void {
    this.cle.set(cle);
    this.consulter();
  }

  consulter(): void {
    if (!this.cle().trim()) {
      return;
    }
    this.loading.set(true);
    this.errorMessage.set(null);
    this.pageHistorique.set(1);
    this.configurationsService.historique(this.cle()).subscribe({
      next: (historique) => {
        this.historique.set(historique);
        this.loading.set(false);
        this.consulte.set(true);
      },
      error: () => {
        this.errorMessage.set('Impossible de charger l\'historique de cette configuration.');
        this.loading.set(false);
      },
    });
  }

  definir(): void {
    const confirme = this.confirmationService.confirmer(
      `Définir une nouvelle version de « ${this.cle()} » avec la valeur « ${this.nouvelleValeur()} » ? Cette action crée une nouvelle version, jamais réversible en place.`
    );
    if (!confirme) {
      return;
    }
    this.loading.set(true);
    this.errorMessage.set(null);
    this.configurationsService.definir(this.cle(), this.nouvelleValeur()).subscribe({
      next: () => {
        this.nouvelleValeur.set('');
        this.consulter();
        this.chargerCatalogue();
      },
      error: () => {
        this.errorMessage.set('Impossible de définir cette configuration.');
        this.loading.set(false);
      },
    });
  }
}
