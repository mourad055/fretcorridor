import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ConfirmationService } from '../../services/confirmation.service';

/** Modale de confirmation globale — remplace window.confirm sur tout le portail. */
@Component({
  selector: 'app-confirm-dialog',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './confirm-dialog.component.html',
})
export class ConfirmDialogComponent {
  readonly confirmationService = inject(ConfirmationService);

  annuler(): void {
    this.confirmationService.repondre(false);
  }

  confirmer(): void {
    this.confirmationService.repondre(true);
  }
}
