package com.flysoft.fretcorridor.ida.service;

import com.flysoft.fretcorridor.ida.dto.AuthDto;
import com.flysoft.fretcorridor.ida.entity.Acteur;
import com.flysoft.fretcorridor.ida.entity.AffiliationTenant;
import com.flysoft.fretcorridor.ida.entity.RoleActeur;
import com.flysoft.fretcorridor.ida.repository.AffiliationTenantRepository;
import com.flysoft.fretcorridor.ida.repository.ActeurRepository;
import com.flysoft.fretcorridor.ida.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * S18 (Sprint 18, "Second tenant institutionnel", audit de suivi 23 aout) :
 * "Sélection de tenant au login (si multi-bureau)" — Plan d'exécution.
 * Règle produit choisie explicitement par l'utilisatrice : c'est le SECOND
 * BUREAU (jamais le transporteur) qui invite/valide l'affiliation - il n'y a
 * donc aucun flux de demande/acceptation côté transporteur, l'invitation
 * EST la validation.
 */
@Service
@RequiredArgsConstructor
public class AffiliationService {

    private final ActeurRepository acteurRepository;
    private final AffiliationTenantRepository affiliationTenantRepository;
    private final JwtService jwtService;

    /**
     * Un opérateur du bureau invitant (role BUREAU, tenantId pris de son
     * propre JWT — jamais du corps de requête, même principe que partout
     * ailleurs dans ce dépôt) rattache un transporteur/chauffeur existant à
     * son tenant. Idempotent (contrainte unique acteur+tenant) : réinviter
     * un acteur déjà affilié ne duplique rien.
     */
    @Transactional
    public void inviter(String tenantIdBureau, String telephoneTransporteur) {
        Acteur acteur = acteurRepository.findByTelephone(telephoneTransporteur)
                .orElseThrow(() -> new RuntimeException("ACTEUR_INTROUVABLE"));

        boolean estTransporteur = acteur.getRoles().contains(RoleActeur.CHAUFFEUR)
                || acteur.getRoles().contains(RoleActeur.TRANSPORTEUR)
                || acteur.getRoles().contains(RoleActeur.CHAUFFEUR_PROPRIETAIRE);
        if (!estTransporteur) {
            throw new RuntimeException("ROLE_NON_AFFILIABLE");
        }

        if (tenantIdBureau.equals(acteur.getTenantId())
                || affiliationTenantRepository.existsByActeurIdAndTenantId(acteur.getId(), tenantIdBureau)) {
            return; // déjà rattaché (tenant d'origine ou affiliation existante) - idempotent
        }

        affiliationTenantRepository.save(AffiliationTenant.builder()
                .acteurId(acteur.getId())
                .tenantId(tenantIdBureau)
                .build());
    }

    /** Tenant d'origine (toujours présent, en premier) + toutes les affiliations accordées. */
    @Transactional(readOnly = true)
    public List<AuthDto.TenantDisponible> mesTenants(UUID acteurId) {
        Acteur acteur = acteurRepository.findById(acteurId)
                .orElseThrow(() -> new RuntimeException("ACTEUR_INTROUVABLE"));

        List<AuthDto.TenantDisponible> resultat = new java.util.ArrayList<>();
        resultat.add(new AuthDto.TenantDisponible(acteur.getTenantId(), true));
        affiliationTenantRepository.findByActeurId(acteurId)
                .forEach(a -> resultat.add(new AuthDto.TenantDisponible(a.getTenantId(), false)));
        return resultat;
    }

    /**
     * Réémet un token scopé au tenant choisi, après vérification que
     * l'acteur y est bien affilié (tenant d'origine ou affiliation
     * accordée) — jamais un tenant arbitraire fourni par le client.
     */
    @Transactional(readOnly = true)
    public AuthDto.AuthResponse selectionner(UUID acteurId, String tenantIdChoisi) {
        Acteur acteur = acteurRepository.findById(acteurId)
                .orElseThrow(() -> new RuntimeException("ACTEUR_INTROUVABLE"));

        boolean autorise = tenantIdChoisi.equals(acteur.getTenantId())
                || affiliationTenantRepository.existsByActeurIdAndTenantId(acteurId, tenantIdChoisi);
        if (!autorise) {
            throw new RuntimeException("TENANT_NON_AFFILIE");
        }

        String access = jwtService.genererAccessToken(acteur, tenantIdChoisi);
        String refresh = jwtService.genererRefreshToken(acteur);
        return AuthDto.AuthResponse.of(access, refresh, acteur, tenantIdChoisi);
    }
}
