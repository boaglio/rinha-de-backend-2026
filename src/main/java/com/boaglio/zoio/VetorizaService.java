package com.boaglio.zoio;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Map;

@Service
public class VetorizaService {

    static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");

    static final Map<String, Double> MCC = Map.of(
            "5411", 0.15,
            "5812", 0.30,
            "5912", 0.20,
            "5944", 0.45,
            "7801", 0.80,
            "7802", 0.75,
            "7995", 0.85,
            "4511", 0.35,
            "5311", 0.25,
            "5999", 0.50);

    static final int max_amount = 10000;
    static final int max_installments = 12;
    static final int amount_vs_avg_ratio = 10;
    static final int max_minutes = 1440;
    static final int max_km = 1000;
    static final int max_tx_count_24h = 20;
    static final int max_merchant_avg_amount = 10000;

    public double [] vetoriza(FraudDetectorRequest fraudDetectorRequest) {

        var transaction = fraudDetectorRequest.transaction();
        var customer = fraudDetectorRequest.customer();
        var dateRequest =  LocalDateTime.parse(transaction.requested_at(), DATE_FORMAT);
        var noLastTransaction = fraudDetectorRequest.lastTransaction()==null;
        double lastTransactionMin = 0;
        double lastTransactionKm = 0;
        if (!noLastTransaction) {
            var dateLastTrans =  LocalDateTime.parse(fraudDetectorRequest.lastTransaction().timestamp(), DATE_FORMAT);
            lastTransactionMin= ChronoUnit.MINUTES.between(dateLastTrans, dateRequest);
            lastTransactionKm = fraudDetectorRequest.lastTransaction().km_from_current();
        }
        var hour = dateRequest.getHour();
        var dayOfWeek = dateRequest.getDayOfWeek().getValue()-1;
        var terminal = fraudDetectorRequest.terminal();
        var merchant = fraudDetectorRequest.merchant();
        var unknown_merchant = true;
        for (String merchantId : customer.known_merchants()) {
            if (merchantId.equals(merchant.id())) {
                unknown_merchant = false;
                break;
            }
        }

        return new double[]{
                // amount - limitar(transaction.amount / max_amount)
                limitar(transaction.amount() / max_amount),

                // installments 	limitar(transaction.installments / max_installments)
                limitar((double) transaction.installments() / max_installments),

                // amount_vs_avg 	limitar((transaction.amount / customer.avg_amount) / amount_vs_avg_ratio)
                limitar((transaction.amount() / customer.avg_amount()) / amount_vs_avg_ratio),

                // hour_of_day 	hora(transaction.requested_at) / 23 (0-23, UTC)
                round((double) hour / 23),

                // day_of_week 	dia_da_semana(transaction.requested_at) / 6 (seg=0, dom=6)
                round((double) dayOfWeek / 6),

                // minutes_since_last_tx 	limitar(minutos / max_minutes) ou -1 se last_transaction: null
                noLastTransaction ? -1   : limitar(lastTransactionMin / max_minutes),

                // km_from_last_tx 	limitar(last_transaction.km_from_current / max_km) ou -1 se last_transaction: null
                noLastTransaction ? -1  : limitar(lastTransactionKm / max_km),

                // km_from_home 	limitar(terminal.km_from_home / max_km)
                limitar(terminal.km_from_home() / max_km),

                // tx_count_24h 	limitar(customer.tx_count_24h / max_tx_count_24h)
                limitar((double) customer.tx_count_24h() / max_tx_count_24h),

                // is_online 	1 se terminal.is_online, senão 0
                terminal.is_online() ? 1 : 0,

                // card_present 	1 se terminal.card_present, senão 0
                terminal.card_present() ? 1 : 0,

                // unknown_merchant 	1 se merchant.id não estiver em customer.known_merchants, senão 0 (invertido: 1 = desconhecido)
                unknown_merchant ? 1 : 0 ,

                // mcc_risk 	mcc_risk.json[merchant.mcc] (valor padrão 0.5)
                MCC.getOrDefault(merchant.mcc(), 0.5),

                // merchant_avg_amount 	limitar(merchant.avg_amount / max_merchant_avg_amount)
                limitar(merchant.avg_amount() / max_merchant_avg_amount)
        };

    }

    double round(double b) {
        return BigDecimal.valueOf(b)
                .setScale(4, RoundingMode.HALF_UP)
                .stripTrailingZeros()
                .doubleValue();
    }

    double limitar(double amount) {
        if (amount < 0) { return 0.0d; }
        if (amount > 1) { return 1.0d; }
        return round(amount);
    }

}