import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Component } from '@angular/core';
import { FcResponsiveTableDirective } from './fc-responsive-table.directive';
import { PageShellComponent } from '../components/page-shell/page-shell.component';

@Component({
  standalone: true,
  imports: [FcResponsiveTableDirective],
  template: `
    <main class="fc-page">
      <table class="fc-table">
        <thead>
          <tr>
            <th>Origine</th>
            <th>Destination</th>
          </tr>
        </thead>
        <tbody>
          <tr>
            <td>Douala</td>
            <td>Yaoundé</td>
          </tr>
        </tbody>
      </table>
    </main>
  `,
})
class HostComponent {}

describe('FcResponsiveTableDirective', () => {
  it('injecte data-label et la classe responsive', () => {
    const fixture = TestBed.createComponent(HostComponent);
    fixture.detectChanges();

    const table = fixture.nativeElement.querySelector('table.fc-table') as HTMLTableElement;
    expect(table.classList.contains('fc-table--responsive')).toBe(true);

    const cells = table.querySelectorAll('tbody td');
    expect(cells[0].getAttribute('data-label')).toBe('Origine');
    expect(cells[1].getAttribute('data-label')).toBe('Destination');
  });

  it('fonctionne via app-page-shell (contenu projeté)', () => {
    @Component({
      standalone: true,
      imports: [PageShellComponent],
      template: `
        <app-page-shell>
          <table class="fc-table">
            <thead>
              <tr>
                <th>Acteur</th>
                <th>Téléphone</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <td>Alice</td>
                <td>+237600000001</td>
              </tr>
            </tbody>
          </table>
        </app-page-shell>
      `,
    })
    class PageShellHostComponent {}

    const fixture = TestBed.createComponent(PageShellHostComponent);
    fixture.detectChanges();

    const table = fixture.nativeElement.querySelector('table.fc-table') as HTMLTableElement;
    expect(table.classList.contains('fc-table--responsive')).toBe(true);
    expect(table.querySelector('tbody td')?.getAttribute('data-label')).toBe('Acteur');
  });
});
