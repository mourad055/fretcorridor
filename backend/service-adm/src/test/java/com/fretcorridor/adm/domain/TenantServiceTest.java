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
}
