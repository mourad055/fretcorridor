import { TestBed } from '@angular/core/testing';
import {
  StatusBadgeComponent,
  dossierStatusVariant,
  kycStatusVariant,
  axeStatusVariant,
  ecritureStatusVariant,
  missionStatusVariant,
  capaciteStatusVariant,
  libelleDossierStatut,
  libelleKycStatut,
  libelleAxeEtat,
  libelleEcritureStatut,
  libelleMissionStatut,
  libelleModeCollecte,
  libelleCapaciteEtat,
  libelleTypeActeur,
  libelleTypeDossier,
  libellePrioriteDossier,
  libelleEtapeEtat,
  libelleJournalAction,
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

  it('mappe les statuts de mission', () => {
    expect(missionStatusVariant('CLOTUREE')).toBe('success');
    expect(missionStatusVariant('EN_COURS')).toBe('primary');
    expect(missionStatusVariant('CONFIRMEE')).toBe('neutral');
  });

  it('mappe les états de capacité', () => {
    expect(capaciteStatusVariant('APPARIEE')).toBe('success');
    expect(capaciteStatusVariant('PUBLIEE')).toBe('primary');
    expect(capaciteStatusVariant('EXPIREE')).toBe('danger');
  });
});

describe('libellés FR des enums (Sprint 15)', () => {
  it('traduit les statuts de dossier', () => {
    expect(libelleDossierStatut('OUVERT')).toBe('Ouvert');
    expect(libelleDossierStatut('EN_COURS')).toBe('En cours');
    expect(libelleDossierStatut('ESCALADE')).toBe('Escaladé');
    expect(libelleDossierStatut('CLOS')).toBe('Clos');
    expect(libelleDossierStatut(undefined)).toBe('');
  });

  it('traduit les statuts KYC', () => {
    expect(libelleKycStatut('EN_ATTENTE')).toBe('En attente');
    expect(libelleKycStatut('VALIDE')).toBe('Validé');
    expect(libelleKycStatut('REJETE')).toBe('Rejeté');
  });

  it("traduit les états d'axe", () => {
    expect(libelleAxeEtat('VISIBILITE')).toBe('Visibilité');
    expect(libelleAxeEtat('MATCHING')).toBe('Matching');
    expect(libelleAxeEtat('PAIEMENT')).toBe('Paiement');
  });

  it("traduit les statuts d'écriture", () => {
    expect(libelleEcritureStatut('VALIDE')).toBe('Validée');
    expect(libelleEcritureStatut('SUSPENDU')).toBe('Suspendue');
  });

  it('traduit les statuts de mission', () => {
    expect(libelleMissionStatut('CONFIRMEE')).toBe('Confirmée');
    expect(libelleMissionStatut('EN_COURS')).toBe('En cours');
    expect(libelleMissionStatut('CLOTUREE')).toBe('Clôturée');
  });

  it('traduit les modes de collecte', () => {
    expect(libelleModeCollecte('PORTE_A_PORTE')).toBe('Porte à porte');
    expect(libelleModeCollecte('POINT_DEPOT')).toBe('Point de dépôt');
  });

  it('traduit les états de capacité', () => {
    expect(libelleCapaciteEtat('PUBLIEE')).toBe('Publiée');
    expect(libelleCapaciteEtat('APPARIEE')).toBe('Appariée');
    expect(libelleCapaciteEtat('EXPIREE')).toBe('Expirée');
  });

  it("traduit les types d'acteur KYC connus et humanise les valeurs inconnues", () => {
    expect(libelleTypeActeur('CHAUFFEUR')).toBe('Chauffeur');
    expect(libelleTypeActeur('TRANSPORTEUR_PERSONNE_MORALE')).toBe('Transporteur (personne morale)');
    expect(libelleTypeActeur('AUTRE_TYPE')).toBe('Autre type');
  });

  it('traduit les types de dossier', () => {
    expect(libelleTypeDossier('MODERATION')).toBe('Modération');
    expect(libelleTypeDossier('INCIDENT')).toBe('Incident');
    expect(libelleTypeDossier('LITIGE')).toBe('Litige');
  });

  it('traduit les priorités de dossier', () => {
    expect(libellePrioriteDossier('BASSE')).toBe('Basse');
    expect(libellePrioriteDossier('NORMALE')).toBe('Normale');
    expect(libellePrioriteDossier('HAUTE')).toBe('Haute');
  });

  it("traduit les états d'étape de mission", () => {
    expect(libelleEtapeEtat('A_VENIR')).toBe('À venir');
    expect(libelleEtapeEtat('EN_COURS')).toBe('En cours');
    expect(libelleEtapeEtat('TERMINEE')).toBe('Terminée');
  });

  it("humanise les actions du journal d'audit", () => {
    expect(libelleJournalAction('DOSSIER_OUVERT')).toBe('Dossier ouvert');
    expect(libelleJournalAction(undefined)).toBe('');
  });
});
