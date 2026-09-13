package com.tokenhunter.service.proxy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tokenhunter.config.ProviderProperties;
import com.tokenhunter.entity.UsageRecord;
import com.tokenhunter.service.UsageRecordService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class DeepSeekProxyService {

    private static final Logger log = LoggerFactory.getLogger(DeepSeekProxyService.class);

    private final RestTemplate restTemplate;
    private final ProviderProperties providerProperties;
    private final ObjectMapper objectMapper;
    private final UsageRecordService usageRecordService;

    public DeepSeekProxyService(RestTemplate restTemplate, ProviderProperties providerProperties, ObjectMapper objectMapper, UsageRecordService usageRecordService) {
        this.restTemplate = restTemplate;
        this.providerProperties = providerProperties;
        this.objectMapper = objectMapper;
        this.usageRecordService = usageRecordService;
    }

    // parseUsage removed: usage parsing moved inline in forward() (Task 2.5 → 2.6 consolidation).
    // Method was dead code after Task 2.6 — forward() now parses and assembles UsageRecord directly.

    private String extractModel(String requestBody, String responseBody) {
        try {
            JsonNode reqNode = objectMapper.readTree(requestBody);
            String model = reqNode.path("model").asText(null);
            if (model != null && !model.isBlank()) {
                return model;
            }
        } catch (Exception ignored) {
            // fall through to response body
        }
        try {
            if (responseBody != null) {
                JsonNode resNode = objectMapper.readTree(responseBody);
                String model = resNode.path("model").asText(null);
                if (model != null && !model.isBlank()) {
                    return model;
                }
            }
        } catch (Exception ignored) {
            // fall through
        }
        return "unknown";
    }

    private UsageRecord assembleRecord(String requestBody, String responseBody, int httpStatus,
                                        int input, int output, int total, int cacheHit, int cacheMiss,
                                        long durationMs, String path) {
        UsageRecord record = new UsageRecord();
        record.setProvider("deepseek");
        record.setModel(extractModel(requestBody, responseBody));
        record.setInputTokens(input);
        record.setCacheHitTokens(cacheHit);
        record.setCacheMissTokens(cacheMiss);
        record.setOutputTokens(output);
        record.setTotalTokens(total);
        record.setDurationMs((int) durationMs);
        record.setStatus(0);
        record.setHttpStatus(httpStatus);
        record.setErrorMessage(null);
        record.setEndpoint(path);
        record.setStream(0);
        record.setCreatedAt(null);
        return record;
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

        long startTime = System.nanoTime();
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    new HttpEntity<>(body, proxyHeaders),
                    String.class
            );
            long endTime = System.nanoTime();
            long durationMs = (endTime - startTime) / 1_000_000;

            HttpHeaders safeHeaders = new HttpHeaders();
            String contentType = response.getHeaders().getFirst("Content-Type");
            if (contentType != null) {
                safeHeaders.set("Content-Type", contentType);
            }
            int input = 0, output = 0, total = 0, cacheHit = 0, cacheMiss = 0;
            try {
                JsonNode node = objectMapper.readTree(response.getBody());
                JsonNode usage = node.path("usage");
                input = usage.path("prompt_tokens").asInt(0);
                output = usage.path("completion_tokens").asInt(0);
                total = usage.path("total_tokens").asInt(0);
                cacheHit = usage.path("prompt_cache_hit_tokens").asInt(0);
                cacheMiss = usage.path("prompt_cache_miss_tokens").asInt(0);
                log.info("Parsed usage: input={}, output={}, total={}, cacheHit={}, cacheMiss={}",
                        input, output, total, cacheHit, cacheMiss);
            } catch (Exception e) {
                log.warn("Failed to parse usage from response: {}", e.getMessage());
            }
            UsageRecord record = assembleRecord(body, response.getBody(),
                    response.getStatusCode().value(),
                    input, output, total, cacheHit, cacheMiss, durationMs, path);
            log.info("UsageRecord assembled: provider={}, model={}, input={}, output={}, total={}, cacheHit={}, cacheMiss={}, durationMs={}, status={}, httpStatus={}",
                    record.getProvider(), record.getModel(), record.getInputTokens(), record.getOutputTokens(),
                    record.getTotalTokens(), record.getCacheHitTokens(), record.getCacheMissTokens(),
                    record.getDurationMs(), record.getStatus(), record.getHttpStatus());
            try {
                usageRecordService.save(record);
                log.info("UsageRecord saved: id={}", record.getId());
            } catch (Exception e) {
                log.warn("Failed to save UsageRecord: {}", e.getMessage());
            }
            return ResponseEntity.status(response.getStatusCode())
                    .headers(safeHeaders)
                    .body(response.getBody());
        } catch (HttpStatusCodeException e) {
            long endTime = System.nanoTime();
            long durationMs = (endTime - startTime) / 1_000_000;
            log.warn("Proxy upstream error: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            String errorBody = e.getResponseBodyAsString();
            String errorMsg = (errorBody != null && !errorBody.isBlank()) ? errorBody : e.getClass().getSimpleName();
            if (errorMsg.length() > 512) {
                errorMsg = errorMsg.substring(0, 512);
            }
            UsageRecord record = new UsageRecord();
            record.setProvider("deepseek");
            record.setModel(extractModel(body, null));
            record.setInputTokens(0);
            record.setCacheHitTokens(0);
            record.setCacheMissTokens(0);
            record.setOutputTokens(0);
            record.setTotalTokens(0);
            record.setDurationMs((int) durationMs);
            record.setStatus(1);
            record.setHttpStatus(e.getStatusCode().value());
            record.setErrorMessage(errorMsg);
            record.setEndpoint(path);
            record.setStream(0);
            record.setCreatedAt(null);
            try {
                usageRecordService.save(record);
                log.warn("Upstream error: status={}, saved failure record id={}", e.getStatusCode().value(), record.getId());
            } catch (Exception saveEx) {
                log.warn("Failed to save failure record: {}", saveEx.getMessage());
            }
            return ResponseEntity.status(e.getStatusCode())
                    .header("Content-Type", "application/json")
                    .body(e.getResponseBodyAsString());
        } catch (ResourceAccessException e) {
            long endTime = System.nanoTime();
            long durationMs = (endTime - startTime) / 1_000_000;
            Throwable cause = (e.getCause() != null) ? e.getCause() : e;
            String errorMsg = cause.getClass().getSimpleName() + ": " + cause.getMessage();
            if (errorMsg.length() > 512) {
                errorMsg = errorMsg.substring(0, 512);
            }
            log.warn("Proxy network error: {}", errorMsg);
            UsageRecord record = new UsageRecord();
            record.setProvider("deepseek");
            record.setModel(extractModel(body, null));
            record.setInputTokens(0);
            record.setCacheHitTokens(0);
            record.setCacheMissTokens(0);
            record.setOutputTokens(0);
            record.setTotalTokens(0);
            record.setDurationMs((int) durationMs);
            record.setStatus(1);
            record.setHttpStatus(null);
            record.setErrorMessage(errorMsg);
            record.setEndpoint(path);
            record.setStream(0);
            record.setCreatedAt(null);
            try {
                usageRecordService.save(record);
                log.warn("Network error: {}, saved failure record id={}", errorMsg, record.getId());
            } catch (Exception saveEx) {
                log.warn("Failed to save failure record: {}", saveEx.getMessage());
            }
            String errorJson = "{\"error\":\"upstream_unreachable\",\"message\":\"" + errorMsg.replace("\"", "\\\"") + "\"}";
            return ResponseEntity.status(502)
                    .header("Content-Type", "application/json")
                    .body(errorJson);
        }
    }
}
