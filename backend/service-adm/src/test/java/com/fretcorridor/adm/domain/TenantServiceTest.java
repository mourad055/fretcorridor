package com.fretcorridor.adm.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TenantServiceTest {

    private final InMemoryTenantPort tenantPort = new InMemoryTenantPort();
    private final InMemoryJournalAuditPort journalAuditPort = new InMemoryJournalAuditPort();
    private final TenantService service = new TenantService(tenantPort, journalAuditPort);

    @Test
    void creer_un_tenant_le_rend_listable_et_journalise_la_creation() {
        service.creer("tenant-bgft-douala", "Bureau de fret Douala", "Cameroun", "actor-admin-1");

        assertThat(service.lister()).extracting(Tenant::id).containsExactly("tenant-bgft-douala");
        assertThat(journalAuditPort.lister("tenant-bgft-douala")).anyMatch(e -> e.action().equals("TENANT_CREE"));
    }

    @Test
    void creer_un_tenant_avec_un_identifiant_deja_pris_est_interdit() {
        service.creer("tenant-bgft-douala", "Bureau de fret Douala", "Cameroun", "actor-admin-1");

        assertThatThrownBy(() -> service.creer("tenant-bgft-douala", "Doublon", "Cameroun", "actor-admin-1"))
                .isInstanceOf(TenantDejaExistantException.class);
    }

    @Test
    void un_tenant_cree_est_actif_par_defaut() {
        service.creer("tenant-bgft-douala", "Bureau de fret Douala", "Cameroun", "actor-admin-1");

        assertThat(service.lister()).extracting(Tenant::actif).containsExactly(true);
    }

    @Test
    void modifier_un_tenant_met_a_jour_nom_pays_statut_et_journalise() {
        service.creer("tenant-bgft-douala", "Bureau de fret Douala", "Cameroun", "actor-admin-1");

        Tenant modifie = service.modifier("tenant-bgft-douala", "Bureau de fret Douala (renommé)", "Cameroun", false, "actor-admin-2");

        assertThat(modifie.nom()).isEqualTo("Bureau de fret Douala (renommé)");
        assertThat(modifie.actif()).isFalse();
        assertThat(journalAuditPort.lister("tenant-bgft-douala")).anyMatch(e -> e.action().equals("TENANT_MODIFIE"));
    }

    @Test
    void modifier_un_tenant_inexistant_est_refuse() {
        assertThatThrownBy(() -> service.modifier("tenant-inconnu", "X", "Y", true, "actor-admin-1"))
                .isInstanceOf(TenantIntrouvableException.class);
    }
}
