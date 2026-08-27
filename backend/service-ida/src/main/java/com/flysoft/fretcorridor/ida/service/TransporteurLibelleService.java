package com.flysoft.fretcorridor.ida.service;

import com.flysoft.fretcorridor.ida.dto.TransporteurLibelleDto;
import com.flysoft.fretcorridor.ida.repository.ActeurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransporteurLibelleService {

    private final ActeurRepository acteurRepository;

    @Transactional(readOnly = true)
    public List<TransporteurLibelleDto> libellesPourTenant(String tenantId, List<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return acteurRepository.findAllById(ids).stream()
                .filter(acteur -> tenantId.equals(acteur.getTenantId()))
                .map(TransporteurLibelleDto::from)
                .toList();
    }
}
