package com.fretcorridor.geo.domain;

import com.uber.h3core.H3Core;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ZonageH3ServiceTest {

    @Mock
    private ConfigurationH3Repository configurationH3Repository;

    @Mock
    private H3Core h3Core;

    @InjectMocks
    private ZonageH3Service zonageH3Service;

    @BeforeEach
    void setUp() throws IOException {
        ReflectionTestUtils.setField(zonageH3Service, "h3Core", h3Core);
    }

    @Test
    void shouldReturnDefaultResolutionWhenNoConfig() {
        when(configurationH3Repository.findByCle("resolution_defaut")).thenReturn(Optional.empty());
        int resolution = zonageH3Service.resolutionActuelle();
        assertThat(resolution).isEqualTo(7);
    }

    @Test
    void shouldReturnConfiguredResolutionWhenPresent() {
        ConfigurationH3 mockConfig = org.mockito.Mockito.mock(ConfigurationH3.class);
        when(mockConfig.getValeur()).thenReturn("8");
        when(configurationH3Repository.findByCle("resolution_defaut")).thenReturn(Optional.of(mockConfig));

        int resolution = zonageH3Service.resolutionActuelle();
        assertThat(resolution).isEqualTo(8);
    }

    @Test
    void shouldComputeH3Index() throws Exception {
        when(h3Core.latLngToCellAddress(4.0511, 9.7679, 7)).thenReturn("8928308280fffff");
        String index = zonageH3Service.indexPourPoint(4.0511, 9.7679);
        assertThat(index).isEqualTo("8928308280fffff");
    }

    @Test
    void shouldComputeKring() throws Exception {
        when(h3Core.gridDisk("8928308280fffff", 1)).thenReturn(List.of("a", "b", "c"));
        List<String> result = zonageH3Service.kRing("8928308280fffff", 1);
        assertThat(result).containsExactly("a", "b", "c");
    }
}
