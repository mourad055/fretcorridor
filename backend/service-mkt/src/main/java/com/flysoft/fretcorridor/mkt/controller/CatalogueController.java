package com.flysoft.fretcorridor.mkt.controller;

import com.flysoft.fretcorridor.mkt.dto.CatalogueDto;
import com.flysoft.fretcorridor.mkt.service.CatalogueService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/catalogue-emballages")
@RequiredArgsConstructor
public class CatalogueController {

    private final CatalogueService catalogueService;

    // RG-038 : consultation sans authentification
    @GetMapping
    public ResponseEntity<List<CatalogueDto.EmballageResponse>> getCatalogue() {
        return ResponseEntity.ok(catalogueService.getCatalogue());
    }
}
