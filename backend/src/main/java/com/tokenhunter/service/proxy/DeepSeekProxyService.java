package com.tokenhunter.service.proxy;

import com.tokenhunter.config.ProviderProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class DeepSeekProxyService {

    private static final Logger log = LoggerFactory.getLogger(DeepSeekProxyService.class);

    private final RestTemplate restTemplate;
    private final ProviderProperties providerProperties;

    public DeepSeekProxyService(RestTemplate restTemplate, ProviderProperties providerProperties) {
        this.restTemplate = restTemplate;
        this.providerProperties = providerProperties;
    }

    public ResponseEntity<String> forward(String path, String body, HttpHeaders headers) {
        Map<String, ProviderProperties.Provider> providers = providerProperties.getProviders();
        ProviderProperties.Provider deepseek = providers.get("deepseek");
        String baseUrl = deepseek.getBaseUrl();
        String apiKey = deepseek.getApiKey();

        String url = baseUrl + path;
        log.info("Proxy forward: {} -> {}", path, url);

        HttpHeaders proxyHeaders = new HttpHeaders();
        headers.forEach((key, values) -> {
            if ("authorization".equalsIgnoreCase(key) || "content-type".equalsIgnoreCase(key) || "accept".equalsIgnoreCase(key)) {
                proxyHeaders.put(key, values);
            }
        });
        proxyHeaders.setBearerAuth(apiKey);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    new HttpEntity<>(body, proxyHeaders),
                    String.class
            );
            HttpHeaders safeHeaders = new HttpHeaders();
            String contentType = response.getHeaders().getFirst("Content-Type");
            if (contentType != null) {
                safeHeaders.set("Content-Type", contentType);
            }
            return ResponseEntity.status(response.getStatusCode())
                    .headers(safeHeaders)
                    .body(response.getBody());
        } catch (HttpStatusCodeException e) {
            log.warn("Proxy upstream error: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            return ResponseEntity.status(e.getStatusCode())
                    .header("Content-Type", "application/json")
                    .body(e.getResponseBodyAsString());
        }
    }
}
