package com.flysoft.fretcorridor.exe.repository;

import com.flysoft.fretcorridor.exe.entity.PreuveEtape;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

/** RG-072 : aucune méthode de suppression ou de mise à jour n'est ajoutée par construction. */
@Repository
public interface PreuveEtapeRepository extends JpaRepository<PreuveEtape, UUID> {
}
