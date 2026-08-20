export interface EntreeJournalAudit {
  id: string;
  tenantId: string | null;
  acteurId: string;
  action: string;
  ressource: string;
  horodatage: string;
}
