-- Proposition de données de démonstration pour service-ida — À REVOIR ET FUSIONNER PAR MOBILE
-- ================================================================================
--
-- Contexte (docs/ROADMAP_INTEGRATION_gateway.md, Phase 1) : le gateway bascule son
-- authentification d'un mock local vers un vrai appel HTTP à service-ida
-- (POST /api/auth/login). service-ida n'a aujourd'hui aucune donnée de démonstration
-- (seulement schema.sql, pas de data.sql) et aucun endpoint de création de compte
-- BUREAU/TRANSPORTEUR/ADMINISTRATION (seule l'auto-inscription chargeur existe) —
-- sans ce script, les boutons de connexion rapide du web et les 5 acteurs
-- historiquement mockés côté gateway n'ont plus d'équivalent réel.
--
-- Reproduit exactement les 5 acteurs de l'ancien MockIdaAuthenticationAdapter
-- (mêmes téléphones, mêmes rôles, mêmes tenants) pour que rien ne change côté
-- démonstration/tests manuels après le basculement.
--
-- PIN de développement UNIQUEMENT : "1234" pour les 5 comptes, haché BCrypt
-- ci-dessous (jamais en clair — cohérent avec Acteur.codePin, cf.
-- backend/service-ida/src/main/java/.../entity/Acteur.java).
--
-- Comment l'utiliser (au choix de Mobile, porteur de service-ida) :
--   1) Comme backend/service-ida/src/main/resources/data-dev.sql, avec
--      spring.jpa.defer-datasource-initialization: true dans application.yml
--      (indispensable : sans ce flag, Spring exécute data.sql AVANT que
--      Hibernate ne crée les tables via ddl-auto=update, et l'insertion échoue) ;
--   2) Ou exécuté manuellement (psql) après le premier démarrage du service,
--      une fois que Hibernate a créé le schéma.
--
-- Idempotent (ON CONFLICT DO NOTHING) : rejouable sans erreur si déjà appliqué.

SET search_path TO service_ida;

INSERT INTO acteurs (id, telephone, code_pin, tenant_id, niveau_kyc, actif, tentatives_echouees, date_creation)
VALUES
    ('a0000000-0000-0000-0000-000000000001', '+237600000001', '$2y$10$Ly67HRqbsix1/e8/MUcpzO35Y63be65zdMLJHoiTWWMkjb6Sg6.xK', 'tenant-bgft-douala',   'NIVEAU_1', true, 0, now()),
    ('a0000000-0000-0000-0000-000000000002', '+237600000002', '$2y$10$Ly67HRqbsix1/e8/MUcpzO35Y63be65zdMLJHoiTWWMkjb6Sg6.xK', 'tenant-bgft-douala',   'NIVEAU_1', true, 0, now()),
    ('a0000000-0000-0000-0000-000000000003', '+237600000003', '$2y$10$Ly67HRqbsix1/e8/MUcpzO35Y63be65zdMLJHoiTWWMkjb6Sg6.xK', 'tenant-flysoft',       'NIVEAU_1', true, 0, now()),
    ('a0000000-0000-0000-0000-000000000004', '+235600000004', '$2y$10$Ly67HRqbsix1/e8/MUcpzO35Y63be65zdMLJHoiTWWMkjb6Sg6.xK', 'tenant-bnft-ndjamena', 'NIVEAU_1', true, 0, now()),
    ('a0000000-0000-0000-0000-000000000005', '+237600000005', '$2y$10$Ly67HRqbsix1/e8/MUcpzO35Y63be65zdMLJHoiTWWMkjb6Sg6.xK', 'tenant-bgft-douala',   'NIVEAU_1', true, 0, now())
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
    ('a0000000-0000-0000-0000-000000000005'::uuid, 'TRANSPORTEUR')
) AS v(acteur_id, role)
WHERE NOT EXISTS (
    SELECT 1 FROM acteur_roles ar WHERE ar.acteur_id = v.acteur_id AND ar.role = v.role
);
