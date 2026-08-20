-- Conformite au modele de donnees CDC S13 : "Tenant -- 1-n Axe".
-- Colonne seule en Phase 1 (MVP) - la logique d'isolation stricte
-- (ENF-MUL-01/03, filtrage actif, tests d'etancheite automatises) est
-- explicitement Phase 3 (Plan d'execution S18, "second tenant
-- institutionnel"). Un seul tenant existe en Phase 1 (BGFT, client-ancre) :
-- rien a isoler de lui-meme, mais le champ doit exister des maintenant pour
-- eviter une migration de donnees plus tard une fois Mobile/Web dependants.
ALTER TABLE geo.hub
    ADD COLUMN tenant_id UUID;

ALTER TABLE geo.axe
    ADD COLUMN tenant_id UUID;

-- Valeur de tenant unique pour la Phase 1 (BGFT). Un vrai referentiel
-- Tenant (service-ida, cf EF-IDA) arrivera avec le multi-tenant Phase 3 -
-- ici on fige juste un UUID constant pour ne pas laisser tenant_id null
-- sur les donnees existantes.
UPDATE geo.hub SET tenant_id = '00000000-0000-0000-0000-000000000001' WHERE tenant_id IS NULL;
UPDATE geo.axe SET tenant_id = '00000000-0000-0000-0000-000000000001' WHERE tenant_id IS NULL;

CREATE INDEX idx_hub_tenant_id ON geo.hub (tenant_id);
CREATE INDEX idx_axe_tenant_id ON geo.axe (tenant_id);
