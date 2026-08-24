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

  it('affiche le donut de repartition credit/debit quand il y a des ecritures', () => {
    const fixture = TestBed.createComponent(TotauxEcrituresComponent);
    fixture.componentInstance.totaux = { nombre: 2, totalCredit: 750, totalDebit: 250, solde: 500 };
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('app-repartition-donut')).toBeTruthy();
  });

  it('ne montre pas de donut sans ecritures', () => {
    const fixture = TestBed.createComponent(TotauxEcrituresComponent);
    fixture.componentInstance.totaux = { nombre: 0, totalCredit: 0, totalDebit: 0, solde: 0 };
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('app-repartition-donut')).toBeNull();
  });

  it("n'a aucune violation d'accessibilité automatiquement détectable", async () => {
    const fixture = TestBed.createComponent(TotauxEcrituresComponent);
    fixture.componentInstance.totaux = { nombre: 0, totalCredit: 0, totalDebit: 0, solde: 0 };
    fixture.detectChanges();

    const resultats = await axe(fixture.nativeElement);
    expect(resultats).toHaveNoViolations();
  });

  it("n'a aucune violation d'accessibilité avec le donut affiché", async () => {
    const fixture = TestBed.createComponent(TotauxEcrituresComponent);
    fixture.componentInstance.totaux = { nombre: 2, totalCredit: 750, totalDebit: 250, solde: 500 };
    fixture.detectChanges();

    const resultats = await axe(fixture.nativeElement);
    expect(resultats).toHaveNoViolations();
  });
});
