import { TestBed } from '@angular/core/testing';
import { axe } from 'jest-axe';
import { TotauxEcrituresComponent } from './totaux-ecritures.component';

describe('TotauxEcrituresComponent', () => {
  it('affiche les 4 totaux', () => {
    const fixture = TestBed.createComponent(TotauxEcrituresComponent);
    fixture.componentInstance.totaux = { nombre: 3, totalCredit: 150, totalDebit: 30, solde: 120 };
    fixture.detectChanges();

    const texte = fixture.nativeElement.textContent;
    expect(texte).toContain('3');
    expect(texte).toContain('150');
    expect(texte).toContain('30');
    expect(texte).toContain('120');
  });

  it("n'a aucune violation d'accessibilité automatiquement détectable", async () => {
    const fixture = TestBed.createComponent(TotauxEcrituresComponent);
    fixture.componentInstance.totaux = { nombre: 0, totalCredit: 0, totalDebit: 0, solde: 0 };
    fixture.detectChanges();

    const resultats = await axe(fixture.nativeElement);
    expect(resultats).toHaveNoViolations();
  });
});
