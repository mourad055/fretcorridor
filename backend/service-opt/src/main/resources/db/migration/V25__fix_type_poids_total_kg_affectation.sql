-- Corrige le type de poids_total_kg ajoute en V24 : il etait NUMERIC(10,2)
-- pour coller a un BigDecimal, mais l'entite le mappe en Double. Hibernate
-- en mode validate refusait le couple (Double + precision/scale) puis, une
-- fois cette annotation retiree, le mismatch NUMERIC vs double precision.
-- ALTER ici plutot que de modifier V24 (checksum_flyway deja engage).
ALTER TABLE opt.affectation
    ALTER COLUMN poids_total_kg TYPE DOUBLE PRECISION USING poids_total_kg::DOUBLE PRECISION;
