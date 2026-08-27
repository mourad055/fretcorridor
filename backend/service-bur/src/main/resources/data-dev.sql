-- Données de démonstration pour service-bur — chargées UNIQUEMENT sous le
-- profil "dev" (cf. application-dev.yml), jamais en production.
--
-- Missions appariées + positions temps réel pour les deux tenants de
-- démonstration, pour que "Missions appariées" et "Suivi temps réel" côté
-- Bureau de fret ne soient jamais vides en environnement de démo.
--
-- axe_id reprend les UUID d'axes définis dans
-- backend/service-geo/src/main/resources/data-dev.sql (pas de FK
-- inter-service, juste une cohérence d'identifiant pour l'affichage).
-- transporteur_id reprend les comptes de
-- backend/service-ida/src/main/resources/data-dev.sql.
--
-- Idempotent (ON CONFLICT DO NOTHING).

INSERT INTO public.missions_appariees (id, axe_id, confirmee_le, destination_nom, devise, event_id, mission_id, origine_nom, prix_transport, tenant_id, transporteur_id)
VALUES
    ('30000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000001', now() - interval '2 hours',  'Yaoundé',    'XAF', '40000000-0000-0000-0000-000000000001', '30000000-0000-0000-0000-000000000001', 'Douala',     125000.0, 'tenant-bgft-douala',   'a0000000-0000-0000-0000-000000000002'),
    ('30000000-0000-0000-0000-000000000010', '20000000-0000-0000-0000-000000000001', now() - interval '1 day',    'Yaoundé',    'XAF', '40000000-0000-0000-0000-000000000010', '30000000-0000-0000-0000-000000000010', 'Douala',     118000.0, 'tenant-bgft-douala',   'a0000000-0000-0000-0000-000000000005'),
    ('30000000-0000-0000-0000-000000000011', '20000000-0000-0000-0000-000000000001', now() - interval '3 days',   'Yaoundé',    'XAF', '40000000-0000-0000-0000-000000000011', '30000000-0000-0000-0000-000000000011', 'Douala',     132000.0, 'tenant-bgft-douala',   'a0000000-0000-0000-0000-000000000002'),
    ('30000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000002', now() - interval '5 hours',  'Bafoussam',  'XAF', '40000000-0000-0000-0000-000000000002', '30000000-0000-0000-0000-000000000002', 'Douala',      98000.0, 'tenant-bgft-douala',   'a0000000-0000-0000-0000-000000000005'),
    ('30000000-0000-0000-0000-000000000012', '20000000-0000-0000-0000-000000000002', now() - interval '1 day',    'Bafoussam',  'XAF', '40000000-0000-0000-0000-000000000012', '30000000-0000-0000-0000-000000000012', 'Douala',     105000.0, 'tenant-bgft-douala',   'a0000000-0000-0000-0000-000000000002'),
    ('30000000-0000-0000-0000-000000000013', '20000000-0000-0000-0000-000000000002', now() - interval '4 days',   'Bafoussam',  'XAF', '40000000-0000-0000-0000-000000000013', '30000000-0000-0000-0000-000000000013', 'Douala',     112000.0, 'tenant-bgft-douala',   'a0000000-0000-0000-0000-000000000005'),
    ('30000000-0000-0000-0000-000000000014', '20000000-0000-0000-0000-000000000003', now() - interval '6 hours',  'N''Djamena', 'XAF', '40000000-0000-0000-0000-000000000014', '30000000-0000-0000-0000-000000000014', 'Yaoundé',    420000.0, 'tenant-bgft-douala',   'a0000000-0000-0000-0000-000000000002'),
    ('30000000-0000-0000-0000-000000000015', '20000000-0000-0000-0000-000000000003', now() - interval '2 days',   'N''Djamena', 'XAF', '40000000-0000-0000-0000-000000000015', '30000000-0000-0000-0000-000000000015', 'Yaoundé',    380000.0, 'tenant-bgft-douala',   'a0000000-0000-0000-0000-000000000005'),
    ('30000000-0000-0000-0000-000000000016', '20000000-0000-0000-0000-000000000003', now() - interval '5 days',   'N''Djamena', 'XAF', '40000000-0000-0000-0000-000000000016', '30000000-0000-0000-0000-000000000016', 'Yaoundé',    450000.0, 'tenant-bgft-douala',   'a0000000-0000-0000-0000-000000000002'),
    ('30000000-0000-0000-0000-000000000017', '20000000-0000-0000-0000-000000000003', now() - interval '3 days',   'Yaoundé',    'XAF', '40000000-0000-0000-0000-000000000017', '30000000-0000-0000-0000-000000000017', 'N''Djamena', 395000.0, 'tenant-bgft-douala',   'a0000000-0000-0000-0000-000000000005'),
    ('30000000-0000-0000-0000-000000000003', '20000000-0000-0000-0000-000000000004', now() - interval '2 hours',  'Garoua',     'XAF', '40000000-0000-0000-0000-000000000003', '30000000-0000-0000-0000-000000000003', 'N''Djamena', 210000.0, 'tenant-bnft-ndjamena', 'a0000000-0000-0000-0000-000000000006'),
    ('30000000-0000-0000-0000-000000000004', '20000000-0000-0000-0000-000000000005', now() - interval '5 hours',  'N''Djamena', 'XAF', '40000000-0000-0000-0000-000000000004', '30000000-0000-0000-0000-000000000004', 'Garoua',     175000.0, 'tenant-bnft-ndjamena', 'a0000000-0000-0000-0000-000000000006')
ON CONFLICT (id) DO NOTHING;

-- Missions ingérées sans prix (tests mobile / Kafka) : valeurs de démo pour que
-- l'observatoire ne plante pas au calcul de la médiane (RG-085, EF-BUR-03).
UPDATE public.missions_appariees
SET prix_transport = 115000.0,
    devise = COALESCE(devise, 'XAF')
WHERE tenant_id = 'tenant-bgft-douala'
  AND prix_transport IS NULL;

INSERT INTO public.positions (id, captured_le, latitude, longitude, mission_id, tenant_id, vehicule_id)
VALUES
    ('30000000-0000-0000-0000-000000000001', now() - interval '5 minutes',  3.9902, 10.2882, '30000000-0000-0000-0000-000000000001', 'tenant-bgft-douala',   '50000000-0000-0000-0000-000000000001'),
    ('30000000-0000-0000-0000-000000000002', now() - interval '10 minutes',5.0469, 10.2227, '30000000-0000-0000-0000-000000000002', 'tenant-bgft-douala',   '50000000-0000-0000-0000-000000000002'),
    ('30000000-0000-0000-0000-000000000003', now() - interval '5 minutes', 10.7500,14.2000, '30000000-0000-0000-0000-000000000003', 'tenant-bnft-ndjamena', '50000000-0000-0000-0000-000000000003'),
    ('30000000-0000-0000-0000-000000000004', now() - interval '10 minutes',9.8000, 13.8000, '30000000-0000-0000-0000-000000000004', 'tenant-bnft-ndjamena', '50000000-0000-0000-0000-000000000004')
ON CONFLICT (id) DO UPDATE SET
    captured_le = EXCLUDED.captured_le,
    latitude = EXCLUDED.latitude,
    longitude = EXCLUDED.longitude;

-- Alignement demo sur les positions réelles ingérées par service-flt (tests
-- mobile Yaoundé→N'Djamena, etc.) — écrase les seeds ci-dessus si plus récent.
INSERT INTO public.positions (id, mission_id, tenant_id, vehicule_id, latitude, longitude, captured_le)
SELECT gen_random_uuid(), p.mission_id, p.tenant_id, m.vehicule_id, p.latitude, p.longitude, p.horodatage AT TIME ZONE 'UTC'
FROM (
    SELECT DISTINCT ON (mission_id) mission_id, tenant_id, latitude, longitude, horodatage
    FROM service_flt.positions
    ORDER BY mission_id, horodatage DESC
) p
LEFT JOIN service_exe.missions m ON m.id = p.mission_id
ON CONFLICT (mission_id) DO UPDATE SET
    latitude = EXCLUDED.latitude,
    longitude = EXCLUDED.longitude,
    captured_le = EXCLUDED.captured_le,
    vehicule_id = COALESCE(EXCLUDED.vehicule_id, public.positions.vehicule_id),
    tenant_id = EXCLUDED.tenant_id
WHERE public.positions.captured_le < EXCLUDED.captured_le;
