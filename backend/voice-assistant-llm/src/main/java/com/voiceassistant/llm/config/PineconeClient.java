package com.voiceassistant.llm.config;

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
 * Lightweight REST client for Pinecone with integrated embedding.
 * Uses java.net.http.HttpClient (no Spring Web dependency needed).
 * Pinecone embeds text server-side when the index is configured with an embedding model.
 */
@Slf4j
public class PineconeClient {

    private static final String BASE_URL = "https://api.pinecone.io";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String indexName;

    public PineconeClient(String apiKey, String indexName) {
        this.apiKey = apiKey;
        this.indexName = indexName;
        this.objectMapper = new ObjectMapper();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public void upsert(List<Record> records) {
        try {
            String body = objectMapper.writeValueAsString(new UpsertRequest(records));
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/records/upsert"))
                    .header("Api-Key", apiKey)
                    .header("Pinecone-Index", indexName)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .timeout(Duration.ofSeconds(30))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200 && response.statusCode() != 201) {
                log.error("Pinecone upsert failed: status={}, body={}", response.statusCode(), response.body());
                throw new RuntimeException("Pinecone upsert failed: HTTP " + response.statusCode());
            }
            log.debug("Pinecone upsert OK: {} records", records.size());
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
                    .uri(URI.create(BASE_URL + "/records/search"))
                    .header("Api-Key", apiKey)
                    .header("Pinecone-Index", indexName)
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
            return resp.result.hits;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("Pinecone search error", e);
            throw new RuntimeException("Pinecone search failed: " + e.getMessage(), e);
        }
    }

    // --- DTOs ---

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
    static class SearchResponse {
        private Result result;
    }

    @Data
    static class Result {
        private List<Hit> hits;
    }

    @Data
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
