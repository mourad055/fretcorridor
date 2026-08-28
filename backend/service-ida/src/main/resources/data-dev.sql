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
-- Idempotent. ON CONFLICT (telephone) DO UPDATE du tenant_id : un compte
-- démo créé avant ce seed (souvent via l'inscription chargeur, donc
-- tenant_id = MARKETPLACE_CM) doit être réaligné sur le bureau institutionnel
-- correspondant. Sans ça, le backoffice Bureau Douala affiche MARKETPLACE_CM
-- dans le bandeau. PIN / id / date_creation ne sont pas écrasés.

SET search_path TO service_ida;

INSERT INTO acteurs (id, telephone, code_pin, tenant_id, niveau_kyc, actif, tentatives_echouees, date_creation)
VALUES
    ('a0000000-0000-0000-0000-000000000001', '+237600000001', '$2y$10$Ly67HRqbsix1/e8/MUcpzO35Y63be65zdMLJHoiTWWMkjb6Sg6.xK', 'tenant-bgft-douala',   'NIVEAU_1', true, 0, now()),
    ('a0000000-0000-0000-0000-000000000002', '+237600000002', '$2y$10$Ly67HRqbsix1/e8/MUcpzO35Y63be65zdMLJHoiTWWMkjb6Sg6.xK', 'tenant-bgft-douala',   'NIVEAU_1', true, 0, now()),
    ('a0000000-0000-0000-0000-000000000003', '+237600000003', '$2y$10$Ly67HRqbsix1/e8/MUcpzO35Y63be65zdMLJHoiTWWMkjb6Sg6.xK', 'tenant-flysoft',       'NIVEAU_1', true, 0, now()),
    ('a0000000-0000-0000-0000-000000000004', '+235600000004', '$2y$10$Ly67HRqbsix1/e8/MUcpzO35Y63be65zdMLJHoiTWWMkjb6Sg6.xK', 'tenant-bnft-ndjamena', 'NIVEAU_1', true, 0, now()),
    ('a0000000-0000-0000-0000-000000000005', '+237600000005', '$2y$10$Ly67HRqbsix1/e8/MUcpzO35Y63be65zdMLJHoiTWWMkjb6Sg6.xK', 'tenant-bgft-douala',   'NIVEAU_1', true, 0, now()),
    ('a0000000-0000-0000-0000-000000000006', '+235600000006', '$2y$10$Ly67HRqbsix1/e8/MUcpzO35Y63be65zdMLJHoiTWWMkjb6Sg6.xK', 'tenant-bnft-ndjamena', 'NIVEAU_1', true, 0, now())
ON CONFLICT (telephone) DO UPDATE SET tenant_id = EXCLUDED.tenant_id;

-- roles : table de jointure @ElementCollection (acteur_roles.acteur_id, acteur_roles.role).
-- Pas de ON CONFLICT ici : Hibernate ne pose pas nécessairement de contrainte
-- d'unicité sur cette table de jointure — NOT EXISTS reste correct dans tous les cas.
-- Resolution par telephone (pas par id fixe) : l'id reellement attribue
-- en base peut differer de celui suppose ci-dessus si l'acteur existait deja
-- avant ce script (ON CONFLICT n'insère alors pas l'id fixe a0000000-...) -
-- joindre sur telephone est la seule facon fiable de retrouver le bon acteur_id.
INSERT INTO acteur_roles (acteur_id, role)
SELECT a.id, v.role
FROM (VALUES
    ('+237600000001', 'BUREAU'),
    ('+237600000002', 'TRANSPORTEUR'),
    ('+237600000003', 'ADMINISTRATION'),
    ('+235600000004', 'BUREAU'),
    ('+237600000005', 'TRANSPORTEUR'),
    ('+235600000006', 'TRANSPORTEUR')
) AS v(telephone, role)
JOIN acteurs a ON a.telephone = v.telephone
WHERE NOT EXISTS (
    SELECT 1 FROM acteur_roles ar WHERE ar.acteur_id = a.id AND ar.role = v.role
);

-- Chauffeur mobile (+237696000001) : rôle TRANSPORTEUR en plus de CHAUFFEUR
-- pour que le portail web affiche les mêmes missions/capacités que l'app mobile
-- (même acteurId JWT, PRD §5.3).
UPDATE acteurs SET
    tenant_id = 'tenant-bgft-douala',
    code_pin = '$2y$10$Ly67HRqbsix1/e8/MUcpzO35Y63be65zdMLJHoiTWWMkjb6Sg6.xK',
    nom = COALESCE(NULLIF(nom, ''), 'Kamga'),
    prenom = COALESCE(NULLIF(prenom, ''), 'Jean'),
    raison_sociale = COALESCE(NULLIF(raison_sociale, ''), 'Transport Étoile SARL')
WHERE telephone = '+237696000001';

INSERT INTO acteur_roles (acteur_id, role)
SELECT a.id, v.role
FROM (VALUES
    ('+237696000001', 'CHAUFFEUR'),
    ('+237696000001', 'TRANSPORTEUR')
) AS v(telephone, role)
JOIN acteurs a ON a.telephone = v.telephone
WHERE NOT EXISTS (
    SELECT 1 FROM acteur_roles ar WHERE ar.acteur_id = a.id AND ar.role = v.role
);

-- Libellés des comptes démo web (évite les UUID bruts côté chronologie Bureau).
UPDATE acteurs SET nom = 'Étoile', prenom = 'Demo', raison_sociale = 'Transport Étoile Demo'
WHERE telephone = '+237600000002' AND (nom IS NULL OR nom = '');

UPDATE acteurs SET nom = 'Sahel', prenom = 'Logistique', raison_sociale = 'Fourgon Sahel SARL'
WHERE telephone = '+237600000005' AND (nom IS NULL OR nom = '');

-- FE-ADM-06 : dossier KYC en attente pour l'écran Admin (tenant Flysoft).
INSERT INTO acteurs (id, telephone, code_pin, tenant_id, niveau_kyc, actif, tentatives_echouees, date_creation, nom, prenom, raison_sociale)
VALUES
    ('70000000-0000-0000-0000-000000000001', '+237600000010', '$2y$10$Ly67HRqbsix1/e8/MUcpzO35Y63be65zdMLJHoiTWWMkjb6Sg6.xK', 'tenant-flysoft', 'NIVEAU_1', true, 0, now(), 'Mballa', 'Aïcha', NULL)
ON CONFLICT (telephone) DO UPDATE SET tenant_id = EXCLUDED.tenant_id;

INSERT INTO acteur_roles (acteur_id, role)
SELECT a.id, 'TRANSPORTEUR'
FROM acteurs a
WHERE a.telephone = '+237600000010'
  AND NOT EXISTS (
    SELECT 1 FROM acteur_roles ar WHERE ar.acteur_id = a.id AND ar.role = 'TRANSPORTEUR'
);

INSERT INTO pieces_justificatives (id, acteur_id, type_document, object_key, date_depot)
SELECT '80000000-0000-0000-0000-000000000001'::uuid, a.id, 'CNI', 'demo/kyc/en-attente-cni.pdf', now()
FROM acteurs a
WHERE a.telephone = '+237600000010'
  AND NOT EXISTS (
    SELECT 1 FROM pieces_justificatives p WHERE p.acteur_id = a.id AND p.type_document = 'CNI'
);
