-- ENF-MUL-01 : isolation tenant. Colonne seule pour l'instant (Phase 1/2),
-- meme demarche que geo.axe (V4 -> V8) : le champ existe des maintenant pour
-- eviter une migration de donnees plus tard, la logique de filtrage actif
-- reste Phase 3 (Plan d'execution S18). Type VARCHAR (jamais UUID), coherent
-- avec la convention deja fixee cote GEO.
ALTER TABLE opt.affectation
    ADD COLUMN tenant_id VARCHAR(100);

CREATE INDEX idx_affectation_tenant_id ON opt.affectation (tenant_id);
