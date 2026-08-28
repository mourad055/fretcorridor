-- V9 : Corrige les tenant_id des axes pour matcher le seed data-dev.sql.
-- Probleme reproductible : lors d'un rebuild, V8 etape les axes a
-- tenant-bgft-douala/tenant-bnft-ndjamena, mais data-dev.sql peut les
-- ecraser a MARKETPLACE_CM si le search_path ou l'ordre d'execution
-- n'est pas garanti. Cette migration idempotente verrouille la verite
-- source (data-dev.sql) dans Flyway.

SET search_path TO geo, public;

-- Axes Cameroun (tenant principal Phase 1 - BGFT)
UPDATE axe SET tenant_id = 'tenant-bgft-douala' WHERE id IN (
    '20000000-0000-0000-0000-000000000001', -- Douala - Yaounde
    '20000000-0000-0000-0000-000000000002', -- Douala - Bafoussam
    '20000000-0000-0000-0000-000000000003'  -- Yaounde - N'Djamena
);

-- Axes Tchad (BNFT)
UPDATE axe SET tenant_id = 'tenant-bnft-ndjamena' WHERE id IN (
    '20000000-0000-0000-0000-000000000004', -- N'Djamena - Garoua
    '20000000-0000-0000-0000-000000000005'  -- Garoua - N'Djamena
);
