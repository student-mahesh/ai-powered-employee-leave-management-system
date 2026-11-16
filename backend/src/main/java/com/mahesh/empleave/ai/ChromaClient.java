package com.mahesh.empleave.ai;

import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@Slf4j
public class ChromaClient {
    private static final String V2_PREFIX = "/api/v2/tenants/default_tenant/databases/default_database";
    private volatile RestClient client;
    private final String baseUrl;
    private volatile String collectionId;

    public ChromaClient(@Value("${spring.ai.chroma-url:http://127.0.0.1:8001}") String baseUrl) {
        this.baseUrl = baseUrl.replaceAll("/$", "");
    }

    public String upsert(String id, String document, float[] embedding, Map<String, String> metadata) {
        if (embedding == null || embedding.length == 0) throw new ChromaUnavailableException("Embedding vector is empty");
        try {
            var collection = collection();
            var body = Map.of("ids", List.of(id), "documents", List.of(document),
                    "embeddings", List.of(toList(embedding)), "metadatas", List.of(metadata));
            client().post().uri(V2_PREFIX + "/collections/{id}/upsert", collection)
                    .contentType(MediaType.APPLICATION_JSON).body(body).retrieve().toBodilessEntity();
            return id;
        } catch (Exception ex) {
            log.warn("Chroma upsert failed: type={} message={}", ex.getClass().getSimpleName(), ex.getMessage());
            throw new ChromaUnavailableException("Policy vector indexing is unavailable", ex);
        }
    }

    public List<QueryResult> query(float[] embedding, int limit) {
        try {
            var collection = collection();
            var response = client().post().uri(V2_PREFIX + "/collections/{id}/query", collection)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("query_embeddings", List.of(toList(embedding)), "n_results", limit, "include", List.of("documents", "metadatas", "distances")))
                    .retrieve().body(new ParameterizedTypeReference<Map<String, Object>>() {});
            return response == null ? List.of() : extractResults(response);
        } catch (Exception ex) {
            log.warn("Chroma query failed: type={} message={}", ex.getClass().getSimpleName(), ex.getMessage());
            throw new ChromaUnavailableException("Policy search is unavailable", ex);
        }
    }

    private String collection() {
        if (collectionId != null) return collectionId;
        synchronized (this) {
            if (collectionId == null) {
                var response = client().post().uri(V2_PREFIX + "/collections")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(Map.of("name", "hr_policy", "get_or_create", true))
                        .retrieve().body(new ParameterizedTypeReference<Map<String, Object>>() {});
                collectionId = Objects.toString(response == null ? null : response.get("id"), "hr_policy");
            }
            return collectionId;
        }
    }

    public record QueryResult(String id, double distance) { }

    private List<QueryResult> extractResults(Map<String, Object> response) {
        var ids = nestedValues(response.get("ids"));
        var distances = nestedValues(response.get("distances"));
        var results = new ArrayList<QueryResult>();
        for (int i = 0; i < ids.size(); i++) {
            var distance = i < distances.size() ? toDouble(distances.get(i)) : Double.NaN;
            results.add(new QueryResult(String.valueOf(ids.get(i)), distance));
        }
        return results;
    }

    private List<?> nestedValues(Object value) {
        if (!(value instanceof List<?> outer) || outer.isEmpty() || !(outer.get(0) instanceof List<?> inner)) return List.of();
        return inner;
    }

    private double toDouble(Object value) {
        if (value instanceof Number number) return number.doubleValue();
        try { return Double.parseDouble(String.valueOf(value)); } catch (NumberFormatException ex) { return Double.NaN; }
    }

    private List<Float> toList(float[] values) { var result = new ArrayList<Float>(values.length); for (float value : values) result.add(value); return result; }

    private RestClient client() {
        var current = client;
        if (current == null) synchronized (this) {
            if (client == null) client = RestClient.builder().baseUrl(baseUrl).build();
            current = client;
        }
        return current;
    }
}
