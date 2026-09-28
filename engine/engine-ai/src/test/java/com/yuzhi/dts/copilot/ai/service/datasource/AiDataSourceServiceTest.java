package com.yuzhi.dts.copilot.ai.service.datasource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.yuzhi.dts.copilot.ai.domain.AiDataSource;
import com.yuzhi.dts.copilot.ai.repository.AiDataSourceRepository;
import com.yuzhi.dts.copilot.ai.web.rest.dto.AiDataSourceUpsertRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AiDataSourceServiceTest {

    @Mock
    private AiDataSourceRepository repository;

    @InjectMocks
    private AiDataSourceService service;

    @Test
    void createDataSourceBuildsJdbcUrlFromStructuredPostgresFields() {
        when(repository.save(any(AiDataSource.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AiDataSource created = service.createDataSource(new AiDataSourceUpsertRequest(
                "园林业务库",
                "postgres",
                null,
                "db.internal",
                5432,
                "garden",
                null,
                null,
                "readonly",
                "secret-pass",
                "用于 NL2SQL"));

        assertThat(created.getName()).isEqualTo("园林业务库");
        assertThat(created.getDbType()).isEqualTo("postgres");
        assertThat(created.getJdbcUrl()).isEqualTo("jdbc:postgresql://db.internal:5432/garden");
        assertThat(created.getUsername()).isEqualTo("readonly");
        assertThat(created.getPassword()).isEqualTo("secret-pass");
        assertThat(created.getStatus()).isEqualTo("ACTIVE");
    }
}
