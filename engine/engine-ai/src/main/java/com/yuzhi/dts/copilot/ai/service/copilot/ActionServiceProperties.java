package com.yuzhi.dts.copilot.ai.service.copilot;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "dts.studio.action")
public class ActionServiceProperties {
    private Map<String, Binding> services = Map.of();

    public Map<String, Binding> getServices() { return services; }
    public void setServices(Map<String, Binding> services) {
        this.services = services == null ? Map.of() : Map.copyOf(services);
    }

    /** Credentials are deployment configuration and never Pack assets. */
    public record Binding(String baseUrl, String authMode, String credential) {
        @Override public String toString() { return "Binding[baseUrl=" + baseUrl + ", authMode=" + authMode + ", credential=<redacted>]"; }
    }
}
