package com.fretcorridor.adm.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** FE-ADM-04 : gestion des tenants (bureaux de fret, entités multi-tenant). */
public class TenantService {

    private final TenantPort tenantPort;
    private final JournalAuditPort journalAuditPort;

    public TenantService(TenantPort tenantPort, JournalAuditPort journalAuditPort) {
        this.tenantPort = tenantPort;
        this.journalAuditPort = journalAuditPort;
    }

    public Tenant creer(String id, String nom, String pays, String auteur) {
        if (tenantPort.parId(id).isPresent()) {
            throw new TenantDejaExistantException(id);
        }
        Tenant tenant = new Tenant(id, nom, pays, true);
        tenantPort.sauvegarder(tenant);
        journalAuditPort.enregistrer(new EntreeJournalAudit(UUID.randomUUID().toString(), id, auteur,
                "TENANT_CREE", "tenant:" + id, Instant.now()));
        return tenant;
    }

    /** FE-ADM-04 (audit UX 2026-08-23) : édition nom/pays et statut actif/inactif d'un tenant existant. */
    public Tenant modifier(String id, String nom, String pays, boolean actif, String auteur) {
        tenantPort.parId(id).orElseThrow(() -> new TenantIntrouvableException(id));
        Tenant tenant = new Tenant(id, nom, pays, actif);
        tenantPort.sauvegarder(tenant);
        journalAuditPort.enregistrer(new EntreeJournalAudit(UUID.randomUUID().toString(), id, auteur,
                "TENANT_MODIFIE", "tenant:" + id, Instant.now()));
        return tenant;
    }

    public List<Tenant> lister() {
        return tenantPort.lister();
    }
}
