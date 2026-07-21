package com.cryptoalert.application;

import com.cryptoalert.domain.model.AlertCondition;
import com.cryptoalert.domain.model.AlertStatus;
import com.cryptoalert.domain.model.CryptoAlert;
import com.cryptoalert.infrastructure.persistence.InMemoryCryptoAlertRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CryptoAlertServiceTest {

    private InMemoryCryptoAlertRepository repository;
    private CryptoAlertService service;

    @BeforeEach
    void setUp() {
        repository = new InMemoryCryptoAlertRepository();
        service = new CryptoAlertService(repository);
    }

    @Test
    void createAlertReturnsSavedAlert() {
        CryptoAlert created = service.createAlert(" btc ", new BigDecimal("42000"), AlertCondition.ABOVE)
                .await().indefinitely();

        assertNotNull(created.getId());
        assertEquals("BTC", created.getSymbol());
    }

    @Test
    void createAlertRejectsBlankSymbol() {
        assertThrows(RuntimeException.class, () -> service.createAlert(" ", new BigDecimal("42000"), AlertCondition.ABOVE)
                .await().indefinitely());
    }

    @Test
    void createAlertRejectsNonPositiveTargetPrice() {
        assertThrows(RuntimeException.class, () -> service.createAlert("BTC", BigDecimal.ZERO, AlertCondition.ABOVE)
                .await().indefinitely());
    }

    @Test
    void cancelAlertTransitionsActiveAlertToCancelled() {
        CryptoAlert alert = CryptoAlert.createNew("eth", new BigDecimal("3000"), AlertCondition.BELOW);
        repository.save(alert).await().indefinitely();

        CryptoAlert cancelled = service.cancelAlert(alert.getId()).await().indefinitely();

        assertEquals(AlertStatus.CANCELLED, cancelled.getStatus());
    }

    @Test
    void getAlertThrowsWhenNotFound() {
        UUID id = UUID.randomUUID();

        assertThrows(RuntimeException.class, () -> service.getAlert(id).await().indefinitely());
    }
}
