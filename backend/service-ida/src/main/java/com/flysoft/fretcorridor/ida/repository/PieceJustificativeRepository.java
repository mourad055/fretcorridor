package com.flysoft.fretcorridor.ida.repository;

import com.flysoft.fretcorridor.ida.entity.PieceJustificative;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface PieceJustificativeRepository extends JpaRepository<PieceJustificative, UUID> {
    List<PieceJustificative> findByActeurId(UUID acteurId);
}
