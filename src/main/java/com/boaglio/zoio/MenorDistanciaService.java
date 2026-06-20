package com.boaglio.zoio;

import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class MenorDistanciaService {

    public static double euclideanDistance(double[] vectorA, double[] vectorB) {
        if (vectorA.length != vectorB.length) {
            throw new IllegalArgumentException("Vectors must have the same length");
        }

        double sum = 0.0;
        for (int i = 0; i < vectorA.length; i++) {
            double diff = vectorA[i] - vectorB[i];
            sum += diff * diff;
        }

        return Math.sqrt(sum);
    }

    public static List<FraudVector> findTop5Optimized(double[] input) {
        int k = 5;
        int n = BuscaService.vectors.length;
        int cores = Runtime.getRuntime().availableProcessors();
        int chunkSize = (n + cores - 1) / cores;

        // One heap per thread — no shared state, no contention
        List<PriorityQueue<double[]>> perThreadHeaps = IntStream.range(0, cores)
                .parallel()
                .mapToObj(thread -> {
                    int from = thread * chunkSize;
                    int to   = Math.min(from + chunkSize, n);

                    // Max-heap: worst of best-k at the top
                    PriorityQueue<double[]> heap = new PriorityQueue<>(k,
                            (a, b) -> Double.compare(b[0], a[0]));

                    for (int i = from; i < to; i++) {
                        double dist = euclideanDistance(input, BuscaService.vectors[i]);
                        if (heap.size() < k) {
                            heap.offer(new double[]{dist, i});
                        } else if (dist < heap.peek()[0]) {
                            heap.poll();
                            heap.offer(new double[]{dist, i});
                        }
                    }
                    return heap;
                })
                .toList();

        // Merge all per-thread heaps into a single heap of size k
        PriorityQueue<double[]> merged = new PriorityQueue<>(k,
                (a, b) -> Double.compare(b[0], a[0]));

        for (PriorityQueue<double[]> heap : perThreadHeaps) {
            for (double[] entry : heap) {
                if (merged.size() < k) {
                    merged.offer(entry);
                } else if (entry[0] < merged.peek()[0]) {
                    merged.poll();
                    merged.offer(entry);
                }
            }
        }

        return merged.stream()
                .sorted(Comparator.comparingDouble(a -> a[0]))
                .map(a -> new FraudVector(
                        BuscaService.vectors[(int) a[1]],
                        BuscaService.labels[(int) a[1]],
                        a[0]))
                .collect(Collectors.toList());
    }

    private static final ForkJoinPool POOL =
            new ForkJoinPool(Runtime.getRuntime().availableProcessors());

    public static List<FraudVector> findTop5(double[] input) throws Exception {
        return POOL.submit(() -> findTop5Optimized(input) // runs inside dedicated pool
        ).get();
    }

}