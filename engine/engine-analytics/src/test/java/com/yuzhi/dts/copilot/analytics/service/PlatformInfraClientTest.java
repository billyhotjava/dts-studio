package com.yuzhi.dts.copilot.analytics.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PlatformInfraClientTest {

    @Mock
    private CopilotAiClient copilotAiClient;

    @Mock
    private LocalAiDataSourceLookup localAiDataSourceLookup;

    @Test
    void fetchDataSourceDetailShouldFallBackToLocalLookupWhenAiHttpReturnsEmpty() {
        when(copilotAiClient.getDataSource(15L, "")).thenReturn(Optional.empty());
        when(localAiDataSourceLookup.findById(15L))
                .thenReturn(Optional.of(new PlatformInfraClient.DataSourceDetail(
                        "15",
                        "园林业务库",
                        "mysql",
                        "jdbc:mysql://db.weitaor.com:3306/rs_cloud_flower",
                        "flowerai",
                        "业务库",
                        null,
                        Map.of(),
                        Map.of("password", "ai2026Flower@demo"),
                        "ACTIVE",
                        null)));

        PlatformInfraClient client = new PlatformInfraClient(copilotAiClient, new ObjectMapper(), localAiDataSourceLookup);

        PlatformInfraClient.DataSourceDetail detail = client.fetchDataSourceDetail(15L);

        assertThat(detail.id()).isEqualTo("15");
        assertThat(detail.name()).isEqualTo("园林业务库");
        assertThat(detail.jdbcUrl()).isEqualTo("jdbc:mysql://db.weitaor.com:3306/rs_cloud_flower");
        assertThat(detail.username()).isEqualTo("flowerai");
        assertThat(detail.secrets()).containsEntry("password", "ai2026Flower@demo");
    }
}
