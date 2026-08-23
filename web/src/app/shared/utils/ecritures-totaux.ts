import { Ecriture } from '../models/ecriture.models';

/**
 * Agrégation du grand livre (audit UX 2026-08-23,
 * docs/AUDIT_ROADMAP_Backoffice_Web_2026-08-23.md §1.7) : les 3 rapports
 * financiers (Admin, Bureau, Transporteur) n'affichaient que des lignes
 * brutes, sans aucun total. `sens` ∈ {CREDIT, DEBIT} (backend
 * service-pay/SensEcriture), `montant` toujours strictement positif — le
 * solde se calcule donc par différence, jamais par simple somme.
 */
export interface TotauxEcritures {
  nombre: number;
  totalCredit: number;
  totalDebit: number;
  solde: number;
}

export function calculerTotauxEcritures(ecritures: Ecriture[]): TotauxEcritures {
  const totalCredit = ecritures.filter((e) => e.sens === 'CREDIT').reduce((somme, e) => somme + e.montant, 0);
  const totalDebit = ecritures.filter((e) => e.sens === 'DEBIT').reduce((somme, e) => somme + e.montant, 0);
  return { nombre: ecritures.length, totalCredit, totalDebit, solde: totalCredit - totalDebit };
}

const ENTETE_CSV = 'missionId,typeCompte,nature,sens,montant,modePaiement,creeLe,statut';

function champCsv(valeur: string | number | null): string {
  const texte = String(valeur ?? '');
  return texte.includes(',') || texte.includes('"') ? `"${texte.replace(/"/g, '""')}"` : texte;
}

export function ecrituresVersCsv(ecritures: Ecriture[]): string {
  const lignes = ecritures.map((e) =>
    [e.missionId, e.typeCompte, e.nature, e.sens, e.montant, e.modePaiement, e.creeLe, e.statut]
      .map(champCsv)
      .join(',')
  );
  return [ENTETE_CSV, ...lignes].join('\n');
}

/** Déclenche le téléchargement d'un CSV généré côté client (pas d'appel serveur). */
export function telechargerCsv(nomFichier: string, contenu: string): void {
  const blob = new Blob([contenu], { type: 'text/csv' });
  const url = URL.createObjectURL(blob);
  const lien = document.createElement('a');
  lien.href = url;
  lien.download = nomFichier;
  lien.click();
  URL.revokeObjectURL(url);
}
