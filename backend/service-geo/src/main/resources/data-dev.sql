-- Données de démonstration pour service-geo — chargées UNIQUEMENT sous le
-- profil "dev" (cf. application-dev.yml : spring.sql.init.data-locations),
-- jamais en production. Exécuté après les migrations Flyway (comportement
-- par défaut de Spring Boot : SQL init tourne après Flyway).
--
-- 5 hubs (Cameroun + Tchad) et des axes couvrant les deux tenants de
-- démonstration (tenant-bgft-douala, tenant-bnft-ndjamena), pour que la
-- carte des axes du Bureau de fret ne soit jamais vide en environnement de
-- démo. Idempotent (ON CONFLICT DO NOTHING).

SET search_path TO geo, public;

INSERT INTO hub (id, nom, ville, type_hub, position, pays, date_creation)
VALUES
    ('10000000-0000-0000-0000-000000000001', 'Douala',    'Douala',    'VILLE', ST_SetSRID(ST_MakePoint(9.7679::float8, 4.0511::float8), 4326), 'CMR', now()),
    ('10000000-0000-0000-0000-000000000002', 'Yaoundé',   'Yaoundé',   'VILLE', ST_SetSRID(ST_MakePoint(11.5021::float8, 3.8480::float8), 4326), 'CMR', now()),
    ('10000000-0000-0000-0000-000000000003', 'Bafoussam', 'Bafoussam', 'VILLE', ST_SetSRID(ST_MakePoint(10.4176::float8, 5.4737::float8), 4326), 'CMR', now()),
    ('10000000-0000-0000-0000-000000000004', 'N''Djamena','N''Djamena','VILLE', ST_SetSRID(ST_MakePoint(15.0557::float8, 12.1348::float8), 4326), 'TCD', now()),
    ('10000000-0000-0000-0000-000000000005', 'Garoua',    'Garoua',    'VILLE', ST_SetSRID(ST_MakePoint(13.3921::float8, 9.3017::float8), 4326), 'CMR', now())
ON CONFLICT (id) DO NOTHING;

-- Axes tenant-bgft-douala (Cameroun, tenant principal Phase 1)
INSERT INTO axe (id, nom, hub_origine_id, hub_destination_id, visibilite_active, matching_actif, paiement_actif, parametres, tenant_id, date_creation)
VALUES
    ('20000000-0000-0000-0000-000000000001', 'Douala - Yaoundé',   '10000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000002', true, true, true,  '{}'::jsonb, 'tenant-bgft-douala', now()),
    ('20000000-0000-0000-0000-000000000002', 'Douala - Bafoussam', '10000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000003', true, true, false, '{}'::jsonb, 'tenant-bgft-douala', now()),
    ('20000000-0000-0000-0000-000000000003', 'Yaoundé - N''Djamena','10000000-0000-0000-0000-000000000002','10000000-0000-0000-0000-000000000004', true, true, true,  '{}'::jsonb, 'tenant-bgft-douala', now())
ON CONFLICT (id) DO NOTHING;

-- Axes tenant-bnft-ndjamena (Tchad)
INSERT INTO axe (id, nom, hub_origine_id, hub_destination_id, visibilite_active, matching_actif, paiement_actif, parametres, tenant_id, date_creation)
VALUES
    ('20000000-0000-0000-0000-000000000004', 'N''Djamena - Garoua', '10000000-0000-0000-0000-000000000004', '10000000-0000-0000-0000-000000000005', true, true, true,  '{}'::jsonb, 'tenant-bnft-ndjamena', now()),
    ('20000000-0000-0000-0000-000000000005', 'Garoua - N''Djamena', '10000000-0000-0000-0000-000000000005', '10000000-0000-0000-0000-000000000004', true, true, false, '{}'::jsonb, 'tenant-bnft-ndjamena', now())
ON CONFLICT (id) DO NOTHING;

-- Axes MARKETPLACE_CM : tenant assigne par defaut a l'inscription publique
-- chargeur (cf. Acteur.tenantId cote service-ida, "MARKETPLACE_CM par
-- defaut"). Sans ca, un compte Chargeur fraichement auto-inscrit ne voit
-- jamais aucun axe (le selecteur de dio_provider.dart/axes_provider.dart
-- reste vide, aucune erreur affichee) - trouve le 24/08 en debug croise
-- avec un coequipier dont le compte de test retombait sur ce tenant.
INSERT INTO axe (id, nom, hub_origine_id, hub_destination_id, visibilite_active, matching_actif, paiement_actif, parametres, tenant_id, date_creation)
VALUES
    ('20000000-0000-0000-0000-000000000006', 'Douala - Yaoundé', '10000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000002', true, true, true, '{}'::jsonb, 'MARKETPLACE_CM', now()),
    ('20000000-0000-0000-0000-000000000007', 'Yaoundé - Douala', '10000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000001', true, true, true, '{}'::jsonb, 'MARKETPLACE_CM', now())
ON CONFLICT (id) DO NOTHING;
