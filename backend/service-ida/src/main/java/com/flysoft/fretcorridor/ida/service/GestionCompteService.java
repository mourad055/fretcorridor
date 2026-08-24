package com.flysoft.fretcorridor.ida.service;

import com.flysoft.fretcorridor.ida.entity.Acteur;
import com.flysoft.fretcorridor.ida.entity.RoleActeur;
import com.flysoft.fretcorridor.ida.repository.ActeurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Gestion des comptes par un Admin (audit UX 2026-08-23,
 * docs/AUDIT_ROADMAP_Backoffice_Web_2026-08-23.md §1.1) : créer/désactiver/
 * réactiver un compte, changer son rôle. Jusqu'ici aucune interface
 * n'exposait le cycle de vie d'un Acteur au-delà de son propre profil.
 *
 * Périmètre volontairement restreint : la réinitialisation du moyen
 * d'authentification (PIN) exigerait un flux OTP/SMS dédié — hors scope
 * ici, réservée à une conception à part (roadmap §3, "hors périmètre").
 */
@Service
@RequiredArgsConstructor
public class GestionCompteService {

    private final ActeurRepository acteurRepository;

    public List<Acteur> listerParTenant(String tenantId) {
        return acteurRepository.findByTenantId(tenantId);
    }

    @Transactional
    public Acteur changerStatut(UUID acteurId, String tenantId, boolean actif) {
        Acteur acteur = acteurDuTenant(acteurId, tenantId);
        acteur.setActif(actif);
        return acteurRepository.save(acteur);
    }

    @Transactional
    public Acteur changerRoles(UUID acteurId, String tenantId, Set<RoleActeur> roles) {
        Acteur acteur = acteurDuTenant(acteurId, tenantId);
        acteur.setRoles(roles);
        return acteurRepository.save(acteur);
    }

    // Même principe IDOR que le reste du dépôt (DossierController/service-adm,
    // CapaciteController/service-cap) : un compte hors du tenant de l'Admin
    // appelant est traité comme introuvable, jamais comme "existe mais refusé".
    private Acteur acteurDuTenant(UUID acteurId, String tenantId) {
        Acteur acteur = acteurRepository.findById(acteurId)
                .orElseThrow(() -> new RuntimeException("COMPTE_INTROUVABLE"));
        if (!acteur.getTenantId().equals(tenantId)) {
            throw new RuntimeException("COMPTE_INTROUVABLE");
        }
        return acteur;
    }
}
