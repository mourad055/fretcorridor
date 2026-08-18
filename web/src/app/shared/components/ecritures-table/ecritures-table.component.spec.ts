import { TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { EcrituresTableComponent } from './ecritures-table.component';
import { Ecriture } from '../../models/ecriture.models';

const ECRITURES: Ecriture[] = [
  { id: 'e1', missionId: 'mission-1', typeCompte: 'COMPTE_TRANSPORTEUR', nature: 'REVERSEMENT', sens: 'DEBIT', montant: 90, creeLe: '2026-01-01T00:00:00Z', statut: 'VALIDE', modePaiement: null, litigeActif: false },
];

describe('EcrituresTableComponent', () => {
  it('affiche une ligne par ecriture', async () => {
    await TestBed.configureTestingModule({ imports: [EcrituresTableComponent] }).compileComponents();
    const fixture = TestBed.createComponent(EcrituresTableComponent);
    fixture.componentRef.setInput('ecritures', ECRITURES);
    fixture.detectChanges();

    expect(fixture.debugElement.queryAll(By.css('tbody tr'))).toHaveLength(1);
  });

  it('affiche le mode de paiement quand connu et le masque sur un reversement', async () => {
    await TestBed.configureTestingModule({ imports: [EcrituresTableComponent] }).compileComponents();
    const fixture = TestBed.createComponent(EcrituresTableComponent);
    fixture.componentRef.setInput('ecritures', [
      { id: 'e2', missionId: 'mission-2', typeCompte: 'COMPTE_SEQUESTRE_PRESTATAIRE', nature: 'ENCAISSEMENT', sens: 'CREDIT', montant: 500, creeLe: '2026-01-01T00:00:00Z', statut: 'VALIDE', modePaiement: 'VIREMENT', litigeActif: false },
    ]);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Virement');
  });

  it('affiche un badge litige quand le reversement est suspendu', async () => {
    await TestBed.configureTestingModule({ imports: [EcrituresTableComponent] }).compileComponents();
    const fixture = TestBed.createComponent(EcrituresTableComponent);
    fixture.componentRef.setInput('ecritures', [
      { id: 'e3', missionId: 'mission-3', typeCompte: 'COMPTE_SEQUESTRE_PRESTATAIRE', nature: 'ENCAISSEMENT', sens: 'CREDIT', montant: 500, creeLe: '2026-01-01T00:00:00Z', statut: 'VALIDE', modePaiement: 'VIREMENT', litigeActif: true },
    ]);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Litige');
  });

  it("affiche un message quand la liste est vide", async () => {
    await TestBed.configureTestingModule({ imports: [EcrituresTableComponent] }).compileComponents();
    const fixture = TestBed.createComponent(EcrituresTableComponent);
    fixture.componentRef.setInput('ecritures', []);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Aucune écriture');
  });
});
