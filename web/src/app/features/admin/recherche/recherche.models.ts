export type TypeResultatRecherche = 'TENANT' | 'JOURNAL_AUDIT';

export interface ResultatRecherche {
  type: TypeResultatRecherche;
  titre: string;
  detail: string;
  tenantId: string | null;
}
