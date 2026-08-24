import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { nombreDePages } from '../../utils/pagination';

/** Contrôles réutilisables (Précédent/Suivant + "Page X / Y") — voir `paginer()`. */
@Component({
  selector: 'app-pagination',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './pagination.component.html',
  styleUrl: './pagination.component.css',
})
export class PaginationComponent {
  @Input({ required: true }) page = 1;
  @Input({ required: true }) total = 0;
  @Input() taillePage = 20;
  @Output() readonly pageChange = new EventEmitter<number>();

  get totalPages(): number {
    return nombreDePages(this.total, this.taillePage);
  }

  precedent(): void {
    if (this.page > 1) {
      this.pageChange.emit(this.page - 1);
    }
  }

  suivant(): void {
    if (this.page < this.totalPages) {
      this.pageChange.emit(this.page + 1);
    }
  }
}
