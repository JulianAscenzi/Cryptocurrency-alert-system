package com.cryptoalert.application;

import com.cryptoalert.domain.model.AlertCondition;
import com.cryptoalert.domain.model.CryptoAlert;
import com.cryptoalert.domain.model.PriceRecord;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class NotificationServiceTest {

    @Test
    void shouldNotifyWithoutThrowingWhenAlertAndPriceRecordArePresent() {
        NotificationService notificationService = new NotificationService();
        CryptoAlert alert = CryptoAlert.createNew("BTCUSDT", new BigDecimal("1000"), AlertCondition.ABOVE);
        PriceRecord priceRecord = new PriceRecord("BTCUSDT", new BigDecimal("1001.50"), Instant.now());

        assertDoesNotThrow(() -> notificationService.notifyTriggeredAlert(alert, priceRecord));
    }

    @Test
    void shouldFallbackGracefullyWhenValuesAreMissing() {
        NotificationService notificationService = new NotificationService();

        assertDoesNotThrow(() -> notificationService.notifyTriggeredAlert(null, null));
    }
}
