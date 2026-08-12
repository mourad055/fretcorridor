import { TestBed } from '@angular/core/testing';
import {
  StatusBadgeComponent,
  dossierStatusVariant,
  kycStatusVariant,
  axeVisibiliteVariant,
  axeMatchingVariant,
  axePaiementVariant,
  ecritureStatusVariant,
  missionStatusVariant,
  capaciteStatusVariant,
  libelleDossierStatut,
  libelleKycStatut,
  libelleAxeVisibilite,
  libelleAxeMatching,
  libelleAxePaiement,
  libelleEcritureStatut,
  libelleMissionStatut,
  libelleModeCollecte,
  libelleCapaciteEtat,
  libelleTypeActeur,
  libelleTypeDossier,
  libellePrioriteDossier,
  libelleEtapeEtat,
  libelleJournalAction,
  libelleModePaiement,
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

  it("mappe les états d'axe (3 booléens indépendants, EF-GEO-03)", () => {
    expect(axeVisibiliteVariant(true)).toBe('neutral');
    expect(axeVisibiliteVariant(false)).toBe('danger');
    expect(axeMatchingVariant(true)).toBe('primary');
    expect(axeMatchingVariant(false)).toBe('danger');
    expect(axePaiementVariant(true)).toBe('success');
    expect(axePaiementVariant(false)).toBe('danger');
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
    expect(libelleAxeVisibilite(true)).toBe('Visible');
    expect(libelleAxeVisibilite(false)).toBe('Masqué');
    expect(libelleAxeMatching(true)).toBe('Matching actif');
    expect(libelleAxeMatching(false)).toBe('Matching inactif');
    expect(libelleAxePaiement(true)).toBe('Paiement actif');
    expect(libelleAxePaiement(false)).toBe('Paiement inactif');
  });

  it("traduit les statuts d'écriture", () => {
    expect(libelleEcritureStatut('VALIDE')).toBe('Validée');
    expect(libelleEcritureStatut('SUSPENDU')).toBe('Suspendue');
  });

  it('traduit les modes de paiement et affiche un tiret sur un reversement (mode null)', () => {
    expect(libelleModePaiement('MONNAIE_ELECTRONIQUE')).toBe('Monnaie électronique');
    expect(libelleModePaiement('VIREMENT')).toBe('Virement');
    expect(libelleModePaiement('TERME_CONTRACTUEL')).toBe('Terme contractuel');
    expect(libelleModePaiement('ESPECES')).toBe('Espèces');
    expect(libelleModePaiement(null)).toBe('—');
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
