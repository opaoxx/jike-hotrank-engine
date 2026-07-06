package com.jike.hotrank.engine.service;

import com.jike.hotrank.engine.config.HotRankProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LoadTestService {

    private static final String DEFAULT_BASE_URL = "http://localhost:8080";

    private final HotRankProperties properties;

    public Map<String, Object> runLoadTest(Integer qps, Integer durationSeconds, String baseUrl) {
        int actualQps = normalize(qps, 20, 1, properties.getPerformance().getMaxQps());
        int actualDuration = normalize(durationSeconds, 5, 1, properties.getPerformance().getMaxDurationSeconds());
        String targetBaseUrl = normalizeBaseUrl(baseUrl);
        int totalRequests = actualQps * actualDuration;
        int threads = Math.min(properties.getPerformance().getMaxThreads(), Math.max(1, actualQps));

        HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2))
            .build();
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        long startedNanos = System.nanoTime();
        List<Future<RequestResult>> futures = new ArrayList<>(totalRequests);

        for (int i = 0; i < totalRequests; i++) {
            int sequence = i;
            long scheduledAt = startedNanos + (1_000_000_000L * sequence / actualQps);
            futures.add(executor.submit(() -> executeRequest(httpClient, targetBaseUrl, sequence, scheduledAt)));
        }

        executor.shutdown();
        List<RequestResult> results = collectResults(futures);
        long elapsedNanos = System.nanoTime() - startedNanos;
        awaitShutdown(executor);

        return buildReport(actualQps, actualDuration, targetBaseUrl, elapsedNanos, results);
    }

    private RequestResult executeRequest(HttpClient httpClient, String baseUrl, int sequence, long scheduledAt) {
        long waitNanos = scheduledAt - System.nanoTime();
        if (waitNanos > 0) {
            LockSupport.parkNanos(waitNanos);
        }

        Endpoint endpoint = endpointFor(sequence, baseUrl);
        long start = System.nanoTime();
        int statusCode = 0;
        String error = null;
        try {
            statusCode = httpClient.send(endpoint.request(), HttpResponse.BodyHandlers.discarding()).statusCode();
        } catch (IOException e) {
            error = e.getClass().getSimpleName();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            error = "InterruptedException";
        }
        long latencyNanos = System.nanoTime() - start;
        return new RequestResult(endpoint.name(), statusCode >= 200 && statusCode < 300, statusCode, latencyNanos, error);
    }

    private Endpoint endpointFor(int sequence, String baseUrl) {
        int bucket = sequence % 10;
        if (bucket < 4) {
            return new Endpoint("GET /api/ranking/global", HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/ranking/global?limit=50"))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build());
        }
        if (bucket < 6) {
            return new Endpoint("GET /api/ranking/circle/{id}", HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/ranking/circle/1?limit=20"))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build());
        }
        if (bucket < 9) {
            long userId = 900000L + System.currentTimeMillis() % 100000L + sequence;
            String body = """
                {"topicId":1,"userId":%d,"interactionType":1,"deviceFingerprint":"perf_%d","ipAddress":"10.10.0.%d"}
                """.formatted(userId, sequence, sequence % 255);
            return new Endpoint("POST /api/interaction", HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/interaction"))
                .timeout(Duration.ofSeconds(5))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build());
        }
        return new Endpoint("GET /api/anti-spam/report", HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/api/anti-spam/report"))
            .timeout(Duration.ofSeconds(5))
            .GET()
            .build());
    }

    private List<RequestResult> collectResults(List<Future<RequestResult>> futures) {
        List<RequestResult> results = new ArrayList<>(futures.size());
        for (Future<RequestResult> future : futures) {
            try {
                results.add(future.get());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                results.add(new RequestResult("unknown", false, 0, 0, "InterruptedException"));
            } catch (ExecutionException e) {
                results.add(new RequestResult("unknown", false, 0, 0, e.getCause().getClass().getSimpleName()));
            }
        }
        return results;
    }

    private void awaitShutdown(ExecutorService executor) {
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            executor.shutdownNow();
        }
    }

    private Map<String, Object> buildReport(int targetQps,
                                            int durationSeconds,
                                            String baseUrl,
                                            long elapsedNanos,
                                            List<RequestResult> results) {
        long successCount = results.stream().filter(RequestResult::success).count();
        long errorCount = results.size() - successCount;
        double elapsedSeconds = elapsedNanos / 1_000_000_000.0;

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("targetQps", targetQps);
        report.put("actualQps", round(results.size() / Math.max(elapsedSeconds, 0.001)));
        report.put("durationSeconds", durationSeconds);
        report.put("elapsedSeconds", round(elapsedSeconds));
        report.put("baseUrl", baseUrl);
        report.put("totalRequests", results.size());
        report.put("successCount", successCount);
        report.put("errorCount", errorCount);
        report.put("successRate", percentage(successCount, results.size()));
        report.put("latency", latencyStats(results));
        report.put("byEndpoint", endpointStats(results));
        report.put("suggestions", suggestions(successCount, results));
        return report;
    }

    private Map<String, Object> endpointStats(List<RequestResult> results) {
        return results.stream()
            .collect(Collectors.groupingBy(RequestResult::endpoint, LinkedHashMap::new, Collectors.toList()))
            .entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> {
                    List<RequestResult> endpointResults = entry.getValue();
                    long success = endpointResults.stream().filter(RequestResult::success).count();
                    Map<String, Object> stats = new LinkedHashMap<>();
                    stats.put("count", endpointResults.size());
                    stats.put("successCount", success);
                    stats.put("errorCount", endpointResults.size() - success);
                    stats.put("successRate", percentage(success, endpointResults.size()));
                    stats.put("latency", latencyStats(endpointResults));
                    return stats;
                },
                (left, right) -> left,
                LinkedHashMap::new
            ));
    }

    private Map<String, Object> latencyStats(List<RequestResult> results) {
        List<Double> latencies = results.stream()
            .map(result -> result.latencyNanos() / 1_000_000.0)
            .sorted(Comparator.naturalOrder())
            .toList();
        Map<String, Object> stats = new LinkedHashMap<>();
        if (latencies.isEmpty()) {
            stats.put("minMs", 0);
            stats.put("maxMs", 0);
            stats.put("avgMs", 0);
            stats.put("p50Ms", 0);
            stats.put("p95Ms", 0);
            stats.put("p99Ms", 0);
            return stats;
        }

        stats.put("minMs", round(latencies.getFirst()));
        stats.put("maxMs", round(latencies.getLast()));
        stats.put("avgMs", round(latencies.stream().mapToDouble(Double::doubleValue).average().orElse(0)));
        stats.put("p50Ms", round(percentile(latencies, 0.50)));
        stats.put("p95Ms", round(percentile(latencies, 0.95)));
        stats.put("p99Ms", round(percentile(latencies, 0.99)));
        return stats;
    }

    private List<String> suggestions(long successCount, List<RequestResult> results) {
        List<String> suggestions = new ArrayList<>();
        BigDecimal successRate = percentage(successCount, results.size());
        double p99 = (double) latencyStats(results).get("p99Ms");
        if (successRate.compareTo(new BigDecimal("99.00")) < 0) {
            suggestions.add("成功率低于 99%，优先检查应用日志、数据库连接池和反作弊拒绝比例。");
        }
        if (p99 > 200) {
            suggestions.add("P99 延迟高于 200ms，建议检查榜单缓存命中率和慢 SQL。");
        }
        if (suggestions.isEmpty()) {
            suggestions.add("当前压测指标正常，可进一步提高 QPS 或延长持续时间观察稳定性。");
        }
        return suggestions;
    }

    private double percentile(List<Double> sortedValues, double percentile) {
        int index = (int) Math.ceil(percentile * sortedValues.size()) - 1;
        return sortedValues.get(Math.max(0, Math.min(sortedValues.size() - 1, index)));
    }

    private int normalize(Integer requested, int defaultValue, int min, int max) {
        if (requested == null) {
            return defaultValue;
        }
        return Math.max(min, Math.min(max, requested));
    }

    private String normalizeBaseUrl(String baseUrl) {
        String value = baseUrl == null || baseUrl.isBlank() ? DEFAULT_BASE_URL : baseUrl.trim();
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private BigDecimal percentage(long part, long total) {
        if (total <= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(part)
            .multiply(BigDecimal.valueOf(100))
            .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
    }

    private double round(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private record Endpoint(String name, HttpRequest request) {
    }

    private record RequestResult(String endpoint, boolean success, int statusCode, long latencyNanos, String error) {
    }
}
