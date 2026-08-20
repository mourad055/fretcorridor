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

describe('libellés des enums → clés de traduction (Sprint 15, i18n au Sprint 23)', () => {
  it('mappe les statuts de dossier vers leur clé enum.*', () => {
    expect(libelleDossierStatut('OUVERT')).toBe('enum.dossierStatut.OUVERT');
    expect(libelleDossierStatut('EN_COURS')).toBe('enum.dossierStatut.EN_COURS');
    expect(libelleDossierStatut('ESCALADE')).toBe('enum.dossierStatut.ESCALADE');
    expect(libelleDossierStatut('CLOS')).toBe('enum.dossierStatut.CLOS');
    expect(libelleDossierStatut(undefined)).toBe('');
  });

  it('mappe les statuts KYC vers leur clé enum.*', () => {
    expect(libelleKycStatut('EN_ATTENTE')).toBe('enum.kycStatut.EN_ATTENTE');
    expect(libelleKycStatut('VALIDE')).toBe('enum.kycStatut.VALIDE');
    expect(libelleKycStatut('REJETE')).toBe('enum.kycStatut.REJETE');
  });

  it("mappe les états d'axe vers leur clé enum.*", () => {
    expect(libelleAxeVisibilite(true)).toBe('enum.axeVisibilite.ACTIVE');
    expect(libelleAxeVisibilite(false)).toBe('enum.axeVisibilite.INACTIVE');
    expect(libelleAxeMatching(true)).toBe('enum.axeMatching.ACTIVE');
    expect(libelleAxeMatching(false)).toBe('enum.axeMatching.INACTIVE');
    expect(libelleAxePaiement(true)).toBe('enum.axePaiement.ACTIVE');
    expect(libelleAxePaiement(false)).toBe('enum.axePaiement.INACTIVE');
  });

  it("mappe les statuts d'écriture vers leur clé enum.*", () => {
    expect(libelleEcritureStatut('VALIDE')).toBe('enum.ecritureStatut.VALIDE');
    expect(libelleEcritureStatut('SUSPENDU')).toBe('enum.ecritureStatut.SUSPENDU');
  });

  it('traduit les modes de paiement et affiche un tiret sur un reversement (mode null)', () => {
    expect(libelleModePaiement('MONNAIE_ELECTRONIQUE')).toBe('Monnaie électronique');
    expect(libelleModePaiement('VIREMENT')).toBe('Virement');
    expect(libelleModePaiement('TERME_CONTRACTUEL')).toBe('Terme contractuel');
    expect(libelleModePaiement('ESPECES')).toBe('Espèces');
    expect(libelleModePaiement(null)).toBe('—');
  });

  it('mappe les statuts de mission vers leur clé enum.*', () => {
    expect(libelleMissionStatut('CONFIRMEE')).toBe('enum.missionStatut.CONFIRMEE');
    expect(libelleMissionStatut('EN_COURS')).toBe('enum.missionStatut.EN_COURS');
    expect(libelleMissionStatut('CLOTUREE')).toBe('enum.missionStatut.CLOTUREE');
  });

  it('mappe les modes de collecte vers leur clé enum.*', () => {
    expect(libelleModeCollecte('PORTE_A_PORTE')).toBe('enum.modeCollecte.PORTE_A_PORTE');
    expect(libelleModeCollecte('POINT_DEPOT')).toBe('enum.modeCollecte.POINT_DEPOT');
  });

  it('mappe les états de capacité vers leur clé enum.*', () => {
    expect(libelleCapaciteEtat('PUBLIEE')).toBe('enum.capaciteEtat.PUBLIEE');
    expect(libelleCapaciteEtat('APPARIEE')).toBe('enum.capaciteEtat.APPARIEE');
    expect(libelleCapaciteEtat('EXPIREE')).toBe('enum.capaciteEtat.EXPIREE');
  });

  it("mappe les types d'acteur KYC connus vers leur clé enum.* et humanise les valeurs inconnues", () => {
    expect(libelleTypeActeur('CHAUFFEUR')).toBe('enum.typeActeur.CHAUFFEUR');
    expect(libelleTypeActeur('TRANSPORTEUR_PERSONNE_MORALE')).toBe('enum.typeActeur.TRANSPORTEUR_PERSONNE_MORALE');
    expect(libelleTypeActeur('AUTRE_TYPE')).toBe('Autre type');
  });

  it('mappe les types de dossier vers leur clé enum.*', () => {
    expect(libelleTypeDossier('MODERATION')).toBe('enum.typeDossier.MODERATION');
    expect(libelleTypeDossier('INCIDENT')).toBe('enum.typeDossier.INCIDENT');
    expect(libelleTypeDossier('LITIGE')).toBe('enum.typeDossier.LITIGE');
  });

  it('mappe les priorités de dossier vers leur clé enum.*', () => {
    expect(libellePrioriteDossier('BASSE')).toBe('enum.prioriteDossier.BASSE');
    expect(libellePrioriteDossier('NORMALE')).toBe('enum.prioriteDossier.NORMALE');
    expect(libellePrioriteDossier('HAUTE')).toBe('enum.prioriteDossier.HAUTE');
  });

  it("mappe les états d'étape de mission vers leur clé enum.*", () => {
    expect(libelleEtapeEtat('A_VENIR')).toBe('enum.etapeEtat.A_VENIR');
    expect(libelleEtapeEtat('EN_COURS')).toBe('enum.etapeEtat.EN_COURS');
    expect(libelleEtapeEtat('TERMINEE')).toBe('enum.etapeEtat.TERMINEE');
  });

  it("humanise les actions du journal d'audit (vocabulaire ouvert, pas traduit)", () => {
    expect(libelleJournalAction('DOSSIER_OUVERT')).toBe('Dossier ouvert');
    expect(libelleJournalAction(undefined)).toBe('');
  });
});
