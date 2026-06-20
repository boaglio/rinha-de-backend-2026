package com.boaglio.zoio;

import com.fasterxml.jackson.annotation.JsonProperty;

public record FraudDetectorRequest(
   String id,
   Transaction transaction,
   Customer customer,
   Merchant merchant,
   Terminal terminal,
   @JsonProperty("last_transaction") LastTransaction lastTransaction
) {
}

record Transaction(
        Double amount,
        Integer installments,
        String requested_at
){}

record Customer(
     Double avg_amount,
     Integer tx_count_24h,
     String[] known_merchants
){}

record Merchant(
      String id,
      String mcc,
      Double avg_amount
){}

record Terminal(
        Boolean is_online,
        Boolean card_present,
        Double km_from_home
){}

record LastTransaction(
       String timestamp,
       Double km_from_current
){}