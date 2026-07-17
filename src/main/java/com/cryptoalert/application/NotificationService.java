package com.cryptoalert.application;

import com.cryptoalert.domain.model.CryptoAlert;
import com.cryptoalert.domain.model.PriceRecord;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
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
        Object value = readValue(alert, "getSymbol", "symbol");
        if (value instanceof String symbol && !symbol.isBlank()) {
            return symbol;
        }
        Object priceValue = readValue(priceRecord, "getSymbol", "symbol");
        return priceValue instanceof String symbolValue ? symbolValue : "UNKNOWN";
    }

    private String resolveCondition(CryptoAlert alert) {
        Object value = readValue(alert, "getCondition", "condition");
        return value != null ? String.valueOf(value) : "unknown";
    }

    private String resolvePriceValue(PriceRecord priceRecord) {
        Object value = readValue(priceRecord, "getPrice", "price");
        return value != null ? String.valueOf(value) : "n/a";
    }

    private Object readValue(Object target, String... names) {
        if (target == null) {
            return null;
        }

        for (String name : names) {
            try {
                Method getter = target.getClass().getMethod(name);
                Object value = getter.invoke(target);
                if (value != null) {
                    return value;
                }
            } catch (ReflectiveOperationException ignored) {
                // Try the next accessor name.
            }

            try {
                Field field = target.getClass().getDeclaredField(name);
                field.setAccessible(true);
                Object value = field.get(target);
                if (value != null) {
                    return value;
                }
            } catch (ReflectiveOperationException ignored) {
                // Try the next accessor name.
            }
        }

        return null;
    }

    private record WebhookPayload(String symbol, String price, String condition, Instant triggeredAt) {
    }
}
