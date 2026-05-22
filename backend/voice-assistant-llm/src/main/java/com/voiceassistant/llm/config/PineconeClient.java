package com.voiceassistant.llm.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * REST client for Pinecone standard vector API.
 * Uses client-side embedding (via EmbeddingModel) + Pinecone /vectors/upsert and /query.
 */
@Slf4j
public class PineconeClient {

    private static final String CONTROL_PLANE = "https://api.pinecone.io";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String indexHost;

    public PineconeClient(String apiKey, String indexName) {
        this.apiKey = apiKey;
        this.objectMapper = new ObjectMapper();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.indexHost = discoverHost(indexName);
        log.info("Pinecone index host: {}", indexHost);
    }

    private String discoverHost(String indexName) {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(CONTROL_PLANE + "/indexes/" + indexName))
                    .header("Api-Key", apiKey)
                    .GET()
                    .timeout(Duration.ofSeconds(10))
                    .build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200) {
                IndexDescription desc = objectMapper.readValue(resp.body(), IndexDescription.class);
                if (desc.host != null && !desc.host.isBlank()) {
                    return desc.host.startsWith("http") ? desc.host : "https://" + desc.host;
                }
            }
            log.warn("Pinecone host discovery failed (HTTP {})", resp.statusCode());
        } catch (Exception e) {
            log.warn("Pinecone host discovery error: {}", e.getMessage());
        }
        return CONTROL_PLANE;
    }

    /**
     * Upsert vectors with metadata to Pinecone.
     */
    public void upsert(List<VectorEntry> vectors) {
        try {
            String body = objectMapper.writeValueAsString(Map.of("vectors", vectors));
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(indexHost + "/vectors/upsert"))
                    .header("Api-Key", apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .timeout(Duration.ofSeconds(60))
                    .build();

            HttpResponse<String> resp = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200 && resp.statusCode() != 201) {
                log.error("Pinecone upsert failed: HTTP {} body={}", resp.statusCode(),
                        resp.body().length() > 300 ? resp.body().substring(0, 300) : resp.body());
                throw new RuntimeException("Pinecone upsert failed: HTTP " + resp.statusCode());
            }
            log.debug("Pinecone upsert OK: {} vectors", vectors.size());
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("Pinecone upsert error", e);
            throw new RuntimeException("Pinecone upsert failed", e);
        }
    }

    /**
     * Query Pinecone by vector, returning matching metadata.
     */
    public List<Match> query(float[] queryVector, int topK) {
        try {
            QueryRequest qr = new QueryRequest(queryVector, topK,
                    true,  // includeMetadata
                    false  // includeValues (don't need vectors back)
            );
            String body = objectMapper.writeValueAsString(qr);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(indexHost + "/query"))
                    .header("Api-Key", apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .timeout(Duration.ofSeconds(30))
                    .build();

            HttpResponse<String> resp = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) {
                log.error("Pinecone query failed: HTTP {} body={}", resp.statusCode(),
                        resp.body().length() > 300 ? resp.body().substring(0, 300) : resp.body());
                throw new RuntimeException("Pinecone query failed: HTTP " + resp.statusCode());
            }
            QueryResponse qr2 = objectMapper.readValue(resp.body(), QueryResponse.class);
            return qr2.matches != null ? qr2.matches : List.of();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("Pinecone query error", e);
            throw new RuntimeException("Pinecone query failed", e);
        }
    }

    // --- DTOs ---

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class IndexDescription {
        private String host;
    }

    @Data
    public static class VectorEntry {
        private String id;
        private float[] values;
        private Map<String, Object> metadata;

        public VectorEntry() {}
        public VectorEntry(String id, float[] values, Map<String, Object> metadata) {
            this.id = id; this.values = values; this.metadata = metadata;
        }
    }

    @Data
    static class QueryRequest {
        @JsonProperty("top_k")
        private final int topK;
        private final float[] vector;
        @JsonProperty("include_metadata")
        private final boolean includeMetadata;
        @JsonProperty("include_values")
        private final boolean includeValues;

        QueryRequest(float[] vector, int topK, boolean includeMetadata, boolean includeValues) {
            this.vector = vector; this.topK = topK;
            this.includeMetadata = includeMetadata; this.includeValues = includeValues;
        }
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class QueryResponse {
        private List<Match> matches;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Match {
        private String id;
        private double score;
        private Map<String, Object> metadata;

        public String getText() {
            return metadata != null ? (String) metadata.get("text") : null;
        }
    }
}
