package com.yuzhi.dts.copilot.ai.service.copilot;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

class FinanceApplicationMysqlAuthorityRegistryTest {

    @Test
    void shouldLogApplicationMysqlAuthoritySqlCasesWithoutOracleDatabaseTerminology() {
        Logger logger = (Logger) LoggerFactory.getLogger(FinanceApplicationMysqlAuthorityRegistry.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            FinanceAuthorityRegistry authorityRegistry = new FinanceAuthorityRegistry(new ObjectMapper());
            authorityRegistry.init();
            FinanceApplicationMysqlAuthorityRegistry registry =
                    new FinanceApplicationMysqlAuthorityRegistry(new ObjectMapper(), authorityRegistry);

            registry.init();
        } finally {
            logger.detachAppender(appender);
        }

        assertThat(appender.list)
                .extracting(ILoggingEvent::getFormattedMessage)
                .anySatisfy(message -> assertThat(message)
                        .contains("finance application MySQL authority SQL case(s)")
                        .contains("finance-application-mysql-authority-sql.v1.json")
                        .doesNotContain("application MySQL oracle SQL"));
    }
}
