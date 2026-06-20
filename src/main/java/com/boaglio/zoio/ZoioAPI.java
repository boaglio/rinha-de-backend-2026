package com.boaglio.zoio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

@RestController
public class ZoioAPI {

    private static long counter = 0;
    private final VetorizaService vetorizaService;

    public ZoioAPI(VetorizaService vetorizaService) {
        this.vetorizaService = vetorizaService;
    }

    private static final Logger log = LoggerFactory.getLogger(ZoioAPI.class);

    @GetMapping("/ready")
    public ResponseEntity<Void> ready() {
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @PostMapping("/fraud-score")
    public ResponseEntity<FraudDetectorResponse> fraudDetectorResponse(@RequestBody FraudDetectorRequest fraudDetectorRequest) {

        long start = System.nanoTime();
        ++counter;
//        log.info("► {} [fraud-score] Request received: {}",counter,fraudDetectorRequest);

        try {
//            IO.println(fraudDetectorRequest);

            var normalized = vetorizaService.vetoriza(fraudDetectorRequest);

//            IO.println(Arrays.toString(normalized));

            var listNearestFraudVector = MenorDistanciaService.findTop5( normalized );

//            IO.println(listNearestFraudVector);

            double fraudSum = 0;
            if (ZoioNasFraudesApplication.FRAUD.equals(listNearestFraudVector.getFirst().label())) fraudSum++;
//            IO.println("mais perto = %s - %s".formatted(listNearestFraudVector.getFirst().distance(),listNearestFraudVector.getFirst().label()));
            if (ZoioNasFraudesApplication.FRAUD.equals(listNearestFraudVector.get(1).label())) fraudSum++;
            if (ZoioNasFraudesApplication.FRAUD.equals(listNearestFraudVector.get(2).label())) fraudSum++;
            if (ZoioNasFraudesApplication.FRAUD.equals(listNearestFraudVector.get(3).label())) fraudSum++;
            if (ZoioNasFraudesApplication.FRAUD.equals(listNearestFraudVector.get(4).label())) fraudSum++;
//            IO.println("menos perto = %s - %s".formatted(listNearestFraudVector.get(4).distance(),listNearestFraudVector.get(4).label()));
            var fraudScore = fraudSum/5;

//            IO.println("fraudSum = %s".formatted(fraudSum));
//            IO.println("fraudScore = %s".formatted(fraudScore));

            // approved = fraud_score < 0.6 (threshold is fixed; 0.6 must be denied)
            FraudDetectorResponse fraudDetectorResponse =
                    new FraudDetectorResponse(fraudScore < 0.6, fraudScore);

            long elapsed = System.nanoTime() - start;
            log.info("◄ {}[fraud-score: {}] Completed in {} ms ({} µs)",
                    counter,
                    fraudScore,
                    TimeUnit.NANOSECONDS.toMillis(elapsed),
                    TimeUnit.NANOSECONDS.toMicros(elapsed));

            return ResponseEntity.ok(fraudDetectorResponse);

        } catch (Exception e) {
            long elapsed = System.nanoTime() - start;
            log.error("✗ [fraud-score] Failed after {} ms — {}",
                    TimeUnit.NANOSECONDS.toMillis(elapsed), e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }

    }
}
