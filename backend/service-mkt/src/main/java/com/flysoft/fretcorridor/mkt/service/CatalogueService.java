package com.flysoft.fretcorridor.mkt.service;

import com.flysoft.fretcorridor.mkt.dto.CatalogueDto;
import com.flysoft.fretcorridor.mkt.repository.CatalogueEmballageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CatalogueService {

    private final CatalogueEmballageRepository repository;

    public List<CatalogueDto.EmballageResponse> getCatalogue() {
        return repository.findByActifTrueOrderByOrdreAffichageAsc().stream()
                .map(CatalogueDto.EmballageResponse::fromEntity)
                .toList();
    }
}
