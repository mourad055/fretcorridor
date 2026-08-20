import { TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { EspecesTableComponent } from './especes-table.component';
import { DeclarationEspeces } from '../../models/declaration-especes.models';

const PAIEMENTS: DeclarationEspeces[] = [
  { id: 'd1', missionId: 'mission-1', montant: 150, declareeLe: '2026-01-01T00:00:00Z', protectionAssuree: false },
];

describe('EspecesTableComponent', () => {
  it('affiche une ligne par paiement en espèces avec un badge d\'absence de protection', async () => {
    await TestBed.configureTestingModule({ imports: [EspecesTableComponent] }).compileComponents();
    const fixture = TestBed.createComponent(EspecesTableComponent);
    fixture.componentRef.setInput('paiements', PAIEMENTS);
    fixture.detectChanges();

    expect(fixture.debugElement.queryAll(By.css('tbody tr'))).toHaveLength(1);
    expect(fixture.nativeElement.textContent).toContain('Aucune protection');
  });

  it('affiche un message quand la liste est vide', async () => {
    await TestBed.configureTestingModule({ imports: [EspecesTableComponent] }).compileComponents();
    const fixture = TestBed.createComponent(EspecesTableComponent);
    fixture.componentRef.setInput('paiements', []);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Aucun paiement en espèces');
  });
});
