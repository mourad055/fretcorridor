-- EF-GEO-05 (CDC S9.9) / Phase 4 : "gerer plusieurs pays" - sans savoir a
-- quel pays appartient un hub, impossible de determiner quelle convention
-- bilaterale (RG-052) s'applique a un axe donne. Champ absent du modele
-- CDC S13 (Hub : "ville, plateforme, point de consolidation") - ecart
-- confirme, comble ici plutot que devine en Java.
--
-- Nullable, meme principe que tenant_id (migration V4) : les hubs Phase
-- 1-2 existants (Douala, Yaounde, Bafoussam) sont retroactivement
-- camerounais - backfill explicite plutot que de laisser une donnee
-- ambigue. Tout nouveau hub cree via HubCreationRequest devra desormais
-- le renseigner (contrainte applicative, pas une contrainte NOT NULL en
-- base pour ne pas casser une eventuelle re-application de migration sur
-- des donnees anciennes non anticipees).
--
-- ISO 3166-1 alpha-3 (CMR, TCD, CAF...) - coherent avec le contrat
-- shared-contracts/asyncapi/events/repartition-conventionnelle-appliquee.yaml
ALTER TABLE geo.hub ADD COLUMN pays VARCHAR(3);

UPDATE geo.hub SET pays = 'CMR' WHERE pays IS NULL;
