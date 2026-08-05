import { TestBed } from '@angular/core/testing';
import {
  StatusBadgeComponent,
  dossierStatusVariant,
  kycStatusVariant,
  axeStatusVariant,
  ecritureStatusVariant,
} from './status-badge.component';

describe('StatusBadgeComponent', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [StatusBadgeComponent],
    }).compileComponents();
  });

  it('affiche le libellé avec la classe de variante', () => {
    const fixture = TestBed.createComponent(StatusBadgeComponent);
    fixture.componentRef.setInput('label', 'CLOS');
    fixture.componentRef.setInput('variant', 'success');
    fixture.detectChanges();

    const badge = fixture.nativeElement.querySelector('.fc-badge');
    expect(badge.textContent.trim()).toBe('CLOS');
    expect(badge.classList).toContain('fc-badge--success');
  });

  it('utilise neutral par défaut', () => {
    const fixture = TestBed.createComponent(StatusBadgeComponent);
    fixture.componentRef.setInput('label', 'INCONNU');
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.fc-badge').classList).toContain('fc-badge--neutral');
  });
});

describe('mapping statut → variante', () => {
  it('mappe les statuts de dossier', () => {
    expect(dossierStatusVariant('EN_COURS')).toBe('primary');
    expect(dossierStatusVariant('CLOS')).toBe('success');
    expect(dossierStatusVariant('ESCALADE')).toBe('danger');
    expect(dossierStatusVariant('OUVERT')).toBe('warning');
    expect(dossierStatusVariant(undefined)).toBe('neutral');
  });

  it('mappe les statuts KYC', () => {
    expect(kycStatusVariant('VALIDE')).toBe('success');
    expect(kycStatusVariant('REJETE')).toBe('danger');
    expect(kycStatusVariant('EN_ATTENTE')).toBe('warning');
  });

  it("mappe les états d'axe", () => {
    expect(axeStatusVariant('PAIEMENT')).toBe('success');
    expect(axeStatusVariant('MATCHING')).toBe('primary');
    expect(axeStatusVariant('VISIBILITE')).toBe('neutral');
  });

  it('mappe les statuts d\'écriture', () => {
    expect(ecritureStatusVariant('VALIDE')).toBe('success');
    expect(ecritureStatusVariant('SUSPENDU')).toBe('danger');
  });
});
