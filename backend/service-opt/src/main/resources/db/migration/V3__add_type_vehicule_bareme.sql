-- CDC S8.9.1 : "COUT_BASE(axe, distance, type_vehicule)" - le socle
-- forfaitaire doit varier par type de vehicule, pas seulement par axe.
-- type_vehicule NULL = barème générique pour l'axe (repli si aucun barème
-- specifique au type n'existe), meme logique que axe_id NULL = defaut global.
ALTER TABLE opt.bareme_tarification ADD COLUMN type_vehicule VARCHAR(30);

-- Remplace l'ancien index (axe seul) par le composite axe + type_vehicule.
-- COALESCE double sur deux sentinelles distinctes pour ne pas collisionner
-- entre "aucun axe" et "aucun type" dans le meme index.
DROP INDEX IF EXISTS opt.idx_bareme_tarification_actif_par_axe;

CREATE UNIQUE INDEX idx_bareme_tarification_actif_par_axe_type
    ON opt.bareme_tarification (
        COALESCE(axe_id, '00000000-0000-0000-0000-000000000000'),
        COALESCE(type_vehicule, '__DEFAUT__')
    )
    WHERE actif = true;
