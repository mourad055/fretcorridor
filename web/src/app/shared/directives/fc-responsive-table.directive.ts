import { AfterViewInit, Directive, ElementRef, NgZone, OnDestroy, inject } from '@angular/core';

/**
 * Parcourt les tableaux `.fc-table` du contenu page et injecte `data-label`
 * depuis les en-têtes — permet le layout cartes mobile (styles.css).
 */
@Directive({
  selector: 'app-page-shell, main.fc-page, [fcResponsiveTable]',
  standalone: true,
})
export class FcResponsiveTableDirective implements AfterViewInit, OnDestroy {
  private readonly el = inject(ElementRef<HTMLElement>);
  private readonly zone = inject(NgZone);
  private observer?: MutationObserver;
  private scheduled = false;

  ngAfterViewInit(): void {
    this.applyLabels();
    this.zone.runOutsideAngular(() => {
      this.observer = new MutationObserver(() => this.scheduleApply());
      this.observer.observe(this.el.nativeElement, { childList: true, subtree: true });
    });
  }

  ngOnDestroy(): void {
    this.observer?.disconnect();
  }

  private scheduleApply(): void {
    if (this.scheduled) {
      return;
    }
    this.scheduled = true;
    requestAnimationFrame(() => {
      this.scheduled = false;
      this.applyLabels();
    });
  }

  private applyLabels(): void {
    this.el.nativeElement.querySelectorAll('table.fc-table').forEach((node: Element) => {
      this.labelTable(node as HTMLTableElement);
    });
  }

  private labelTable(table: HTMLTableElement): void {
    const headers = this.readHeaders(table);
    if (headers.length === 0) {
      return;
    }

    table.querySelectorAll('tbody tr').forEach((row) => {
      row.querySelectorAll('td').forEach((cell, index) => {
        const label = headers[index];
        if (label && cell.getAttribute('data-label') !== label) {
          cell.setAttribute('data-label', label);
        }
      });
    });

    if (!table.classList.contains('fc-table--responsive')) {
      table.classList.add('fc-table--responsive');
    }
  }

  private readHeaders(table: HTMLTableElement): string[] {
    return Array.from(table.querySelectorAll('thead th')).map((th) => {
      const srOnly = th.querySelector('.fc-sr-only');
      const fromSr = srOnly?.textContent?.trim();
      if (fromSr) {
        return fromSr;
      }
      return th.textContent?.trim() ?? '';
    });
  }
}
