package com.fretcorridor.opt.client;

import java.util.List;
import java.util.UUID;

// Miroir du contrat PositionsBatchRequest cote service-trk.
public record PositionsBatchRequestDto(List<UUID> vehiculeIds) {
}
