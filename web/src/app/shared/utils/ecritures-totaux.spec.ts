import { Ecriture } from '../models/ecriture.models';
import { calculerTotauxEcritures, calculerTotauxPaiementTransporteur, ecrituresVersCsv } from './ecritures-totaux';

function ecriture(partial: Partial<Ecriture>): Ecriture {
  return {
    id: 'e1',
    missionId: 'mission-a',
    typeCompte: 'COMPTE_TRANSPORTEUR',
    nature: 'ENCAISSEMENT',
    sens: 'CREDIT',
    montant: 100,
    creeLe: '2026-08-01T00:00:00Z',
    statut: 'VALIDE',
    modePaiement: 'VIREMENT',
    litigeActif: false,
    ...partial,
  };
}

describe('calculerTotauxEcritures', () => {
  it('additionne credits et debits separement et calcule le solde par difference', () => {
    const totaux = calculerTotauxEcritures([
      ecriture({ sens: 'CREDIT', montant: 100 }),
      ecriture({ sens: 'CREDIT', montant: 50 }),
      ecriture({ sens: 'DEBIT', montant: 30 }),
    ]);

    expect(totaux).toEqual({ nombre: 3, totalCredit: 150, totalDebit: 30, solde: 120 });
  });

  it('renvoie des totaux nuls sur une liste vide', () => {
    expect(calculerTotauxEcritures([])).toEqual({ nombre: 0, totalCredit: 0, totalDebit: 0, solde: 0 });
  });
});

describe('calculerTotauxPaiementTransporteur', () => {
  it('somme les reversements reçus comme montant positif', () => {
    const totaux = calculerTotauxPaiementTransporteur([
      ecriture({ nature: 'REVERSEMENT', sens: 'DEBIT', montant: 165000 }),
      ecriture({ id: 'e2', nature: 'REVERSEMENT', sens: 'DEBIT', montant: 92000 }),
    ]);
    expect(totaux).toEqual({ nombre: 2, totalCredit: 0, totalDebit: 257000, solde: 257000 });
  });
});

describe('ecrituresVersCsv', () => {
  it('genere une ligne d\'entete puis une ligne par ecriture', () => {
    const csv = ecrituresVersCsv([ecriture({ missionId: 'mission-a', montant: 100 })]);
    const lignes = csv.split('\n');

    expect(lignes[0]).toBe('missionId,typeCompte,nature,sens,montant,modePaiement,creeLe,statut');
    expect(lignes[1]).toContain('mission-a');
    expect(lignes[1]).toContain('100');
  });

  it('echappe les valeurs contenant une virgule', () => {
    const csv = ecrituresVersCsv([ecriture({ nature: 'Encaissement, partiel' })]);

    expect(csv).toContain('"Encaissement, partiel"');
  });

  it('gère un modePaiement null (reversement)', () => {
    const csv = ecrituresVersCsv([ecriture({ modePaiement: null })]);

    expect(csv.split('\n')[1].split(',')).toContain('');
  });
});
