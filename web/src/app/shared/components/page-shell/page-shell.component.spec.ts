import { TestBed } from '@angular/core/testing';
import { Component } from '@angular/core';
import { axe } from 'jest-axe';
import { PageShellComponent } from './page-shell.component';

@Component({
  standalone: true,
  imports: [PageShellComponent],
  template: `<app-page-shell><p>contenu projeté</p></app-page-shell>`,
})
class HoteDeTestComponent {}

describe('PageShellComponent', () => {
  it('projette son contenu dans un <main class="fc-page">', () => {
    TestBed.configureTestingModule({ imports: [HoteDeTestComponent] });
    const fixture = TestBed.createComponent(HoteDeTestComponent);
    fixture.detectChanges();

    const main = fixture.nativeElement.querySelector('main.fc-page');
    expect(main).toBeTruthy();
    expect(main.textContent).toContain('contenu projeté');
  });

  it("n'a aucune violation d'accessibilité automatiquement détectable", async () => {
    TestBed.configureTestingModule({ imports: [HoteDeTestComponent] });
    const fixture = TestBed.createComponent(HoteDeTestComponent);
    fixture.detectChanges();

    const resultats = await axe(fixture.nativeElement);
    expect(resultats).toHaveNoViolations();
  });

  it('ajoute la classe de scoping propre au composant sur le même <main>', () => {
    TestBed.configureTestingModule({ imports: [PageShellComponent] });
    const fixture = TestBed.createComponent(PageShellComponent);
    fixture.componentInstance.extraClass = 'kyc-dashboard';
    fixture.detectChanges();

    const main = fixture.nativeElement.querySelector('main');
    expect(main.classList.contains('fc-page')).toBe(true);
    expect(main.classList.contains('kyc-dashboard')).toBe(true);
  });
});
