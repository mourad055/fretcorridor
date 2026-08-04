package com.flysoft.fretcorridor.ida.repository;

import com.flysoft.fretcorridor.ida.entity.Acteur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ActeurRepository extends JpaRepository<Acteur, UUID> {
    Optional<Acteur> findByTelephone(String telephone);
    boolean existsByTelephone(String telephone);
}
