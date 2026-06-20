package com.boaglio.zoio;

public record FraudDetectorResponse(
  boolean approved,
  Double fraud_score
){}