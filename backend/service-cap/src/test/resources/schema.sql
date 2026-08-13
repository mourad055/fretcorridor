-- Miroir H2 de V1__init_schema_and_capacite.sql (table cap.decrement_log
-- uniquement) - Flyway est desactive en test (profil H2), et cette table
-- n'est pas une entite JPA donc ddl-auto=create-drop ne la genere jamais.
-- Execute APRES la generation Hibernate (spring.jpa.defer-datasource-
-- initialization=true) pour que cap.capacite existe deja pour la FK.
CREATE TABLE IF NOT EXISTS cap.decrement_log (
    id                  UUID DEFAULT RANDOM_UUID() PRIMARY KEY,
    capacite_id         UUID NOT NULL REFERENCES cap.capacite(id),
    cle_idempotence     VARCHAR(100) NOT NULL,
    montant_kg          NUMERIC(10,2) NOT NULL,
    date_creation       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_decrement_idempotence UNIQUE (capacite_id, cle_idempotence)
);
