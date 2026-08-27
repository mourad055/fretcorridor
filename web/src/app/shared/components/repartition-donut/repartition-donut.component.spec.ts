import { TestBed } from '@angular/core/testing';
import { axe } from 'jest-axe';
import { RepartitionDonutComponent } from './repartition-donut.component';

describe('RepartitionDonutComponent', () => {
  it('calcule les pourcentages de chaque segment', () => {
    const fixture = TestBed.createComponent(RepartitionDonutComponent);
    fixture.componentInstance.segments = [
      { label: 'Crédit', valeur: 750, couleur: '#067647' },
      { label: 'Débit', valeur: 250, couleur: '#b42318' },
    ];
    fixture.detectChanges();

    const calcules = fixture.componentInstance.segmentsCalcules;
    expect(calcules.map((c) => c.pourcentage)).toEqual([75, 25]);
  });

  it('ignore les segments a zero et ne rend rien si le total est nul', () => {
    const fixture = TestBed.createComponent(RepartitionDonutComponent);
    fixture.componentInstance.segments = [
      { label: 'Crédit', valeur: 0, couleur: '#067647' },
      { label: 'Débit', valeur: 0, couleur: '#b42318' },
    ];
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.repartition-donut')).toBeNull();
  });

  it('affiche toujours un libelle texte a cote de chaque couleur (legende)', () => {
    const fixture = TestBed.createComponent(RepartitionDonutComponent);
    fixture.componentInstance.segments = [
      { label: 'Crédit', valeur: 750, couleur: '#067647' },
      { label: 'Débit', valeur: 250, couleur: '#b42318' },
    ];
    fixture.detectChanges();

    const texte = fixture.nativeElement.textContent;
    expect(texte).toContain('Crédit — 750 (75 %)');
    expect(texte).toContain('Débit — 250 (25 %)');
  });

  it("n'a aucune violation d'accessibilité automatiquement détectable", async () => {
    const fixture = TestBed.createComponent(RepartitionDonutComponent);
    fixture.componentInstance.segments = [
      { label: 'Crédit', valeur: 750, couleur: '#067647' },
      { label: 'Débit', valeur: 250, couleur: '#b42318' },
    ];
    fixture.detectChanges();

    const resultats = await axe(fixture.nativeElement);
    expect(resultats).toHaveNoViolations();
  });
});
