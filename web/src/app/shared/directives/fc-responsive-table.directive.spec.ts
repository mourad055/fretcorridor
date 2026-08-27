import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Component } from '@angular/core';
import { FcResponsiveTableDirective } from './fc-responsive-table.directive';

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
});
