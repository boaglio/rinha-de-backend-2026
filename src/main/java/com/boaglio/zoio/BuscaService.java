package com.boaglio.zoio;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class BuscaService implements ApplicationRunner  {

    private static final Logger log = LoggerFactory.getLogger(BuscaService.class);
    int TOTAL_VECTORS = 3_000_000;
    double FRACAO = .01;
    public static List<FraudVector> fraudVectors;
    public static double[][] vectors;
    public static String[]  labels;

    void load() {

        long start = System.nanoTime();
        log.info("► [load] Start");
        var mapper = new ObjectMapper();

        try (InputStream is = BuscaService.class
                .getClassLoader()
                .getResourceAsStream("references.json")) {

            if (is == null) {
                throw new IllegalStateException("json file not found in resources");
            }
            fraudVectors = mapper.readValue(
                    is,
                    new TypeReference<List<FraudVector>>() {}
            );
            fraudVectors = fraudVectors.subList(0, (int) (TOTAL_VECTORS * FRACAO));

            vectors = new double[fraudVectors.size()][];
            labels  = new String[fraudVectors.size()];

            for (int i = 0; i < fraudVectors.size(); i++) {
                vectors[i] = fraudVectors.get(i).vector();
                labels[i]  = fraudVectors.get(i).label();
            }
            log.info("Loaded {} vectors into flat array structure", vectors.length);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        long elapsed = System.nanoTime() - start;
        log.info("◄ [load] Completed in {} ms ({} µs)",
                TimeUnit.NANOSECONDS.toMillis(elapsed),
                TimeUnit.NANOSECONDS.toMicros(elapsed));

        IO.println("List size: "+fraudVectors.size());
    }

    void logMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        runtime.gc();

        long totalMemory = runtime.totalMemory();
        long freeMemory  = runtime.freeMemory();
        long usedMemory  = totalMemory - freeMemory;
        long maxMemory   = runtime.maxMemory();

        log.info("──────────────── Memory Usage ────────────────");
        log.info("  List entries : {} vectors", fraudVectors.size());
        log.info("  Estimated list size : {} KB", estimateListSizeKB());
        log.info("  JVM used     : {} MB", toMB(usedMemory));
        log.info("  JVM free     : {} MB", toMB(freeMemory));
        log.info("  JVM total    : {} MB", toMB(totalMemory));
        log.info("  JVM max      : {} MB", toMB(maxMemory));
        log.info("──────────────────────────────────────────────");
    }

    long estimateListSizeKB() {
        if (fraudVectors == null || fraudVectors.isEmpty()) return 0;

        long bytes = fraudVectors.stream()
                .mapToLong(fv ->
                        (long) fv.vector().length * Double.BYTES  // 8 bytes per double
                                + 16L                                      // double[] object header
                                + 50L                                      // String label overhead
                ).sum();

        bytes += 16L + (long) fraudVectors.size() * 4;    // ArrayList header + references

        return bytes / 1024;
    }

    long toMB(long bytes) {
        return bytes / (1024 * 1024);
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {

        load();
        logMemoryUsage();

    }

}