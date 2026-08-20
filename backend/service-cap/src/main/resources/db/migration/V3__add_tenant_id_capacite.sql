-- Corrige le bloquant "0 authentification + IDOR" de l'audit CDC du 19 aout
-- (CapaciteController.java:27,32,42, aucune dependance spring-security dans
-- le pom.xml) : service-cap n'avait jusqu'ici aucune notion de tenant dans
-- son domaine, malgre le principe d'isolation stricte ENF-MUL-01 applique
-- partout ailleurs dans le depot.
--
-- Nullable, meme principe que les colonnes tenant_id ajoutees ailleurs
-- (ex. service-geo V4) : les capacites deja declarees avant ce correctif
-- n'ont aucun tenant a leur attribuer retroactivement, contrairement a un
-- backfill deductible (ex. Hub.pays). Toute nouvelle declaration (JWT
-- desormais obligatoire, cf CapaciteController) renseigne systematiquement
-- cette colonne - contrainte applicative, pas NOT NULL en base.
ALTER TABLE cap.capacite
    ADD COLUMN tenant_id VARCHAR(100);
