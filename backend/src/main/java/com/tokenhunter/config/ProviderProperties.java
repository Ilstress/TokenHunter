package com.tokenhunter.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Data
@Component
@ConfigurationProperties(prefix = "tokenhunter")
public class ProviderProperties {

    private Map<String, Provider> providers = new HashMap<>();

    @Data
    public static class Provider {
        private String baseUrl;
        private String apiKey;
    }
}
