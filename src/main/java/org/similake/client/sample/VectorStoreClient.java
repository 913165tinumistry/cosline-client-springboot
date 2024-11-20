package org.similake.client.sample;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javafaker.Faker;
import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class VectorStoreClient {
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final Faker faker = new Faker();
    private static final HttpClient client = HttpClient.newHttpClient();
    private static final String ENDPOINT = "http://localhost:6767/collections/vector_store/payloads";

    private static final Map<String, List<String>> BRAND_PRODUCTS = Map.of(
            "Apple", Arrays.asList("Smartphone", "Laptop", "Tablet", "Desktop"),
            "Samsung", Arrays.asList("Smartphone", "TV", "Tablet", "Laptop"),
            "Dell", Arrays.asList("Laptop", "Desktop", "Monitor", "Workstation"),
            "OnePlus", Arrays.asList("Smartphone", "Tablet", "Earbuds"),
            "Lenovo", Arrays.asList("Laptop", "Tablet", "Desktop", "Monitor")
    );

    private static final String[] FEATURES = {
            "powerful processor",
            "advanced camera features",
            "fast charging",
            "high refresh rate display",
            "extended battery",
            "high-resolution camera",
            "large screen",
            "lightweight design",
            "5G-enabled",
            "premium build quality"
    };

    public static void main(String[] args) {
        try {
            Instant startTime = Instant.now();

            sendBatchData(10000); // Total records to generate

            Instant endTime = Instant.now();
            Duration duration = Duration.between(startTime, endTime);

            System.out.println("\nSummary:");
            System.out.printf("Total time taken: %.2f seconds%n", duration.toMillis() / 1000.0);
            System.out.printf("Average time per batch: %.2f seconds%n", (duration.toMillis() / 1000.0) / (10000/100));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void sendBatchData(int totalRecords) throws Exception {
        int batchSize = 1000;
        int batches = totalRecords / batchSize;
        int successfulBatches = 0;
        int failedBatches = 0;

        for (int batch = 0; batch < batches; batch++) {
            Instant batchStartTime = Instant.now();

            List<Map<String, Object>> records = generateBatch(batchSize);
            String jsonPayload = mapper.writeValueAsString(records);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(ENDPOINT))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            Duration batchDuration = Duration.between(batchStartTime, Instant.now());

            if (response.statusCode() == 200 || response.statusCode() == 201) {
                successfulBatches++;
                System.out.printf("Batch %d/%d sent. Status: %d. Time: %.2f seconds%n",
                        batch + 1, batches, response.statusCode(), batchDuration.toMillis() / 1000.0);
            } else {
                failedBatches++;
                System.out.printf("Batch %d/%d FAILED. Status: %d. Time: %.2f seconds%n",
                        batch + 1, batches, response.statusCode(), batchDuration.toMillis() / 1000.0);
                System.out.println("Error response: " + response.body());
            }

            // Small delay to avoid overwhelming the server
            Thread.sleep(100);
        }

        System.out.printf("%nSuccessful batches: %d, Failed batches: %d%n",
                successfulBatches, failedBatches);
    }

    private static List<Map<String, Object>> generateBatch(int size) {
        List<Map<String, Object>> batch = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            batch.add(generateRecord());
        }
        return batch;
    }

    private static Map<String, Object> generateRecord() {
        Map<String, Object> record = new HashMap<>();

        String brand = BRAND_PRODUCTS.keySet().stream()
                .skip(ThreadLocalRandom.current().nextInt(BRAND_PRODUCTS.size()))
                .findFirst()
                .orElse("Apple");

        String product = BRAND_PRODUCTS.get(brand).get(
                ThreadLocalRandom.current().nextInt(BRAND_PRODUCTS.get(brand).size())
        );

        String content = generateContent(brand, product);

        Map<String, String> metadata = new HashMap<>();
        metadata.put("brand", brand);
        metadata.put("product_name", product);

        record.put("id", UUID.randomUUID().toString());
        record.put("content", content);
        record.put("vector", generateVector());
        record.put("metadata", metadata);

        return record;
    }

    private static String generateContent(String brand, String product) {
        List<String> allFeatures = new ArrayList<>(Arrays.asList(FEATURES));
        Collections.shuffle(allFeatures);
        List<String> selectedFeatures = allFeatures.subList(0, ThreadLocalRandom.current().nextInt(2, 4));

        return brand + " " + product + " with " + String.join(" and ", selectedFeatures) + ".";
    }

    private static float[] generateVector() {
        float[] vector = new float[1536];
        for (int i = 0; i < vector.length; i++) {
            vector[i] = (float) (ThreadLocalRandom.current().nextDouble(-0.1, 0.1));
        }
        return vector;
    }
}