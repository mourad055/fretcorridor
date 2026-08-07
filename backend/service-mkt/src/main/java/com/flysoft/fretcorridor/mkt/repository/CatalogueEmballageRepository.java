package com.flysoft.fretcorridor.mkt.repository;

import com.flysoft.fretcorridor.mkt.entity.CatalogueEmballage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface CatalogueEmballageRepository extends JpaRepository<CatalogueEmballage, UUID> {
    List<CatalogueEmballage> findByActifTrueOrderByOrdreAffichageAsc();
}
