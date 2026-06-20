package com.boaglio.zoio;

public record FraudVector(
        double [] vector,
        String label,
        double distance
) {
}
