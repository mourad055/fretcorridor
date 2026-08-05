import { TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { EcrituresTableComponent } from './ecritures-table.component';
import { Ecriture } from '../../models/ecriture.models';

const ECRITURES: Ecriture[] = [
  { id: 'e1', missionId: 'mission-1', typeCompte: 'COMPTE_TRANSPORTEUR', nature: 'REVERSEMENT', sens: 'DEBIT', montant: 90, creeLe: '2026-01-01T00:00:00Z', statut: 'VALIDE' },
];

describe('EcrituresTableComponent', () => {
  it('affiche une ligne par ecriture', async () => {
    await TestBed.configureTestingModule({ imports: [EcrituresTableComponent] }).compileComponents();
    const fixture = TestBed.createComponent(EcrituresTableComponent);
    fixture.componentRef.setInput('ecritures', ECRITURES);
    fixture.detectChanges();

    expect(fixture.debugElement.queryAll(By.css('tbody tr'))).toHaveLength(1);
  });

  it("affiche un message quand la liste est vide", async () => {
    await TestBed.configureTestingModule({ imports: [EcrituresTableComponent] }).compileComponents();
    const fixture = TestBed.createComponent(EcrituresTableComponent);
    fixture.componentRef.setInput('ecritures', []);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Aucune écriture');
  });
});
