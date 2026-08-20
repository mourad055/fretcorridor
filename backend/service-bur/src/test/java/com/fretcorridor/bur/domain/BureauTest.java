package com.fretcorridor.bur.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BureauTest {

    @Test
    void creates_a_bureau_with_valid_fields() {
        Bureau bureau = new Bureau("bur-1", "tenant-bgft-douala", "Bureau de Douala");

        assertThat(bureau.id()).isEqualTo("bur-1");
        assertThat(bureau.tenantId()).isEqualTo("tenant-bgft-douala");
    }

    @Test
    void rejects_a_bureau_without_a_tenant() {
        assertThatThrownBy(() -> new Bureau("bur-1", " ", "Bureau de Douala"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tenant");
    }

    @Test
    void rejects_a_bureau_without_a_name() {
        assertThatThrownBy(() -> new Bureau("bur-1", "tenant-bgft-douala", ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nom");
    }
}
