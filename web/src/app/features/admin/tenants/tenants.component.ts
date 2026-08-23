import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { PageShellComponent } from '../../../shared/components/page-shell/page-shell.component';
import { FormsModule } from '@angular/forms';
import { TenantsService } from './tenants.service';
import { Tenant } from '../../../shared/models/tenant.models';

/** FE-ADM-04 (Sprint 10) : gestion des tenants. */
@Component({
  selector: 'app-tenants',
  standalone: true,
  imports: [CommonModule, PageShellComponent, FormsModule],
  templateUrl: './tenants.component.html',
})
export class TenantsComponent implements OnInit {
  readonly tenants = signal<Tenant[]>([]);
  readonly loading = signal(true);
  readonly errorMessage = signal<string | null>(null);

  readonly nouvelId = signal('');
  readonly nouvelNom = signal('');
  readonly nouveauPays = signal('');

  constructor(private readonly tenantsService: TenantsService) {}

  ngOnInit(): void {
    this.charger();
  }

  charger(): void {
    this.loading.set(true);
    this.tenantsService.lister().subscribe({
      next: (tenants) => {
        this.tenants.set(tenants);
        this.loading.set(false);
      },
      error: () => {
        this.errorMessage.set('Impossible de charger les tenants.');
        this.loading.set(false);
      },
    });
  }

  creer(): void {
    this.tenantsService.creer(this.nouvelId(), this.nouvelNom(), this.nouveauPays()).subscribe({
      next: () => {
        this.nouvelId.set('');
        this.nouvelNom.set('');
        this.nouveauPays.set('');
        this.charger();
      },
      error: () => this.errorMessage.set('Impossible de créer ce tenant.'),
    });
  }
}
