-- Données de démonstration pour service-ida — chargées UNIQUEMENT sous le
-- profil "dev" (cf. application-dev.yml : spring.sql.init.data-locations),
-- jamais en production.
--
-- Reproduit les acteurs historiquement mockés côté gateway (mêmes téléphones,
-- mêmes rôles, mêmes tenants) pour que les boutons de connexion démo du web
-- et les comptes de test mobile aient un équivalent réel côté service-ida.
--
-- PIN de développement UNIQUEMENT : "1234" pour tous les comptes, haché
-- BCrypt ci-dessous (jamais en clair — cohérent avec Acteur.codePin, cf.
-- backend/service-ida/src/main/java/.../entity/Acteur.java).
--
-- Idempotent (ON CONFLICT DO NOTHING) : rejouable sans erreur si déjà appliqué.

SET search_path TO service_ida;

INSERT INTO acteurs (id, telephone, code_pin, tenant_id, niveau_kyc, actif, tentatives_echouees, date_creation)
VALUES
    ('a0000000-0000-0000-0000-000000000001', '+237600000001', '$2y$10$Ly67HRqbsix1/e8/MUcpzO35Y63be65zdMLJHoiTWWMkjb6Sg6.xK', 'tenant-bgft-douala',   'NIVEAU_1', true, 0, now()),
    ('a0000000-0000-0000-0000-000000000002', '+237600000002', '$2y$10$Ly67HRqbsix1/e8/MUcpzO35Y63be65zdMLJHoiTWWMkjb6Sg6.xK', 'tenant-bgft-douala',   'NIVEAU_1', true, 0, now()),
    ('a0000000-0000-0000-0000-000000000003', '+237600000003', '$2y$10$Ly67HRqbsix1/e8/MUcpzO35Y63be65zdMLJHoiTWWMkjb6Sg6.xK', 'tenant-flysoft',       'NIVEAU_1', true, 0, now()),
    ('a0000000-0000-0000-0000-000000000004', '+235600000004', '$2y$10$Ly67HRqbsix1/e8/MUcpzO35Y63be65zdMLJHoiTWWMkjb6Sg6.xK', 'tenant-bnft-ndjamena', 'NIVEAU_1', true, 0, now()),
    ('a0000000-0000-0000-0000-000000000005', '+237600000005', '$2y$10$Ly67HRqbsix1/e8/MUcpzO35Y63be65zdMLJHoiTWWMkjb6Sg6.xK', 'tenant-bgft-douala',   'NIVEAU_1', true, 0, now()),
    ('a0000000-0000-0000-0000-000000000006', '+235600000006', '$2y$10$Ly67HRqbsix1/e8/MUcpzO35Y63be65zdMLJHoiTWWMkjb6Sg6.xK', 'tenant-bnft-ndjamena', 'NIVEAU_1', true, 0, now())
ON CONFLICT (id) DO NOTHING;

-- roles : table de jointure @ElementCollection (acteur_roles.acteur_id, acteur_roles.role).
-- Pas de ON CONFLICT ici : Hibernate ne pose pas nécessairement de contrainte
-- d'unicité sur cette table de jointure — NOT EXISTS reste correct dans tous les cas.
INSERT INTO acteur_roles (acteur_id, role)
SELECT v.acteur_id, v.role
FROM (VALUES
    ('a0000000-0000-0000-0000-000000000001'::uuid, 'BUREAU'),
    ('a0000000-0000-0000-0000-000000000002'::uuid, 'TRANSPORTEUR'),
    ('a0000000-0000-0000-0000-000000000003'::uuid, 'ADMINISTRATION'),
    ('a0000000-0000-0000-0000-000000000004'::uuid, 'BUREAU'),
    ('a0000000-0000-0000-0000-000000000005'::uuid, 'TRANSPORTEUR'),
    ('a0000000-0000-0000-0000-000000000006'::uuid, 'TRANSPORTEUR')
) AS v(acteur_id, role)
WHERE NOT EXISTS (
    SELECT 1 FROM acteur_roles ar WHERE ar.acteur_id = v.acteur_id AND ar.role = v.role
);
