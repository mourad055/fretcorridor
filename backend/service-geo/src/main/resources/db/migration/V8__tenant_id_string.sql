-- ENF-MUL-01 : tenantId circule partout ailleurs dans le systeme (JWT,
-- gateway, RealGeoAdapter) comme un identifiant texte libre (ex.
-- "tenant-bgft-douala"), jamais comme un UUID. La colonne avait ete typee
-- UUID par erreur en V4 ; ce correctif change son type et convertit les
-- donnees existantes vers le meme tenant BGFT reference partout ailleurs.

ALTER TABLE geo.hub ALTER COLUMN tenant_id TYPE VARCHAR(100) USING tenant_id::text;
ALTER TABLE geo.axe ALTER COLUMN tenant_id TYPE VARCHAR(100) USING tenant_id::text;

-- Convertit le tenant fixe applique par V4, et comble aussi les lignes
-- restees NULL (donnees de demo inserees apres V4, jamais couvertes par son
-- backfill) : aucune ligne ne doit rester sans tenant (cf. Axe.java).
UPDATE geo.hub SET tenant_id = 'tenant-bgft-douala' WHERE tenant_id = '00000000-0000-0000-0000-000000000001' OR tenant_id IS NULL;
UPDATE geo.axe SET tenant_id = 'tenant-bgft-douala' WHERE tenant_id = '00000000-0000-0000-0000-000000000001' OR tenant_id IS NULL;
