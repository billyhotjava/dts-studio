package com.yuzhi.dts.copilot.ai.service.copilot;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class SemanticPackServiceTest {

    @Test
    void shouldLoadProcurementSemanticPackContext() {
        SemanticPackService service = new SemanticPackService(new ObjectMapper());

        service.init();

        assertThat(service.getDomains()).contains("procurement");
        assertThat(service.getContextForDomain("procurement"))
                .contains("采购明细")
                .contains("t_purchase_price_item")
                .contains("good_name")
                .contains("采购人");
        assertThat(service.getSynonyms("procurement"))
                .containsEntry("采购人", "purchase_user_name")
                .containsEntry("产品", "good_name");
    }
}
