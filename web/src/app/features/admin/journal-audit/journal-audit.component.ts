import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { JournalAuditService } from './journal-audit.service';
import { EntreeJournalAudit } from '../../../shared/models/journal-audit.models';
import { libelleJournalAction } from '../../../shared/components/status-badge/status-badge.component';

/** FE-ADM-05 (Sprint 10) : journal d'audit consultable et exportable, en lecture seule, append-only. */
@Component({
  selector: 'app-journal-audit',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './journal-audit.component.html',
})
export class JournalAuditComponent implements OnInit {
  readonly entrees = signal<EntreeJournalAudit[]>([]);
  readonly loading = signal(true);
  readonly errorMessage = signal<string | null>(null);
  readonly libelleJournalAction = libelleJournalAction;

  constructor(private readonly journalAuditService: JournalAuditService) {}

  ngOnInit(): void {
    this.journalAuditService.lister().subscribe({
      next: (entrees) => {
        this.entrees.set(entrees);
        this.loading.set(false);
      },
      error: () => {
        this.errorMessage.set('Impossible de charger le journal d\'audit.');
        this.loading.set(false);
      },
    });
  }

  exporter(): void {
    this.journalAuditService.exporterCsv().subscribe({
      next: (csv) => {
        const blob = new Blob([csv], { type: 'text/csv' });
        const url = URL.createObjectURL(blob);
        const lien = document.createElement('a');
        lien.href = url;
        lien.download = 'journal-audit.csv';
        lien.click();
        URL.revokeObjectURL(url);
      },
      error: () => this.errorMessage.set('Impossible d\'exporter le journal d\'audit.'),
    });
  }
}
