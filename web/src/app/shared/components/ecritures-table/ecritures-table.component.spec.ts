import { TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { EcrituresTableComponent } from './ecritures-table.component';
import { Ecriture } from '../../models/ecriture.models';
import { provideTranslateServiceForTests } from '../../../../testing/translate-testing.providers';

const ECRITURES: Ecriture[] = [
  { id: 'e1', missionId: 'mission-1', typeCompte: 'COMPTE_TRANSPORTEUR', nature: 'REVERSEMENT', sens: 'DEBIT', montant: 90, creeLe: '2026-01-01T00:00:00Z', statut: 'VALIDE', modePaiement: null, litigeActif: false },
];

describe('EcrituresTableComponent', () => {
  it('affiche une ligne par ecriture', async () => {
    await TestBed.configureTestingModule({
      imports: [EcrituresTableComponent],
      providers: [provideTranslateServiceForTests()],
    }).compileComponents();
    const fixture = TestBed.createComponent(EcrituresTableComponent);
    fixture.componentRef.setInput('ecritures', ECRITURES);
    fixture.detectChanges();

    expect(fixture.debugElement.queryAll(By.css('tbody tr'))).toHaveLength(1);
  });

  it('affiche le mode de paiement quand connu et le masque sur un reversement', async () => {
    await TestBed.configureTestingModule({
      imports: [EcrituresTableComponent],
      providers: [provideTranslateServiceForTests()],
    }).compileComponents();
    const fixture = TestBed.createComponent(EcrituresTableComponent);
    fixture.componentRef.setInput('ecritures', [
      { id: 'e2', missionId: 'mission-2', typeCompte: 'COMPTE_SEQUESTRE_PRESTATAIRE', nature: 'ENCAISSEMENT', sens: 'CREDIT', montant: 500, creeLe: '2026-01-01T00:00:00Z', statut: 'VALIDE', modePaiement: 'VIREMENT', litigeActif: false },
    ]);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Virement');
  });

  it('affiche un badge litige quand le reversement est suspendu', async () => {
    await TestBed.configureTestingModule({
      imports: [EcrituresTableComponent],
      providers: [provideTranslateServiceForTests()],
    }).compileComponents();
    const fixture = TestBed.createComponent(EcrituresTableComponent);
    fixture.componentRef.setInput('ecritures', [
      { id: 'e3', missionId: 'mission-3', typeCompte: 'COMPTE_SEQUESTRE_PRESTATAIRE', nature: 'ENCAISSEMENT', sens: 'CREDIT', montant: 500, creeLe: '2026-01-01T00:00:00Z', statut: 'VALIDE', modePaiement: 'VIREMENT', litigeActif: true },
    ]);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Litige');
  });

  it('pagine au-dela de 20 ecritures et revient a la page 1 quand la liste change', async () => {
    await TestBed.configureTestingModule({
      imports: [EcrituresTableComponent],
      providers: [provideTranslateServiceForTests()],
    }).compileComponents();
    const fixture = TestBed.createComponent(EcrituresTableComponent);
    const beaucoup: Ecriture[] = Array.from({ length: 25 }, (_, i) => ({
      id: `e${i}`,
      missionId: `mission-${i}`,
      typeCompte: 'COMPTE_TRANSPORTEUR',
      nature: 'REVERSEMENT',
      sens: 'DEBIT',
      montant: 10,
      creeLe: '2026-01-01T00:00:00Z',
      statut: 'VALIDE',
      modePaiement: null,
      litigeActif: false,
    }));
    fixture.componentRef.setInput('ecritures', beaucoup);
    fixture.detectChanges();

    expect(fixture.debugElement.queryAll(By.css('tbody tr'))).toHaveLength(20);
    expect(fixture.nativeElement.textContent).toContain('Page 1 / 2');

    fixture.componentInstance.page.set(2);
    fixture.detectChanges();
    expect(fixture.debugElement.queryAll(By.css('tbody tr'))).toHaveLength(5);

    // un nouveau tableau (ex. filtre drill-down cote parent) doit ramener a la page 1
    fixture.componentRef.setInput('ecritures', beaucoup.slice(0, 3));
    fixture.detectChanges();
    expect(fixture.componentInstance.page()).toBe(1);
  });

  it("affiche un message quand la liste est vide", async () => {
    await TestBed.configureTestingModule({
      imports: [EcrituresTableComponent],
      providers: [provideTranslateServiceForTests()],
    }).compileComponents();
    const fixture = TestBed.createComponent(EcrituresTableComponent);
    fixture.componentRef.setInput('ecritures', []);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Aucune écriture');
  });
});
