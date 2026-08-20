package com.flysoft.fretcorridor.cap.client;

import java.util.Map;
import java.util.UUID;

/**
 * Miroir partiel de AxeResponse cote service-geo - seuls les champs utiles a
 * la resolution des coefficients de poids taxable (RG-101) sont repris ici
 * (pas de code partage entre modules, meme principe que
 * service-mkt/client/AxeDto).
 */
public record AxeDto(UUID id, Map<String, Object> parametres) {
}
