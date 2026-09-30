package com.yuzhi.dts.copilot.ai.service.pack;

import com.yuzhi.dts.copilot.ai.domain.ApiKey;
import com.yuzhi.dts.copilot.ai.security.ApiKeyAuthentication;
import com.yuzhi.dts.copilot.ai.web.rest.PackResource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PackResourceTest {
    private final PackRegistryService registry=mock(PackRegistryService.class);
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }

    @Test void callerRolesAndSecretAloneDoNotGrantAdministration() throws Exception {
        var mvc=MockMvcBuilders.standaloneSetup(new PackResource(registry,new PackArchiveValidator(),"test-secret")).build();
        mvc.perform(get("/api/ai/packs").header("X-Admin-Secret","test-secret").header("X-DTS-Roles","STUDIO_ADMIN"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("PACK_FORBIDDEN"));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("admin","",List.of()));
        mvc.perform(get("/api/ai/packs").header("X-Admin-Secret","test-secret")).andExpect(status().isForbidden());
        verifyNoInteractions(registry);
    }

    @Test void configuredSecretAndAuthenticatedApiKeyAuthorizeAndRecordServerActor() throws Exception {
        ApiKey key=new ApiKey(); key.setId(42L); key.setName("test");
        SecurityContextHolder.getContext().setAuthentication(new ApiKeyAuthentication(key,null));
        var mvc=MockMvcBuilders.standaloneSetup(new PackResource(registry,new PackArchiveValidator(),"test-secret")).build();
        mvc.perform(get("/api/ai/packs").header("X-Admin-Secret","wrong")).andExpect(status().isForbidden());
        mvc.perform(post("/api/ai/packs/fixture/versions/1.0.0/activate").header("X-Admin-Secret","test-secret")
                        .header("X-DTS-UserId","forged"))
                .andExpect(status().isOk());
        verify(registry).activate("fixture","1.0.0","api-key:42");
        var unconfigured=MockMvcBuilders.standaloneSetup(new PackResource(registry,new PackArchiveValidator(),"change-me-in-production")).build();
        unconfigured.perform(get("/api/ai/packs").header("X-Admin-Secret","change-me-in-production")).andExpect(status().isForbidden());
    }
}
