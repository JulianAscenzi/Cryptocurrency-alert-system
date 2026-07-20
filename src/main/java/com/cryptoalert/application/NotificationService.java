package com.cryptoalert.application;

import com.cryptoalert.domain.model.CryptoAlert;
import com.cryptoalert.domain.model.PriceRecord;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;

@ApplicationScoped
public class NotificationService {

    private static final Logger LOG = LoggerFactory.getLogger(NotificationService.class);
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    public void notifyTriggeredAlert(CryptoAlert alert, PriceRecord priceRecord) {
        String symbol = resolveSymbol(alert, priceRecord);
        String condition = resolveCondition(alert);
        String priceValue = resolvePriceValue(priceRecord);
        Instant triggeredAt = Instant.now();

        LOG.info("""
                \u001b[1;36m============================================================\u001b[0m
                \u001b[1;36mALERTA ACTIVADA\u001b[0m
                \u001b[1;36m============================================================\u001b[0m
                Symbol: {}
                Price: {}
                Condition: {}
                Triggered at: {}
                \u001b[1;36m============================================================\u001b[0m
                """, symbol, priceValue, condition, triggeredAt);

        try {
            String payload = objectMapper.writeValueAsString(new WebhookPayload(symbol, priceValue, condition, triggeredAt));
            LOG.info("Simulando POST a webhook: {}", payload);
        } catch (JsonProcessingException exception) {
            LOG.warn("No se pudo serializar el payload del webhook", exception);
        }
    }

    private String resolveSymbol(CryptoAlert alert, PriceRecord priceRecord) {
        String symbol = alert != null ? alert.getSymbol() : null;
        if (symbol != null && !symbol.isBlank()) {
            return symbol;
        }

        String priceRecordSymbol = priceRecord != null ? priceRecord.symbol() : null;
        return priceRecordSymbol != null && !priceRecordSymbol.isBlank() ? priceRecordSymbol : "UNKNOWN";
    }

    private String resolveCondition(CryptoAlert alert) {
        return alert != null && alert.getCondition() != null ? alert.getCondition().name() : "unknown";
    }

    private String resolvePriceValue(PriceRecord priceRecord) {
        return priceRecord != null && priceRecord.price() != null ? priceRecord.price().toString() : "n/a";
    }

    private record WebhookPayload(String symbol, String price, String condition, Instant triggeredAt) {
    }
}
