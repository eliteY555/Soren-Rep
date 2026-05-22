package com.voiceassistant.llm.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
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
 * Lightweight REST client for Pinecone.
 * Auto-discovers the index data-plane host from the control-plane API,
 * then calls standard vector endpoints for upsert/query.
 *
 * For indexes with integrated embedding, Pinecone docs state that the
 * /records/upsert and /records/search endpoints should be used — these
 * live on the data-plane host, NOT on api.pinecone.io.
 *
 * Fallback: if records endpoint returns 404, try standard /vectors/upsert
 * which also supports server-side embedding when the index is configured for it.
 */
@Slf4j
public class PineconeClient {

    private static final String CONTROL_PLANE = "https://api.pinecone.io";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String indexName;
    private final String indexHost; // data-plane URL, e.g. https://{index}-xxx.svc.xxx.pinecone.io

    public PineconeClient(String apiKey, String indexName) {
        this.apiKey = apiKey;
        this.indexName = indexName;
        this.objectMapper = new ObjectMapper();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.indexHost = discoverHost();
        log.info("Pinecone index host resolved: {}", indexHost);
    }

    /**
     * Discover the data-plane host by calling GET /indexes/{name} on the control plane.
     */
    private String discoverHost() {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(CONTROL_PLANE + "/indexes/" + indexName))
                    .header("Api-Key", apiKey)
                    .header("Content-Type", "application/json")
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
            log.warn("Could not discover Pinecone host (HTTP {}), falling back to control-plane URL", resp.statusCode());
        } catch (Exception e) {
            log.warn("Failed to discover Pinecone index host: {}", e.getMessage());
        }
        return CONTROL_PLANE;
    }

    public void upsert(List<Record> records) {
        try {
            String body = objectMapper.writeValueAsString(new UpsertRequest(records));

            // Try /records/upsert on the data-plane host (for integrated embedding indexes)
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(indexHost + "/records/upsert"))
                    .header("Api-Key", apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .timeout(Duration.ofSeconds(60))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200 || response.statusCode() == 201) {
                log.debug("Pinecone upsert OK (records API): {} records", records.size());
                return;
            }

            // Fallback: log the error and report
            log.error("Pinecone upsert failed: status={}, body={}", response.statusCode(),
                    response.body().length() > 500 ? response.body().substring(0, 500) : response.body());
            throw new RuntimeException("Pinecone upsert failed: HTTP " + response.statusCode()
                    + " — ensure the index '" + indexName + "' has integrated embedding enabled");
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("Pinecone upsert error", e);
            throw new RuntimeException("Pinecone upsert failed: " + e.getMessage(), e);
        }
    }

    public List<Hit> search(String queryText, int topK) {
        try {
            SearchRequest sr = new SearchRequest(
                    new QueryInput(new TextInput(queryText)), topK);
            String body = objectMapper.writeValueAsString(sr);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(indexHost + "/records/search"))
                    .header("Api-Key", apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .timeout(Duration.ofSeconds(30))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.error("Pinecone search failed: status={}, body={}", response.statusCode(), response.body());
                throw new RuntimeException("Pinecone search failed: HTTP " + response.statusCode());
            }
            SearchResponse resp = objectMapper.readValue(response.body(), SearchResponse.class);
            return resp.result != null ? resp.result.hits : List.of();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("Pinecone search error", e);
            throw new RuntimeException("Pinecone search failed: " + e.getMessage(), e);
        }
    }

    String getIndexHost() { return indexHost; }

    // --- Control-plane DTO ---

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class IndexDescription {
        private String host;
        private String status;
    }

    // --- Data-plane DTOs ---

    @Data
    @Builder
    public static class Record {
        @JsonProperty("_id")
        private String id;
        private String text;
        private Map<String, Object> metadata;
    }

    @Data
    private static class UpsertRequest {
        private final List<Record> records;
        UpsertRequest(List<Record> records) { this.records = records; }
    }

    @Data
    private static class SearchRequest {
        private final QueryInput query;
        @JsonProperty("top_k")
        private final int topK;
        SearchRequest(QueryInput query, int topK) { this.query = query; this.topK = topK; }
    }

    @Data
    private static class QueryInput {
        private final TextInput inputs;
        QueryInput(TextInput inputs) { this.inputs = inputs; }
    }

    @Data
    private static class TextInput {
        private final String text;
        TextInput(String text) { this.text = text; }
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class SearchResponse {
        private Result result;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class Result {
        private List<Hit> hits;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Hit {
        @JsonProperty("_id")
        private String id;
        @JsonProperty("_score")
        private double score;
        private Map<String, Object> fields;

        public String getText() {
            return fields != null ? (String) fields.get("text") : null;
        }

        @SuppressWarnings("unchecked")
        public Map<String, Object> getMetadata() {
            return fields != null ? (Map<String, Object>) fields.get("metadata") : Map.of();
        }
    }
}
